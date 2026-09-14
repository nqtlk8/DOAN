import React, { createContext, useContext, useState, useEffect } from 'react';
import { ApiService } from '../api/ApiService';

export const ROLES = {
  ADMIN: 'ADMIN' as const,
  STAFF: 'STAFF' as const,
};

export type Role = typeof ROLES.ADMIN | typeof ROLES.STAFF | null;

export interface User {
  username: string;
  role: Role;
}

interface AuthContextType {
  user: User | null;
  login: (username: string, password: string) => Promise<{ success: boolean; message?: string }>;
  logout: () => void;
  isAuthenticated: boolean;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
        const parsed = JSON.parse(storedUser);
        if (parsed && (parsed.role === 'ADMIN' || parsed.role === 'STAFF')) {
          setUser(parsed);
        } else {
          localStorage.removeItem('user');
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');
        }
      } catch(e) {
        localStorage.removeItem('user');
      }
    }
  }, []);

  const login = async (username: string, password: string): Promise<{ success: boolean; message?: string }> => {
    console.log(`[AuthContext] Đang thử đăng nhập với username: ${username}`);
    try {
      const response = await ApiService.Auth.login(username, password);
      if (response.success) {
        const authData = response.data;
        // API now returns "ADMIN" or "STAFF"
        const roleString = authData.role || 'STAFF';
        const parsedRole = roleString.toUpperCase();

        console.log(`[AuthContext] Đăng nhập thành công! Role: ${parsedRole}`);

        if (parsedRole === 'STAFF') {
          const branchUrl = authData.branchUrl;
          // DISABLED FOR LOCAL TESTING
          // if (branchUrl && !window.location.href.startsWith(branchUrl)) {
          //   console.warn(`[AuthContext] Branch URL mismatch. Expected: ${branchUrl}, Current: ${window.location.href}`);
          //   return { success: false, message: 'Sai địa chỉ chi nhánh. Vui lòng đăng nhập đúng đường dẫn của chi nhánh bạn.' };
          // }
        }

        const newUser = { username, role: parsedRole };
        setUser(newUser);
        localStorage.setItem('user', JSON.stringify(newUser));
        localStorage.setItem('access_token', authData.accessToken);
        localStorage.setItem('refresh_token', authData.refreshToken); // Add refresh token

        return { success: true };
      } else {
        console.log(`[AuthContext] Đăng nhập thất bại từ server: ${response.message}`);
        return { success: false, message: response.message || 'Sai thông tin đăng nhập' };
      }
    } catch (error: any) {
      console.error('[AuthContext] Login error (Network/500/401):', error);
      const errorMsg = error.response?.data?.message || 'Sai tên đăng nhập hoặc mật khẩu (Invalid credentials)';
      return { success: false, message: errorMsg };
    }
  };

  const logout = async () => {
    const refreshToken = localStorage.getItem('refresh_token');
    setUser(null);
    localStorage.removeItem('user');
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    try {
      if (refreshToken) {
        await ApiService.Auth.revoke(refreshToken);
      }
    } catch (e) {
      console.error('Logout failed', e);
    }
  };

  const hasRole = (role: string) => {
    return user?.role === role;
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user, hasRole }}>{children}</AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
