export default function SectionTitle({ title }: { title: string }) {
  return (
    <div className="flex items-center mb-8 border-b-4 border-primary pb-2">
      <h2 className="text-2xl md:text-3xl font-black font-rubik text-foreground uppercase tracking-tight">{title}</h2>
    </div>
  );
}
