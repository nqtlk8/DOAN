import axios from 'axios';
import { notify } from '../shared/notifications/notification';
import { ApiError } from '../shared/errors/ApiError';
import { normalizeApiError } from '../shared/errors/normalizeApiError';

const axiosInstance = axios.create({
  baseURL: '', // Relative URL for reverse proxy
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

const idempotencyCache = new Map<string, { key: string, timestamp: number }>();

axiosInstance.interceptors.request.use(
  (config: any) => {
    // Add Idempotency-Key
    if (config.method === 'post' || config.method === 'put') {
      const payloadStr = config.data ? JSON.stringify(config.data) : '';
      const cacheKey = `${config.method}:${config.url}:${payloadStr}`;
      const now = Date.now();
      let idKey = idempotencyCache.get(cacheKey);
      
      // Cache key for 5 seconds to prevent double click
      if (!idKey || (now - idKey.timestamp > 5000)) {
        const newKey = (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function')
          ? crypto.randomUUID()
          : `${Date.now().toString(36)}-${Math.random().toString(36).substring(2)}`;
        idKey = { key: newKey, timestamp: now };
        idempotencyCache.set(cacheKey, idKey);
      }
      
      config.headers['Idempotency-Key'] = idKey.key;
    }

    // Add token
    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    return config;
  },
  (error: any) => Promise.reject(error),
);

// Flag to prevent multiple refresh calls and duplicate notifications
let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

axiosInstance.interceptors.response.use(
  (response: any) => response,
  async (error: any) => {
    const originalRequest = error.config;
    
    // Ignore error handling if request config disables it (e.g., custom handling)
    if (originalRequest?.skipGlobalErrorHandler) {
      return Promise.reject(new ApiError(normalizeApiError(error)));
    }

    // 401 Unauthorized handling
    if (error.response?.status === 401 && !originalRequest._retry && originalRequest.url !== '/api/v1/auth/login') {
      if (isRefreshing) {
        return new Promise(function (resolve, reject) {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers['Authorization'] = 'Bearer ' + token;
            return axiosInstance(originalRequest);
          })
          .catch((err) => {
            return Promise.reject(err);
          });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = localStorage.getItem('refresh_token');
        if (refreshToken) {
          const res = await axios.post('/api/v1/auth/refresh', { refreshToken });
          if (res.status === 200 && res.data?.data?.accessToken) {
            const newAccessToken = res.data.data.accessToken;
            localStorage.setItem('access_token', newAccessToken);
            if (res.data.data.refreshToken) {
              localStorage.setItem('refresh_token', res.data.data.refreshToken);
            }
            processQueue(null, newAccessToken);
            originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
            return axiosInstance(originalRequest);
          }
        }
        throw new Error('Refresh failed');
      } catch (err) {
        processQueue(err, null);
        
        // Clear session and notify
        if (typeof window !== 'undefined') {
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');
          localStorage.removeItem('user');
          
          notify.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
          
          // Redirect after a short delay so user can read the toast
          setTimeout(() => {
            window.location.href = '/';
          }, 1500);
        }
        return Promise.reject(new ApiError(normalizeApiError(error)));
      } finally {
        isRefreshing = false;
      }
    }

    const normalizedError = normalizeApiError(error);
    
    // Global notification for Network / 403 / 500+
    // Only toast automatically for mutations (POST, PUT, DELETE, PATCH)
    // GET requests should be handled by ErrorState in the component (Phase 3)
    const method = originalRequest?.method?.toLowerCase();
    const skipToast = (originalRequest as any)?.skipGlobalErrorToast === true;

    if (error.response?.status === 403 && normalizedError.message.startsWith('User identity is missing')) {
      if (typeof window !== 'undefined') {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('user');
        localStorage.removeItem('branch_url');
        
        notify.error('Phiên đăng nhập cũ không còn hợp lệ, vui lòng đăng nhập lại');
        
        setTimeout(() => {
          window.location.href = '/';
        }, 1500);
      }
      return Promise.reject(new ApiError(normalizedError));
    }

    if (method && method !== 'get' && !skipToast) {
      notify.error(normalizedError.message);
    }

    return Promise.reject(new ApiError(normalizedError));
  },
);

export default axiosInstance;
