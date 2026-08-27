import Link from 'next/link';

interface ViewAllButtonProps {
  linkTo: string;
}

export default function ViewAllButton({ linkTo }: ViewAllButtonProps) {
  return (
    <div className="flex justify-center mt-10">
      <Link 
        href={linkTo}
        className="bg-white border-4 border-foreground text-foreground px-8 py-4 text-lg font-black font-rubik uppercase flex items-center justify-center gap-2 hover:bg-slate-100 shadow-[4px_4px_0px_0px_rgba(30,41,59,1)]"
      >
        XEM TẤT CẢ SẢN PHẨM &gt;
      </Link>
    </div>
  );
}
