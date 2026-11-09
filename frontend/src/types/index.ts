export interface User {
  username: string;
  fullName?: string;
  phoneNumber?: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
  type: 'INCOME' | 'EXPENSE';
  isCustom: boolean;
}

export interface CategoryRequest {
  name: string;
  type: 'INCOME' | 'EXPENSE';
}

export interface TransactionResponse {
  id: number;
  amount: number;
  date: string;
  category: string;
  description: string;
  type: 'INCOME' | 'EXPENSE';
}

export interface TransactionRequest {
  amount: number;
  date: string;
  category: string;
  description?: string;
}

export interface GoalResponse {
  id: number;
  goalName: string;
  targetAmount: number;
  targetDate: string;
  startDate: string;
  currentProgress: number;
  progressPercentage: number;
  remainingAmount: number;
}

export interface GoalRequest {
  goalName: string;
  targetAmount: number;
  targetDate: string;
  startDate?: string;
  currentAmount?: number;
}

export interface MonthlyReportResponse {
  month: number;
  year: number;
  totalIncome: Record<string, number>;
  totalExpenses: Record<string, number>;
  netSavings: number;
}

export interface YearlyReportResponse {
  year: number;
  totalIncome: Record<string, number>;
  totalExpenses: Record<string, number>;
  netSavings: number;
}

export interface AiAdviceResponse {
  month: number;
  year: number;
  financialHealthStatus: 'EXCELLENT' | 'GOOD' | 'WARNING' | 'DANGER' | 'NO_DATA' | string;
  savingsRate: number;
  summary: string;
  insights: string[];
  recommendations: string[];
  goalAssessments: string[];
  source: string;
}
