import type { Metadata } from 'next';
import './globals.css';
import Header from './components/Header';
import Providers from './providers';
import React from 'react';

export const metadata: Metadata = {
  title: 'VLXD Giá Tốt - Vật Liệu Xây Dựng & Trang Trí Nội Thất',
  description: 'Chuyên cung cấp Vật Liệu Xây Dựng & Trang Trí Nội Thất giá tốt, chính hãng.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&display=swap" rel="stylesheet" />
      </head>
      <body className="font-inter bg-slate-50 text-slate-800 antialiased min-h-screen flex flex-col">
        <Providers>
          <Header />
          {children}
        </Providers>
      </body>
    </html>
  );
}
