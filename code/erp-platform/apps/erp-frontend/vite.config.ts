import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  test: { 
    environment: 'jsdom', 
    setupFiles: ['./vitest.setup.ts'], 
    globals: true,
    include: ['src/**/*.test.tsx', 'src/**/*.test.ts']
  },
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    // Dev: auth + master data + quan tri -> HQ (8080); API giao dich -> Branch (8081).
    // Doi dich bang VITE_HQ_TARGET / VITE_BRANCH_TARGET. Tren docker dung code/nginx-branch-tpX.conf.
    // Luu y: o dev, DOC master data cung di HQ (don gian hoa, khong phu thuoc replication).
    proxy: {
      '^/api/v1/(auth|branches|analytics|admin|catalog|customers|suppliers)(/|$|\\?)': {
        target: process.env.VITE_HQ_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api': {
        target: process.env.VITE_BRANCH_TARGET || 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
});

