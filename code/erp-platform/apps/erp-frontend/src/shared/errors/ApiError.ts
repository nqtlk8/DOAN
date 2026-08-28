export interface NormalizedApiError {
  status?: number;
  code?: string;
  message: string;
  details?: unknown;
}

export class ApiError extends Error {
  public status?: number;
  public code?: string;
  public details?: unknown;

  constructor(normalized: NormalizedApiError) {
    super(normalized.message);
    this.name = 'ApiError';
    this.status = normalized.status;
    this.code = normalized.code;
    this.details = normalized.details;
  }
}
