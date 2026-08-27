# Tiêu điểm các Sprints (Sprints Index)

Tài liệu này lưu trữ danh sách các tài liệu sprint đã hoàn thành trong dự án, giúp theo dõi quá trình tiến hóa của kiến trúc và các quyết định kỹ thuật theo thời gian.

## Danh sách các Sprints

| Sprint | Tên Sprint / Chủ đề chính | Ngày Hoàn Thành | Tóm tắt |
|---|---|---|---|
| [0.1](./sprint-0.1-n-instance-rs256.md) | Nền tảng Kiến trúc N-Instance & JWT RS256 | 2026-08-19 | Thiết lập khung Spring Boot đa bối cảnh, bảo mật JWT cục bộ RS256, cách ly Redis, Docker Compose 3 nodes. |
| [1.1](./sprint-1.1-refactoring-and-bug-fixes.md) | Refactoring & Bug Fixes | 2026-08-19 | Giải quyết triệt để state leakage H2 test, hoàn thiện JWT, bảo mật Auth và Branch Scoping RBAC. |
| [1.2](./sprint-1.2-catalog.md) | Catalog & Logical Replication | 2026-08-19 | EAV 3NF, Denormalize JSONB, PostgreSQL Pub/Sub đồng bộ HQ-Branch, Domain Encapsulation. |
| [2.1](./sprint-2.1-inventory.md) | Inventory Core & Concurrency | 2026-08-20 | Xây dựng Tồn kho trung tâm, Phiếu nhập/xuất, Costing, Optimistic Locking & Service-level Retry. |
| [2.2](./sprint-2.2-sales-idempotency.md) | Sales & Idempotency | 2026-08-21 | Bán hàng cốt lõi (Sales Invoice), Snapshot công nợ, Giá riêng khách hàng, Cấu trúc Idempotency Aspect chặn lặp. |
| [2.3](./sprint-2.3-procurement-idempotency.md) | Procurement & Idempotency | 2026-08-21 | Module Đặt hàng nhà cung cấp, Công nợ NCC, Chuyển kho nội bộ và Auto-receive PO. |
| [2.4](./sprint-2.4-customer-order.md) | Customer Order & Goods Return | 2026-08-21 | Đơn đặt hàng (Customer Order), Tự động chuyển đổi thành Invoice, Trả hàng (Goods Return). |
| [2.5](./sprint-2.5-phase2-completion.md) | Phase 2 Completion & Core Stabilizations | 2026-08-23 | Đóng gói Phase 2: Fix Idempotency atomicity/hash, Spring-retry concurrency, Validation hàng trả lại, và Security tech debt. |
| [4.0](./sprint-4-frontend-backend-integration-refactor.md) | Frontend Monorepo, API Facade & Backend ID Sync | 2026-08-24 | Xây dựng Workspace Monorepo, tích hợp BFF Proxy, chuyển đổi UUID sang Long (branchId, productId), sửa lỗi Test & bảo mật API v1. |

- [Sprint 5: OpenAPI Integration & TypeScript Strict Typing](./sprint-5-openapi-integration.md)