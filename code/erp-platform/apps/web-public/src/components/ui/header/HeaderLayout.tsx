import { ReactNode } from 'react';

interface HeaderLayoutProps {
  logo: ReactNode;
  searchBox: ReactNode;
  categoryNav: ReactNode;
  contactInfo: ReactNode;
  cartWidget: ReactNode;
}

export default function HeaderLayout({ 
  logo, 
  searchBox, 
  categoryNav, 
  contactInfo, 
  cartWidget 
}: HeaderLayoutProps) {
  return (
    <header className="w-full sticky top-0 z-50 flex flex-col shadow-md">
      {/* White Main Header (Logo, Search, Contact, Cart) */}
      <div className="w-full bg-white py-3">
        <div className="w-full px-4 flex flex-col md:flex-row items-center justify-between gap-6 max-w-7xl mx-auto">
          {/* Logo */}
          <div className="w-full md:w-[20%] flex items-center justify-center md:justify-start">
            {logo}
          </div>

          {/* Search Box */}
          <div className="w-full md:w-[50%] flex justify-center">
            <div className="w-full max-w-2xl">
              {searchBox}
            </div>
          </div>

          {/* Contact & Cart */}
          <div className="w-full md:w-[30%] flex items-center justify-center md:justify-end gap-6">
            {contactInfo}
            {cartWidget}
          </div>
        </div>
      </div>

      {/* Red Top Navigation Ribbon */}
      <div className="w-full bg-gradient-to-r from-primary to-red-600 border-t border-red-500 hidden md:block">
        <div className="max-w-7xl mx-auto px-4">
          {categoryNav}
        </div>
      </div>
    </header>
  );
}
