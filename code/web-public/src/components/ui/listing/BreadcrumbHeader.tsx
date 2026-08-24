import Link from 'next/link';

interface BreadcrumbHeaderProps {
  title: string;
}

export default function BreadcrumbHeader({ title }: BreadcrumbHeaderProps) {
  return (
    <div className="mb-6 border-b-4 border-primary pb-4">
      <h1 className="text-4xl md:text-5xl font-black font-rubik text-foreground mb-4 uppercase tracking-tight">{title}</h1>
      <div className="text-base text-primary font-bold flex items-center gap-2 uppercase tracking-wider">
        <Link href="/" className="hover:text-accent hover:underline transition-none">TRANG CHỦ</Link>
        <span>/</span>
        <span className="text-accent">{title}</span>
      </div>
    </div>
  );
}
