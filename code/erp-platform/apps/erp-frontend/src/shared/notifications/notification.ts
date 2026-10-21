import toast from 'react-hot-toast';

export const notify = {
  success: (message: string) => {
    toast.success(message);
  },
  error: (message: string) => {
    // id = nội dung: cùng một lỗi được báo từ interceptor axios và từ form chỉ hiện một thông báo.
    toast.error(message, {
      id: `error:${message}`,
      duration: 5000,
    });
  },
  warning: (message: string) => {
    toast(message, {
      icon: '⚠️',
      duration: 4000,
    });
  },
  info: (message: string) => {
    toast(message, {
      duration: 3000,
    });
  },
};
