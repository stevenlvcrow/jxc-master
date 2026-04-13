export class ApiError extends Error {
  code: number;
  traceId?: string;
  status?: number;

  constructor(message: string, code = -1, traceId?: string, status?: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.traceId = traceId;
    this.status = status;
  }
}
