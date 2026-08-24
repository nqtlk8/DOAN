import { Phone } from 'lucide-react';

export default function ContactInfo() {
  return (
    <div className="flex flex-col text-right hidden lg:flex text-foreground">
      <div className="flex items-center justify-end gap-2 text-primary">
        <Phone size={20} strokeWidth={2.5} />
        <span className="text-xl font-black tracking-tight">0938.619.989</span>
      </div>
      <div className="text-sm font-semibold mt-0.5 text-gray-500">
        <span className="text-gray-700">Đại lý:</span> 0938.417.635
      </div>
    </div>
  );
}
