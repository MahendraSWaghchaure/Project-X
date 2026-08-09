// Mirrors backend/common/.../web/ApiResponse.java and ApiError.java - every endpoint replies in this envelope.
export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  fieldErrors: ApiFieldError[];
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: ApiErrorBody | null;
  timestamp: string;
}
