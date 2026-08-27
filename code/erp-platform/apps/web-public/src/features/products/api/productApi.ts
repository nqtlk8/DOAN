import type { components } from '@erp/api-contract';

type SalesInvoiceCreateDto = components['schemas']['SalesInvoiceCreateDto'];
type ApiResponseListProductResponseDto = components['schemas']['ApiResponseListProductResponseDto'];
type ApiResponseUUID = components['schemas']['ApiResponseUUID'];
type ProductResponseDto = components['schemas']['ProductResponseDto'];

// Use Next.js native fetch for caching and ISR
const BACKEND_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const getProductListing = async (search?: string, categoryId?: number, page: number = 0, size: number = 20): Promise<ApiResponseListProductResponseDto> => {
  try {
    const url = new URL(BACKEND_URL + '/api/v1/public/catalog/products');
    if (search) url.searchParams.append('search', search);
    if (categoryId) url.searchParams.append('categoryId', categoryId.toString());
    url.searchParams.append('page', page.toString());
    url.searchParams.append('size', size.toString());

    // Revalidate every 60 seconds (ISR)
    const res = await fetch(url.toString(), {
      next: { revalidate: 60 }
    });
    
    if (!res.ok) {
      throw new Error('Failed to fetch products');
    }
    const data = await res.json();
    return data;
  } catch (error) {
    console.error('Error fetching products:', error);
    return { success: false, data: [] };
  }
};

export const getFeaturedProducts = async (rootSlug: string, limit: number = 12): Promise<ProductResponseDto[]> => {
  try {
    const url = new URL(BACKEND_URL + '/api/v1/public/catalog/products');
    url.searchParams.append('size', limit.toString());
    
    const res = await fetch(url.toString(), {
      next: { revalidate: 300 } // Cache featured products for 5 minutes
    });
    
    if (!res.ok) {
      throw new Error('Failed to fetch featured products');
    }
    const result = await res.json();
    return result.success && result.data ? result.data : [];
  } catch (error) {
    console.error('Error fetching featured products:', error);
    return [];
  }
};

export const submitRfq = async (payload: SalesInvoiceCreateDto): Promise<ApiResponseUUID> => {
  const url = BACKEND_URL + '/api/v1/public/rfqs';
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      // 'Idempotency-Key': crypto.randomUUID() // Should be passed from client
    },
    body: JSON.stringify(payload)
  });
  
  if (!res.ok) {
    throw new Error('Failed to submit RFQ');
  }
  return res.json();
};