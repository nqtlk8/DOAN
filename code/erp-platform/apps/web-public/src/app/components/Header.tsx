'use client';
import { useState, ChangeEvent } from 'react';
import HeaderLayout from '../../components/ui/header/HeaderLayout';
import Logo from '../../components/ui/header/Logo';
import SearchBox from '../../components/ui/header/SearchBox';
import CategoryNav from '../../components/ui/header/CategoryNav';
import ContactInfo from '../../components/ui/header/ContactInfo';
import CartWidget from '../../components/ui/header/CartWidget';

export default function Header() {
  const [keyword, setKeyword] = useState<string>('');

  const handleSearch = () => {
    alert(`Tìm kiếm: ${keyword}`);
  };

  const handleKeywordChange = (e: ChangeEvent<HTMLInputElement>) => {
    setKeyword(e.target.value);
  };

  return (
    <HeaderLayout 
      logo={<Logo />}
      searchBox={
        <SearchBox 
          keyword={keyword}
          onKeywordChange={handleKeywordChange}
          onSearch={handleSearch}
        />
      }
      categoryNav={<CategoryNav />}
      contactInfo={<ContactInfo />}
      cartWidget={<CartWidget />}
    />
  );
}
