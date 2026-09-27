import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error:', error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-app p-4">
          <div className="card p-8 max-w-lg w-full text-center flex flex-col items-center">
            <div className="w-16 h-16 bg-danger-soft text-danger rounded-full flex items-center justify-center mb-4">
              <AlertTriangle size={32} />
            </div>
            <h1 className="text-[18px] font-semibold text-ink mb-2">Đã có lỗi hệ thống xảy ra</h1>
            <p className="text-[14px] text-ink-muted mb-6">Xin lỗi, đã xảy ra lỗi trong quá trình tải trang. Vui lòng thử lại.</p>
            {this.state.error && (
              <div className="w-full text-left bg-slate-50 border border-line p-3 rounded-md overflow-auto mb-6 max-h-48">
                <pre className="text-[12px] text-danger/80 whitespace-pre-wrap font-mono">
                  {this.state.error.toString()}
                </pre>
              </div>
            )}
            <button
              onClick={() => window.location.reload()}
              className="btn-primary h-9 px-6 flex items-center gap-2"
            >
              <RefreshCw size={16} />
              <span>Tải lại trang</span>
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
