interface ResultsToolbarProps {
  totalItems: number;
  currentPage: number;
  limit: number;
}

export default function ResultsToolbar({ totalItems, currentPage, limit }: ResultsToolbarProps) {
  const start = totalItems === 0 ? 0 : (currentPage - 1) * limit + 1;
  const end = Math.min(currentPage * limit, totalItems);

  return (
    <div className="flex flex-col md:flex-row justify-between items-center bg-white py-4 px-4 border-[3px] border-border mb-6 shadow-[4px_4px_0px_0px_rgba(30,41,59,0.1)] gap-4">
      <div className="text-base font-bold text-primary uppercase">
        Hiển thị {start}–{end} của {totalItems} kết quả
      </div>
      <div className="flex items-center gap-2 w-full md:w-auto">
        <select className="w-full md:w-auto border-2 border-primary rounded-sm px-4 py-2 text-base font-bold outline-none focus:border-accent bg-background">
          <option>Mặc định</option>
          <option>Giá thấp đến cao</option>
          <option>Giá cao xuống thấp</option>
          <option>Mới nhất</option>
        </select>
      </div>
    </div>
  );
}
