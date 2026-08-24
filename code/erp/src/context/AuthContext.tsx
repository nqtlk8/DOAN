import React, { createContext, useContext, useState, useEffect } from 'react';
import apiClient from '../services/apiClient';

export type Role = 'admin' | 'sales' | null;

export interface User {
  username: string;
  role: Role;
}

interface AuthContextType {
  user: User | null;
  login: (username: string, password: string) => Promise<{success: boolean, message?: string}>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  
  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
  }, []);

  const login = async (username: string, password: string): Promise<{success: boolean, message?: string}> => {
    console.log(`[AuthContext] Đang thử đăng nhập với username: ${username}`);
    try {
      const response = await apiClient.post('/auth/login', { username, password });
      if (response.data.success) {
        const authData = response.data.data;
        const roleString = authData.user?.role || authData.role || 'ROLE_SALES';
        const parsedRole = roleString.replace('ROLE_', '').toLowerCase() as Role;
        const newUser = { username, role: parsedRole };
        
        console.log(`[AuthContext] Đăng nhập thành công! Role: ${parsedRole}`);
        setUser(newUser);
        localStorage.setItem('user', JSON.stringify(newUser));
        localStorage.setItem('access_token', authData.accessToken);
        localStorage.setItem('refresh_token', authData.refreshToken);
        
        return { success: true };
      } else {
        console.log(`[AuthContext] Đăng nhập thất bại từ server: ${response.data.message}`);
        return { success: false, message: response.data.message || 'Sai thông tin đăng nhập' };
      }
    } catch (error: any) {
      console.error('[AuthContext] Login error (Network/500/401):', error);
      const errorMsg = error.response?.data?.message || 'Sai tên đăng nhập hoặc mật khẩu (Invalid credentials)';
      return { success: false, message: errorMsg };
    }
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem('user');
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
