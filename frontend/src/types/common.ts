/** Mirrors PageResponse on the server. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Mirrors LookupOption on the server (dropdown data). */
export interface LookupOption {
  value: string;
  label: string;
}

export type SortDirection = 'asc' | 'desc';

export interface PageParams {
  page: number;
  size: number;
  sort?: string;
  direction?: SortDirection;
  search?: string;
}
