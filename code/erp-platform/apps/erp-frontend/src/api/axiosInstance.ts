import axios from 'axios';

const axiosInstance = axios.create({
  baseURL: '', // Rỗng để trỏ tới relative URL của Next.js app (/api/v1/...)
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosInstance.interceptors.request.use(
  (config: any) => {
    // Tự động thêm Idempotency-Key
    if (config.method === 'post' || config.method === 'put') {
      config.headers['Idempotency-Key'] = crypto.randomUUID();
    }

    // Thêm token từ localStorage (vì đã bỏ Next.js BFF)
    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    return config;
  },
  (error: any) => Promise.reject(error),
);

// Bỏ interceptor inject Authorization header, vì Middleware sẽ lấy từ HttpOnly Cookie
axiosInstance.interceptors.response.use(
  (response: any) => response,
  async (error: any) => {
    const originalRequest = error.config;
    // Nếu token hết hạn (401) và chưa thử lại
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
          // Lấy refresh token từ localStorage
          const refreshToken = localStorage.getItem('refresh_token');
          if (refreshToken) {
            const res = await axios.post('/api/v1/auth/refresh', { refreshToken });
            if (res.status === 200 && res.data?.data?.accessToken) {
              localStorage.setItem('access_token', res.data.data.accessToken);
              if (res.data.data.refreshToken) {
                  localStorage.setItem('refresh_token', res.data.data.refreshToken);
              }
              return axiosInstance(originalRequest);
            }
          }
      } catch (err) {
        // Refresh token cũng đã hết hạn/lỗi -> clear storage và reload
        if (typeof window !== 'undefined') {
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');
          localStorage.removeItem('user');
          window.location.reload();
        }
      }
    }
    return Promise.reject(error);
  },
);

export default axiosInstance;
