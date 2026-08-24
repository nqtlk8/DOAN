REACT PROJECT DEVELOPMENT RULES (TSX + TAILWIND)
1. CORE ARCHITECTURE PRINCIPLES
Tuân thủ nghiêm ngặt việc tách biệt giữa Giao diện (UI), Logic (Business Logic) và Bố cục (Layout):

Atomic UI Components (Tĩnh): Nằm trong src/components/ui/. Chỉ chứa giao diện, nhận dữ liệu qua props. Không chứa Business Logic, không gọi API.

Feature/Client Components (Động): Nằm trong thư mục của tính năng (ví dụ: src/app/home/components/). Chứa logic xử lý (useState, useEffect), quản lý sự kiện và gọi Services.

Page Components (Bố cục): File page.tsx đóng vai trò là "container" để lắp ghép các Component. Hạn chế tối đa code CSS chi tiết tại đây.

Services (Dữ liệu): Nằm trong src/services/. Chứa các hàm gọi API (Axios/Fetch).

2. FOLDER STRUCTURE STANDARD
Mọi file mới phải được đặt đúng vị trí theo sơ đồ sau:

src/
├── app/                  # Chứa cấu trúc theo Route (Next.js Style)
│   └── [feature-name]/   # Ví dụ: home, login, products
│       ├── page.tsx      # File lắp ghép chính của trang
│       ├── components/   # Các Component động chỉ dùng riêng cho trang này
│       └── services/     # (Optional) API riêng cho tính năng này
├── components/           # UI Library dùng chung toàn dự án
│   └── ui/               # Button, Input, Card, Modal (Pure UI)
├── services/             # API dùng chung (Auth, User, v.v.)
├── hooks/                # Custom React Hooks
├── styles/               # Global CSS, Tailwind Config
└── App.tsx               # Cổng vào chính, quản lý Layout tổng

3. CODING STANDARDS & TYPESCRIPT
File Extension: Luôn sử dụng .tsx cho Component và .ts cho logic thuần.

Component Definition: Sử dụng export default function ComponentName() {}.

TypeScript Strict:

Luôn định nghĩa Interface/Type cho Props.

Sử dụng đúng Type cho Event (ví dụ: ChangeEvent<HTMLInputElement>).

Tailwind CSS:

Ưu tiên dùng Utility classes của Tailwind thay vì CSS rời.

Sử dụng các class Responsive (sm:, md:, lg:) để đảm bảo giao diện chạy được trên Mobile.

4. COMPONENT INTERACTION RULES
Top-Down Data Flow: Dữ liệu truyền từ Page xuống UI Component qua props.

Event Bubbling: UI Component báo cáo hành động (click, change) lên Client Component thông qua các hàm callback trong props (ví dụ: onClick, onDataSubmit).

Naming Convention:

Component: PascalCase (ví dụ: ProductCard.tsx).

Hàm xử lý: camelCase và bắt đầu bằng handle (ví dụ: handleSearch).

5. SPECIFIC TECH STACK INSTRUCTIONS
Styling: Luôn kiểm tra file index.css để đảm bảo không có CSS mặc định của Vite gây xung đột màu nền. Ưu tiên nền trắng (bg-white) và text tối (text-gray-800).

Vite Configuration: Ưu tiên nhận diện file .tsx. Nếu import thư mục, Agent phải tự hiểu trỏ vào page.tsx hoặc index.tsx bên trong.