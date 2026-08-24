import Link from 'next/link';

export default function Logo() {
  return (
    <Link href="/" className="flex flex-col justify-center max-h-[70px] w-auto cursor-pointer select-none">
      <div className="text-3xl font-black text-primary whitespace-nowrap leading-none tracking-tight">
        VLXDgiatot<span className="text-accent">.com</span>
      </div>
      <div className="text-[13px] font-bold uppercase text-gray-500 mt-1 tracking-widest">
        Giá Tốt - Chính Hãng
      </div>
    </Link>
  );
}
