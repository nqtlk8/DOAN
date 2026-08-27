# API CONTRACT (FRONTEND - BACKEND)

Tài liệu này định nghĩa chuẩn giao tiếp API giữa hệ thống React Frontend và Java Spring Backend dựa trên OpenAPI Swagger mới nhất.

## 1. Chuẩn Response Toàn Cục
Mọi REST API từ Backend đều bọc dữ liệu trong cấu trúc chuẩn sau (trừ khi có ghi chú khác):
`json
{
  "success": boolean,
  "data": T | null,
  "message": string,
  "errors": Array<string> | null
}
`

## 2. API Endpoints theo Domain

### Domain: Goods Return
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/goods-returns | N/A | any |  |
| POST | /api/v1/goods-returns | any | any | Táº¡o phiáº¿u tráº£ hÃ ng nhÃ¡p (DRAFT) |
| POST | /api/v1/goods-returns/{id}/confirm | N/A | any | XÃ¡c nháº­n tráº£ hÃ ng (CONFIRM) vÃ  hoÃ n kho |
| GET | /api/v1/goods-returns/{id} | N/A | any |  |

### Domain: Inbound Receipt
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/inventory/inbound | N/A | any |  |
| POST | /api/v1/inventory/inbound | any | any | Tạo phiếu nhập kho (DRAFT) |
| POST | /api/v1/inventory/inbound/{id}/confirm | N/A | any | Xác nhận phiếu nhập kho (CONFIRM) và tăng tồn kho |
| GET | /api/v1/inventory/inbound/{id} | N/A | any |  |

### Domain: auth-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| POST | /api/v1/auth/revoke | any | any |  |
| POST | /api/v1/auth/refresh | any | any |  |
| POST | /api/v1/auth/login | any | any |  |

### Domain: branch-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/branches/{id} | N/A | any |  |
| PUT | /api/v1/branches/{id} | any | any |  |
| DELETE | /api/v1/branches/{id} | N/A | any |  |
| GET | /api/v1/branches | N/A | any |  |
| POST | /api/v1/branches | any | any |  |

### Domain: customer-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/customers | N/A | any |  |
| POST | /api/v1/customers | any | N/A |  |

### Domain: customer-product-price-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/customer-prices/{customerId}/product/{productId} | N/A | any |  |

### Domain: dashboard-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/analytics/export/excel | N/A | Array<string> |  |
| GET | /api/v1/analytics/dashboard | N/A | any |  |

### Domain: health-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/health | N/A | string |  |

### Domain: product-read-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/catalog/products/{id} | N/A | any |  |
| GET | /api/v1/catalog/products | N/A | any |  |

### Domain: product-write-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| PUT | /api/v1/catalog/products/{id} | any | any |  |
| POST | /api/v1/catalog/products | any | any |  |

### Domain: public-catalog-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/public/catalog/products | N/A | any |  |
| GET | /api/v1/public/catalog/products/{id} | N/A | any |  |

### Domain: receivable-debt-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/receivable-debts | N/A | any |  |

### Domain: replication-status-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/admin/system/replication-status | N/A | any |  |

### Domain: sales-invoice-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/sales-invoices | N/A | any |  |
| POST | /api/v1/sales-invoices | any | N/A |  |
| POST | /api/v1/sales-invoices/{id}/confirm | N/A | any |  |
| GET | /api/v1/sales-invoices/{id} | N/A | any |  |

### Domain: stock-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/inventory/stock | N/A | any |  |

### Domain: supplier-controller
| Method | Path | Request Body | Response Data (T) | Summary |
|---|---|---|---|---|
| GET | /api/v1/suppliers | N/A | Array<any> |  |
| POST | /api/v1/suppliers | any | any |  |
| GET | /api/v1/suppliers/{id} | N/A | any |  |

## 3. Cấu trúc DTO (Data Transfer Objects) Tham khảo
Một số DTO chính được sử dụng trong Request/Response:

