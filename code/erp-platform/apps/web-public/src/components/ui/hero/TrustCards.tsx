import { ShieldCheck, Truck, Tags } from 'lucide-react';

const COMMITMENTS = [
  {
    icon: ShieldCheck,
    title: 'Hàng chính hãng 100%',
    desc: 'Chứng nhận chất lượng đầy đủ'
  },
  {
    icon: Tags,
    title: 'Giá sỉ tại kho',
    desc: 'Báo giá tốt nhất thị trường'
  },
  {
    icon: Truck,
    title: 'Giao nhanh trong ngày',
    desc: 'Hỗ trợ bốc xếp tận công trình'
  }
];

export default function TrustCards() {
  return (
    <div className="w-full max-w-7xl mx-auto px-4 -mt-16 md:-mt-24 relative z-20 mb-16">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {COMMITMENTS.map((item, index) => (
          <div 
            key={index} 
            className="bg-white rounded-xl shadow-lg p-6 flex items-center gap-4 border border-gray-100 hover:shadow-xl transition-shadow"
          >
            <div className="w-16 h-16 rounded-full bg-red-50 flex items-center justify-center flex-shrink-0">
              <item.icon className="text-primary" size={32} strokeWidth={2} />
            </div>
            <div className="flex flex-col">
              <h3 className="font-bold text-gray-900 text-lg">{item.title}</h3>
              <p className="text-gray-500 text-sm">{item.desc}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
