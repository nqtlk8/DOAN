import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Login } from '../Login';
import * as AuthContext from '../../../context/AuthContext';
import { ENV } from '../../../config/env';

// Mock AuthContext

vi.mock('../../../config/env', () => ({
  ENV: { isDev: true }
}));

vi.mock('../../../context/AuthContext', () => ({
  useAuth: vi.fn(),
  AuthProvider: ({ children }: any) => <div>{children}</div>,
  ROLES: { ADMIN: 'ADMIN', STAFF: 'STAFF' }
}));

describe('Login Component', () => {
  const mockLogin = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    (AuthContext.useAuth as any).mockReturnValue({
      login: mockLogin,
      user: null,
      isAuthenticated: false
    });
  });

  it('U-LOGIN-01: Toggles password visibility', () => {
    render(<Login />);
    
    const passwordInput = screen.getByTestId('login-password');
    const toggleBtn = screen.getByTestId('login-toggle-password');
    
    // Initial state
    expect(passwordInput).toHaveAttribute('type', 'password');
    
    // Click to show
    fireEvent.click(toggleBtn);
    expect(passwordInput).toHaveAttribute('type', 'text');
    
    // Click to hide
    fireEvent.click(toggleBtn);
    expect(passwordInput).toHaveAttribute('type', 'password');
  });

  it('U-LOGIN-02: Shows dev hint when ENV.isDev = true', () => {
    const originalIsDev = ENV.isDev;
    (ENV as any).isDev = true;
    
    render(<Login />);
    expect(screen.getByTestId('login-dev-hint')).toBeInTheDocument();
    
    (ENV as any).isDev = originalIsDev;
  });

  it('U-LOGIN-03: Hides dev hint when ENV.isDev = false', () => {
    const originalIsDev = ENV.isDev;
    (ENV as any).isDev = false;
    
    render(<Login />);
    expect(screen.queryByTestId('login-dev-hint')).not.toBeInTheDocument();
    
    (ENV as any).isDev = originalIsDev;
  });

  it('U-LOGIN-04: Calls login with correct credentials', async () => {
    mockLogin.mockResolvedValue({ success: true });
    
    render(<Login />);
    
    const usernameInput = screen.getByTestId('login-username');
    const passwordInput = screen.getByTestId('login-password');
    const submitBtn = screen.getByTestId('login-submit');
    
    fireEvent.change(usernameInput, { target: { value: 'testuser' } });
    fireEvent.change(passwordInput, { target: { value: 'testpass' } });
    fireEvent.click(submitBtn);
    
    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('testuser', 'testpass');
    });
  });
});
