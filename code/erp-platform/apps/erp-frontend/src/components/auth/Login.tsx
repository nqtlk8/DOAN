import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Lock, User, ShoppingCart, PackageCheck, Wallet, BarChart3, Eye, EyeOff } from 'lucide-react';
import { BRAND } from '../../config/brand';
import { ENV } from '../../config/env';

export const Login: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    const result = await login(username, password);
    if (!result.success) {
      setError(result.message || 'Đăng nhập thất bại');
    }
    setIsLoading(false);
  };

  const featureList = [
    { icon: <ShoppingCart size={18} />, text: 'Bán hàng nhanh, in hóa đơn' },
    { icon: <PackageCheck size={18} />, text: 'Nhập kho & theo dõi tồn' },
    { icon: <Wallet size={18} />, text: 'Công nợ khách hàng' },
    { icon: <BarChart3 size={18} />, text: 'Báo cáo doanh thu' },
  ];

  return (
    <div className="grid min-h-screen lg:grid-cols-[55%_45%] bg-surface">
      {/* Panel trái (chỉ hiện trên màn hình lớn) */}
      <div className="hidden lg:flex flex-col justify-between p-12 text-white bg-primary bg-gradient-to-b from-primary to-[#1E3A8A]">
        <div>
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 bg-white rounded-lg flex items-center justify-center">
              <span className="text-primary-dark font-bold text-xl">{BRAND.mark}</span>
            </div>
            <div>
              <h1 className="text-[22px] font-semibold leading-tight">{BRAND.name}</h1>
              <p className="text-[15px] text-white/80">Quản lý bán hàng, kho và công nợ đa chi nhánh</p>
            </div>
          </div>

          <div className="mt-16 space-y-5">
            {featureList.map((f, i) => (
              <div key={i} className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-full bg-white/10 flex items-center justify-center shrink-0">
                  {f.icon}
                </div>
                <span className="text-[14px] text-white/90 font-medium">{f.text}</span>
              </div>
            ))}
          </div>
        </div>

        <div>
          <p className="text-[12px] text-white/60">{BRAND.copyright}</p>
        </div>
      </div>

      {/* Panel phải (Form đăng nhập) */}
      <div className="flex items-center justify-center p-6 relative">
        <div className="w-full max-w-[380px]">
          {/* Header (hiển thị trên màn hình nhỏ) */}
          <div className="lg:hidden flex flex-col items-center mb-8">
            <div className="w-10 h-10 bg-primary rounded-lg flex items-center justify-center mb-3">
              <span className="text-white font-bold text-xl">{BRAND.mark}</span>
            </div>
            <h1 className="text-[22px] font-semibold text-ink text-center">{BRAND.name}</h1>
          </div>

          <div className="mb-8">
            <h2 className="text-[24px] font-semibold text-ink mb-1">Đăng nhập</h2>
            <p className="text-[14px] text-ink-muted">Nhập tài khoản được cấp để tiếp tục</p>
          </div>

          {error && (
            <div data-testid="login-error" className="mb-5 p-3 bg-red-50 border border-red-200 rounded-md">
              <p className="text-sm text-red-600 font-medium">{error}</p>
            </div>
          )}

          <form data-testid="login-form" onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-[13px] font-medium text-ink mb-1.5">Tên đăng nhập</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <User className="h-[18px] w-[18px] text-ink-subtle" />
                </div>
                <input
                  data-testid="login-username"
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="block w-full h-[40px] pl-9 pr-3 bg-white border border-line-strong rounded-[6px] text-[14px] text-ink placeholder:text-ink-lighter focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/10 transition-all"
                  placeholder="admin"
                  required
                  autoFocus
                />
              </div>
            </div>

            <div>
              <label className="block text-[13px] font-medium text-ink mb-1.5">Mật khẩu</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="h-[18px] w-[18px] text-ink-subtle" />
                </div>
                <input
                  data-testid="login-password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="block w-full h-[40px] pl-9 pr-10 bg-white border border-line-strong rounded-[6px] text-[14px] text-ink placeholder:text-ink-lighter focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/10 transition-all"
                  placeholder="••••••••"
                  required
                />
                <button
                  type="button"
                  data-testid="login-toggle-password"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-ink-subtle hover:text-ink transition-colors focus:outline-none"
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button
              data-testid="login-submit"
              type="submit"
              disabled={isLoading}
              className="w-full h-[40px] mt-2 flex justify-center items-center px-4 border border-transparent rounded-[6px] text-[14px] font-medium text-white bg-primary hover:bg-primary-dark focus:outline-none focus:ring-4 focus:ring-primary/10 transition-all disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {isLoading ? 'Đang xử lý...' : 'Đăng nhập'}
            </button>
          </form>

          {ENV.isDev && (
            <div data-testid="login-dev-hint" className="mt-8 pt-4 border-t border-line text-[13px] text-ink-muted">
              <p className="font-medium text-ink mb-1">Tài khoản test (Dev Mode):</p>
              <ul className="space-y-1">
                <li>• Quản trị: <strong className="text-ink">admin</strong> / password</li>
                <li>• Nhân viên: <strong className="text-ink">staff_tp1</strong> / password</li>
              </ul>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
