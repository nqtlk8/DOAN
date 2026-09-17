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
    const accessToken = localStorage.getItem('access_token');
    if (storedUser && accessToken) {
      try {
        const parsed = JSON.parse(storedUser);
        let validSession = false;

        // Check valid UUID in token (Bug F)
        const base64Url = accessToken.split('.')[1];
        if (base64Url) {
          const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
          const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
              return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
          }).join(''));
          const payload = JSON.parse(jsonPayload);
          const isValidUuid = payload?.sub?.match(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i);
          if (isValidUuid && (parsed.role === 'ADMIN' || parsed.role === 'STAFF')) {
             validSession = true;
          }
        }
        
        // Check branch_url for STAFF
        const enforceBranchUrl = import.meta.env.VITE_ENFORCE_BRANCH_URL !== 'false';
        const branchUrl = localStorage.getItem('branch_url');
        if (parsed.role === 'STAFF' && enforceBranchUrl && branchUrl) {
           const expected = new URL(branchUrl).origin;
           if (expected !== window.location.origin) {
             validSession = false;
           }
        }

        if (validSession) {
          setUser(parsed);
        } else {
          localStorage.removeItem('user');
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');
          localStorage.removeItem('branch_url');
        }
      } catch(e) {
        localStorage.removeItem('user');
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('branch_url');
      }
    } else {
       localStorage.removeItem('user');
       localStorage.removeItem('access_token');
       localStorage.removeItem('refresh_token');
       localStorage.removeItem('branch_url');
    }
  }, []);

  const login = async (username: string, password: string): Promise<{ success: boolean; message?: string }> => {
    try {
      const response = await ApiService.Auth.login(username, password);
      if (response.success) {
        const authData = response.data;
        // API now returns "ADMIN" or "STAFF"
        const roleString = authData.role || 'STAFF';
        const parsedRole = roleString.toUpperCase();


        if (parsedRole === 'STAFF') {
          const enforceBranchUrl = import.meta.env.VITE_ENFORCE_BRANCH_URL !== 'false';
          const branchUrl = authData.branchUrl;
          if (enforceBranchUrl && branchUrl) {
            const expected = new URL(branchUrl).origin;
            if (expected !== window.location.origin) {
              return { success: false, message: `Tài khoản nhân viên chi nhánh phải đăng nhập tại ${expected}` };
            }
          }
        }

        const newUser = { username, role: parsedRole };
        setUser(newUser);
        localStorage.setItem('user', JSON.stringify(newUser));
        localStorage.setItem('access_token', authData.accessToken);
        localStorage.setItem('refresh_token', authData.refreshToken);
        if (authData.branchUrl) {
          localStorage.setItem('branch_url', authData.branchUrl);
        }

        return { success: true };
      } else {
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
    localStorage.removeItem('branch_url');
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
