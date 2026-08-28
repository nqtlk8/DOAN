import toast from 'react-hot-toast';

export const notify = {
  success: (message: string) => {
    toast.success(message);
  },
  error: (message: string) => {
    toast.error(message, {
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
