import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth } from './AuthContext';
import apiClient from '../services/apiClient';

// Mock apiClient
jest.mock('../services/apiClient');
const mockedApiClient = apiClient as jest.Mocked<typeof apiClient>;

const TestComponent = () => {
  const { user, login, logout, isAuthenticated } = useAuth();
  const [username, setUsername] = React.useState('');
  const [password, setPassword] = React.useState('');

  return (
    <div>
      <div data-testid="auth-status">{isAuthenticated ? 'Authenticated' : 'Not Authenticated'}</div>
      <div data-testid="user-role">{user ? user.role : 'None'}</div>
      <input 
        data-testid="username-input" 
        value={username} 
        onChange={(e) => setUsername(e.target.value)} 
      />
      <input 
        data-testid="password-input" 
        type="password"
        value={password} 
        onChange={(e) => setPassword(e.target.value)} 
      />
      <button data-testid="login-button" onClick={() => login(username, password)}>
        Login
      </button>
      <button data-testid="logout-button" onClick={logout}>
        Logout
      </button>
    </div>
  );
};

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
    jest.spyOn(window, 'alert').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('TC_USER_FE_01 & TC_USER_FE_02: should login successfully and save tokens to localStorage', async () => {
    mockedApiClient.post.mockResolvedValueOnce({
      data: {
        success: true,
        data: {
          accessToken: 'mock_access_token',
          refreshToken: 'mock_refresh_token',
          roles: ['ROLE_ADMIN']
        }
      }
    });

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Not Authenticated');

    await userEvent.type(screen.getByTestId('username-input'), 'admin');
    await userEvent.type(screen.getByTestId('password-input'), 'admin');
    await userEvent.click(screen.getByTestId('login-button'));

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');
    });

    expect(screen.getByTestId('user-role')).toHaveTextContent('admin');
    expect(localStorage.getItem('access_token')).toBe('mock_access_token');
    expect(localStorage.getItem('refresh_token')).toBe('mock_refresh_token');
    expect(localStorage.getItem('user')).toEqual(JSON.stringify({ username: 'admin', role: 'admin' }));
  });

  it('TC_USER_FE_04: should display alert on login failure', async () => {
    mockedApiClient.post.mockRejectedValueOnce({
      response: {
        data: {
          success: false,
          message: 'Invalid credentials',
          errors: ['Wrong password']
        }
      }
    });

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await userEvent.type(screen.getByTestId('username-input'), 'admin');
    await userEvent.type(screen.getByTestId('password-input'), 'wrongpassword');
    await userEvent.click(screen.getByTestId('login-button'));

    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledWith('Invalid credentials');
    });

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Not Authenticated');
    expect(localStorage.getItem('access_token')).toBeNull();
  });
});
