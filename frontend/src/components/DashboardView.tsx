import React from 'react';
import {
  TrendingUp,
  TrendingDown,
  PiggyBank,
  Percent,
  Sparkles,
  ArrowRight,
  PlusCircle,
  Calendar,
  AlertCircle,
  Wallet as WalletIcon,
} from 'lucide-react';
import type { TransactionResponse, GoalResponse, AiAdviceResponse, WalletResponse } from '../types';

interface DashboardViewProps {
  transactions: TransactionResponse[];
  goals: GoalResponse[];
  wallets: WalletResponse[];
  aiAdvice: AiAdviceResponse | null;
  onNavigate: (tab: string) => void;
  onOpenAddTransaction: () => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  transactions,
  goals,
  wallets,
  aiAdvice,
  onNavigate,
  onOpenAddTransaction,
}) => {
  // Tính tổng thu nhập và chi tiêu từ các giao dịch hiện có
  const totalIncome = transactions
    .filter((t) => t.type === 'INCOME')
    .reduce((sum, t) => sum + Number(t.amount), 0);

  const totalExpense = transactions
    .filter((t) => t.type === 'EXPENSE')
    .reduce((sum, t) => sum + Number(t.amount), 0);

  const netSavings = totalIncome - totalExpense;
  const savingsRate = totalIncome > 0 ? (netSavings / totalIncome) * 100 : 0;

  // Lấy 5 giao dịch gần nhất
  const recentTransactions = [...transactions]
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
    .slice(0, 5);

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  return (
    <div className="view-container">
      {/* Welcome Banner */}
      <div className="welcome-banner">
        <div>
          <h2>Bảng Điều Khiển Tài Chính</h2>
          <p>Xem tổng quan tình hình thu chi, mục tiêu tiết kiệm và phân tích từ trợ lý AI</p>
        </div>
        <button onClick={onOpenAddTransaction} className="btn-primary">
          <PlusCircle size={18} />
          <span>Thêm Giao Dịch Mới</span>
        </button>
      </div>

      {/* KPI Cards */}
      <div className="kpi-grid">
        <div className="kpi-card income">
          <div className="kpi-header">
            <span>Tổng Thu Nhập</span>
            <div className="kpi-icon"><TrendingUp size={20} /></div>
          </div>
          <div className="kpi-value">{formatCurrency(totalIncome)}</div>
          <span className="kpi-subtext">Từ tất cả các nguồn lương, thưởng,...</span>
        </div>

        <div className="kpi-card expense">
          <div className="kpi-header">
            <span>Tổng Chi Tiêu</span>
            <div className="kpi-icon"><TrendingDown size={20} /></div>
          </div>
          <div className="kpi-value">{formatCurrency(totalExpense)}</div>
          <span className="kpi-subtext">Sinh hoạt, ăn uống, hóa đơn,...</span>
        </div>

        <div className="kpi-card savings">
          <div className="kpi-header">
            <span>Tiết Kiệm Thuần</span>
            <div className="kpi-icon"><PiggyBank size={20} /></div>
          </div>
          <div className={`kpi-value ${netSavings < 0 ? 'text-danger' : ''}`}>
            {formatCurrency(netSavings)}
          </div>
          <span className="kpi-subtext">Thu nhập trừ tổng chi tiêu</span>
        </div>

        <div className="kpi-card rate">
          <div className="kpi-header">
            <span>Tỷ Lệ Tiết Kiệm</span>
            <div className="kpi-icon"><Percent size={20} /></div>
          </div>
          <div className="kpi-value">{savingsRate.toFixed(1)}%</div>
          <span className="kpi-subtext">
            {savingsRate >= 20 ? '✨ Đạt chuẩn vàng (>20%)' : 'Cần tối ưu thêm'}
          </span>
        </div>
      </div>

      {/* Wallet Overview */}
      {wallets.length > 0 && (
        <div className="dashboard-card wallet-overview-card">
          <div className="card-header">
            <div>
              <h3>Số dư các ví</h3>
              <p className="card-subtitle">Theo dõi nhanh số dư hiện tại của từng ví</p>
            </div>
            <button onClick={() => onNavigate('wallets')} className="btn-link">
              Quản lý ví ({wallets.length})
            </button>
          </div>
          <div className="wallet-overview-list">
            {wallets.slice(0, 4).map((wallet) => (
              <div key={wallet.id} className="wallet-overview-item">
                <div className="wallet-overview-name">
                  <WalletIcon size={17} />
                  <span>{wallet.name}</span>
                </div>
                <strong className={wallet.currentBalance < 0 ? 'text-danger' : ''}>
                  {formatCurrency(Number(wallet.currentBalance))}
                </strong>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* AI Advice Summary Banner */}
      {aiAdvice && (
        <div className={`ai-summary-banner status-${aiAdvice.financialHealthStatus.toLowerCase()}`}>
          <div className="ai-summary-header">
            <div className="ai-summary-title">
              <Sparkles size={20} className="sparkle-icon" />
              <strong>Đánh giá nhanh từ Trợ Lý AI: </strong>
              <span className={`status-pill ${aiAdvice.financialHealthStatus}`}>
                {aiAdvice.financialHealthStatus}
              </span>
            </div>
            <button onClick={() => onNavigate('ai')} className="btn-text">
              <span>Xem chi tiết lời khuyên</span>
              <ArrowRight size={16} />
            </button>
          </div>
          <p className="ai-summary-text">{aiAdvice.summary}</p>
        </div>
      )}

      {/* Two Column Layout: Recent Transactions & Active Goals */}
      <div className="dashboard-columns">
        {/* Recent Transactions */}
        <div className="dashboard-card">
          <div className="card-header">
            <h3>Giao dịch gần đây</h3>
            <button onClick={() => onNavigate('transactions')} className="btn-link">
              Xem tất cả ({transactions.length})
            </button>
          </div>

          {recentTransactions.length === 0 ? (
            <div className="empty-state">
              <AlertCircle size={32} />
              <p>Chưa có giao dịch nào được ghi nhận.</p>
              <button onClick={onOpenAddTransaction} className="btn-secondary">
                Thêm giao dịch đầu tiên
              </button>
            </div>
          ) : (
            <div className="recent-list">
              {recentTransactions.map((tx) => (
                <div key={tx.id} className="recent-item">
                  <div className="recent-item-info">
                    <span className="tx-category">{tx.category}</span>
                    <span className="tx-desc">{tx.description || 'Không có mô tả'}</span>
                    <span className="tx-date">
                      <Calendar size={12} />
                      {tx.date}
                    </span>
                  </div>
                  <div className={`tx-amount ${tx.type === 'INCOME' ? 'text-income' : 'text-expense'}`}>
                    {tx.type === 'INCOME' ? '+' : '-'} {formatCurrency(tx.amount)}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Active Goals */}
        <div className="dashboard-card">
          <div className="card-header">
            <h3>Mục tiêu tiết kiệm</h3>
            <button onClick={() => onNavigate('goals')} className="btn-link">
              Quản lý mục tiêu ({goals.length})
            </button>
          </div>

          {goals.length === 0 ? (
            <div className="empty-state">
              <PiggyBank size={32} />
              <p>Chưa có mục tiêu tiết kiệm nào được thiết lập.</p>
              <button onClick={() => onNavigate('goals')} className="btn-secondary">
                Tạo mục tiêu mới
              </button>
            </div>
          ) : (
            <div className="goals-mini-list">
              {goals.slice(0, 4).map((goal) => {
                const percent = Math.min(100, Math.max(0, goal.progressPercentage || 0));
                return (
                  <div key={goal.id} className="goal-mini-item">
                    <div className="goal-mini-header">
                      <strong>{goal.goalName}</strong>
                      <span className="goal-percent">{percent.toFixed(1)}%</span>
                    </div>
                    <div className="progress-bar-container">
                      <div className="progress-bar-fill" style={{ width: `${percent}%` }}></div>
                    </div>
                    <div className="goal-mini-footer">
                      <span>Đã có: {formatCurrency(goal.currentProgress)}</span>
                      <span>Mục tiêu: {formatCurrency(goal.targetAmount)}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
