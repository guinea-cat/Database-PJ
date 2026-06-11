const API_BASE_URL = 'http://localhost:8080/api';

export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
  timestamp: string;
};

export class ApiError extends Error {
  code: number;

  constructor(code: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
  }
}

type QueryValue = string | number | boolean | null | undefined;

function buildUrl(path: string, params?: Record<string, QueryValue>) {
  const url = new URL(`${API_BASE_URL}${path}`);
  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value));
    }
  });
  return url.toString();
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new ApiError(response.status, `HTTP ${response.status}`);
  }
  const payload = (await response.json()) as ApiResponse<T>;
  if (payload.code !== 0) {
    throw new ApiError(payload.code, payload.message || '请求失败');
  }
  return payload.data;
}

export async function apiGet<T>(path: string, params?: Record<string, QueryValue>): Promise<T> {
  const response = await fetch(buildUrl(path, params), {
    method: 'GET',
  });
  return parseResponse<T>(response);
}

export async function apiPost<T, B extends object = Record<string, unknown>>(path: string, body: B): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return parseResponse<T>(response);
}
