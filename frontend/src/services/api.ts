import type {
  BudgetRequest,
  BudgetResponse,
  CategoryResponse,
  CategoryRequest,
  TransactionResponse,
  TransactionRequest,
  GoalResponse,
  GoalRequest,
  MonthlyReportResponse,
  YearlyReportResponse,
  AiAdviceResponse,
  WalletResponse,
  WalletRequest,
  WalletBankAccountRequest
} from '../types';

const BASE_URL = '/api';

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

/**
 * Hàm gọi API tổng quát, tự động gửi kèm Cookie phiên đăng nhập (JSESSIONID)
 */
async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const config: RequestInit = {
    ...options,
    credentials: 'include', // Giữ cookie JSESSIONID qua các request
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  };

  const response = await fetch(`${BASE_URL}${endpoint}`, config);

  if (!response.ok) {
    let errorMsg = `Lỗi ${response.status}: ${response.statusText}`;
    try {
      const errorJson = await response.json();
      if (errorJson.error) {
        errorMsg = errorJson.error;
      } else if (errorJson.message) {
        errorMsg = errorJson.message;
      } else if (typeof errorJson === 'object') {
        errorMsg = Object.values(errorJson).join(', ');
      }
    } catch {
      // Bỏ qua nếu response không phải JSON
    }
    throw new ApiError(errorMsg, response.status);
  }

  // Nếu HTTP 204 No Content hoặc response rỗng
  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

// ----------------------------------------------------
// 1. AUTH API (Đăng ký, Đăng nhập, Đăng xuất)
// ----------------------------------------------------
export const authApi = {
  register: (data: { username: string; password: string; fullName: string; phoneNumber: string }) =>
    request<{ message: string }>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  login: (data: { username: string; password: string }) =>
    request<{ message: string; username: string }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  logout: () =>
    request<{ message: string }>('/auth/logout', {
      method: 'POST',
    }),
};

// ----------------------------------------------------
// 2. TRANSACTION API (Quản lý giao dịch thu/chi)
// ----------------------------------------------------
export const transactionApi = {
  getAll: () => request<TransactionResponse[]>('/transactions'),

  create: (data: TransactionRequest) =>
    request<TransactionResponse>('/transactions', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  update: (id: number, data: TransactionRequest) =>
    request<TransactionResponse>(`/transactions/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  delete: (id: number) =>
    request<{ message: string }>(`/transactions/${id}`, {
      method: 'DELETE',
    }),
};

// ----------------------------------------------------
// 3. WALLET API (Quản lý ví/tài khoản)
// ----------------------------------------------------
export const walletApi = {
  getAll: () => request<WalletResponse[]>('/wallets'),

  create: (data: WalletRequest) =>
    request<WalletResponse>('/wallets', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  update: (id: number, data: WalletRequest) =>
    request<WalletResponse>(`/wallets/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  linkBankAccount: (id: number, data: WalletBankAccountRequest) =>
    request<void>(`/wallets/${id}/bank-account`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  unlinkBankAccount: (id: number) =>
    request<void>(`/wallets/${id}/bank-account`, { method: 'DELETE' }),

  delete: (id: number) =>
    request<{ message: string }>(`/wallets/${id}`, {
      method: 'DELETE',
    }),
};

// ----------------------------------------------------
// 4. CATEGORY API (Quản lý danh mục)
// ----------------------------------------------------
export const categoryApi = {
  getAll: () => request<CategoryResponse[]>('/categories'),

  create: (data: CategoryRequest) =>
    request<CategoryResponse>('/categories', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  delete: (name: string) =>
    request<{ message: string }>(`/categories/${encodeURIComponent(name)}`, {
      method: 'DELETE',
    }),
};

export const budgetApi = {
  getAll: (month: string, signal?: AbortSignal) =>
    request<BudgetResponse[]>(`/budgets?month=${encodeURIComponent(month)}`, { signal }),

  create: (data: BudgetRequest) =>
    request<BudgetResponse>('/budgets', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  update: (id: number, data: BudgetRequest) =>
    request<BudgetResponse>(`/budgets/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  delete: (id: number) =>
    request<void>(`/budgets/${id}`, { method: 'DELETE' }),
};

// ----------------------------------------------------
// 5. SAVINGS GOALS API (Mục tiêu tiết kiệm)
// ----------------------------------------------------
export const goalApi = {
  getAll: () => request<GoalResponse[]>('/goals'),

  getOne: (id: number) => request<GoalResponse>(`/goals/${id}`),

  create: (data: GoalRequest) =>
    request<GoalResponse>('/goals', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  update: (id: number, data: GoalRequest) =>
    request<GoalResponse>(`/goals/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  contribute: (id: number, amount: number) =>
    request<GoalResponse>(`/goals/${id}/contribute?amount=${amount}`, {
      method: 'POST',
    }),

  delete: (id: number) =>
    request<{ message: string }>(`/goals/${id}`, {
      method: 'DELETE',
    }),
};

// ----------------------------------------------------
// 6. REPORT API (Báo cáo tháng & năm)
// ----------------------------------------------------
export const reportApi = {
  getMonthly: (year: number, month: number) =>
    request<MonthlyReportResponse>(`/reports/monthly/${year}/${month}`),

  getYearly: (year: number) =>
    request<YearlyReportResponse>(`/reports/yearly/${year}`),
};

// ----------------------------------------------------
// 7. AI ADVISOR API (Cố vấn tài chính AI)
// ----------------------------------------------------
export const aiApi = {
  getMonthlyAdvice: (year: number, month: number) =>
    request<AiAdviceResponse>(`/ai/advice/monthly/${year}/${month}`),

  getCurrentAdvice: () =>
    request<AiAdviceResponse>('/ai/advice/current'),
};
