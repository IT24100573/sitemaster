/** The error body every API failure returns (see GlobalExceptionHandler). */
export interface ApiErrorBody {
  status: number;
  message: string;
  fieldErrors: Record<string, string>;
  globalErrors: string[];
}

export interface MessageResponse {
  message: string;
}
