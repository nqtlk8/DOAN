import React, { Component, ErrorInfo, ReactNode } from 'react';

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
        <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
          <div className="bg-white p-8 rounded shadow-md max-w-lg w-full text-center">
            <h1 className="text-2xl font-bold text-red-600 mb-4">Đã có lỗi hệ thống xảy ra</h1>
            <p className="text-gray-600 mb-6">Xin lỗi, đã xảy ra lỗi trong quá trình tải trang. Vui lòng thử lại.</p>
            {this.state.error && (
              <pre className="text-left text-xs bg-gray-100 p-4 rounded overflow-auto text-red-800 mb-6 max-h-48">
                {this.state.error.toString()}
              </pre>
            )}
            <button
              onClick={() => window.location.reload()}
              className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
            >
              Tải lại trang
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
