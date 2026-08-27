// src/components/ui/ProductCard.jsx
import './ProductCard.css';

export default function ProductCard({ image, name, price }) {
  return (
    <div className="product-card">
      <img src={image} alt={name} />
      <h3>{name}</h3>
      <p className="price">{price.toLocaleString()} đ</p>
      <button>Thêm vào giỏ</button>
    </div>
  );
}