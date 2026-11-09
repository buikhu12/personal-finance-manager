import React, { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, PiggyBank, Calendar, AlertCircle } from 'lucide-react';
import type { MonthlyReportResponse, YearlyReportResponse } from '../types';
import { reportApi } from '../services/api';

export const ReportsView: React.FC = () => {
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1;

  const [mode, setMode] = useState<'MONTHLY' | 'YEARLY'>('MONTHLY');
  const [selectedYear, setSelectedYear] = useState<number>(currentYear);
  const [selectedMonth, setSelectedMonth] = useState<number>(currentMonth);

  const [monthlyData, setMonthlyData] = useState<MonthlyReportResponse | null>(null);
  const [yearlyData, setYearlyData] = useState<YearlyReportResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  const fetchReport = async () => {
    setLoading(true);
    setError(null);
    try {
      if (mode === 'MONTHLY') {
        const data = await reportApi.getMonthly(selectedYear, selectedMonth);
        setMonthlyData(data);
      } else {
        const data = await reportApi.getYearly(selectedYear);
        setYearlyData(data);
      }
    } catch (err: any) {
      setError(err.message || 'Lỗi khi tải báo cáo tài chính.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, [mode, selectedYear, selectedMonth]);

  const activeReport = mode === 'MONTHLY' ? monthlyData : yearlyData;

  const totalIncome = activeReport
    ? Object.values(activeReport.totalIncome).reduce((sum, v) => sum + Number(v), 0)
    : 0;

  const totalExpense = activeReport
    ? Object.values(activeReport.totalExpenses).reduce((sum, v) => sum + Number(v), 0)
    : 0;

  const netSavings = activeReport ? Number(activeReport.netSavings) : 0;

  return (
    <div className="view-container">
      {/* Header */}
      <div className="view-header">
        <div>
          <h2>Báo Cáo Thu Chi & Tiết Kiệm</h2>
          <p>Phân tích tổng hợp số liệu thu chi theo từng danh mục theo tháng hoặc năm</p>
        </div>

        {/* Mode Toggle */}
        <div className="mode-toggle">
          <button
            className={`toggle-btn ${mode === 'MONTHLY' ? 'active' : ''}`}
            onClick={() => setMode('MONTHLY')}
          >
            Báo cáo Tháng
          </button>
          <button
            className={`toggle-btn ${mode === 'YEARLY' ? 'active' : ''}`}
            onClick={() => setMode('YEARLY')}
          >
            Báo cáo Năm
          </button>
        </div>
      </div>

      {/* Selector Toolbar */}
      <div className="report-toolbar">
        <div className="selector-group">
          <Calendar size={18} />
          {mode === 'MONTHLY' && (
            <select
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(Number(e.target.value))}
            >
              {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                <option key={m} value={m}>
                  Tháng {m}
                </option>
              ))}
            </select>
          )}

          <select
            value={selectedYear}
            onChange={(e) => setSelectedYear(Number(e.target.value))}
          >
            {[currentYear + 1, currentYear, currentYear - 1, currentYear - 2].map((y) => (
              <option key={y} value={y}>
                Năm {y}
              </option>
            ))}
          </select>
        </div>

        <button onClick={fetchReport} className="btn-secondary">
          Làm mới báo cáo
        </button>
      </div>

      {error && (
        <div className="alert-box alert-error">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="loading-state">Đang tải và tổng hợp dữ liệu báo cáo...</div>
      ) : activeReport ? (
        <>
          {/* Summary KPI Cards */}
          <div className="kpi-grid">
            <div className="kpi-card income">
              <div className="kpi-header">
                <span>Tổng Thu Nhập</span>
                <div className="kpi-icon"><TrendingUp size={20} /></div>
              </div>
              <div className="kpi-value">{formatCurrency(totalIncome)}</div>
            </div>

            <div className="kpi-card expense">
              <div className="kpi-header">
                <span>Tổng Chi Tiêu</span>
                <div className="kpi-icon"><TrendingDown size={20} /></div>
              </div>
              <div className="kpi-value">{formatCurrency(totalExpense)}</div>
            </div>

            <div className="kpi-card savings">
              <div className="kpi-header">
                <span>Tiết Kiệm Ròng</span>
                <div className="kpi-icon"><PiggyBank size={20} /></div>
              </div>
              <div className={`kpi-value ${netSavings < 0 ? 'text-danger' : ''}`}>
                {formatCurrency(netSavings)}
              </div>
            </div>
          </div>

          {/* Breakdown Tables: Income vs Expense */}
          <div className="breakdown-grid">
            {/* Thu nhập theo danh mục */}
            <div className="dashboard-card">
              <div className="card-header">
                <h3>Cơ cấu Thu nhập</h3>
                <span className="badge-pill badge-income">Thu nhập</span>
              </div>
              {Object.keys(activeReport.totalIncome).length === 0 ? (
                <p className="text-muted padding-md">Không có khoản thu nào trong kỳ này.</p>
              ) : (
                <div className="breakdown-list">
                  {Object.entries(activeReport.totalIncome).map(([cat, val]) => {
                    const amount = Number(val);
                    const pct = totalIncome > 0 ? (amount / totalIncome) * 100 : 0;
                    return (
                      <div key={cat} className="breakdown-item">
                        <div className="breakdown-item-header">
                          <strong>{cat}</strong>
                          <span className="text-income font-bold">{formatCurrency(amount)} ({pct.toFixed(1)}%)</span>
                        </div>
                        <div className="progress-bar-container">
                          <div className="progress-bar-fill bg-income" style={{ width: `${pct}%` }}></div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Chi tiêu theo danh mục */}
            <div className="dashboard-card">
              <div className="card-header">
                <h3>Cơ cấu Chi tiêu</h3>
                <span className="badge-pill badge-expense">Chi tiêu</span>
              </div>
              {Object.keys(activeReport.totalExpenses).length === 0 ? (
                <p className="text-muted padding-md">Không có khoản chi nào trong kỳ này.</p>
              ) : (
                <div className="breakdown-list">
                  {Object.entries(activeReport.totalExpenses).map(([cat, val]) => {
                    const amount = Number(val);
                    const pct = totalExpense > 0 ? (amount / totalExpense) * 100 : 0;
                    return (
                      <div key={cat} className="breakdown-item">
                        <div className="breakdown-item-header">
                          <strong>{cat}</strong>
                          <span className="text-expense font-bold">{formatCurrency(amount)} ({pct.toFixed(1)}%)</span>
                        </div>
                        <div className="progress-bar-container">
                          <div className="progress-bar-fill bg-expense" style={{ width: `${pct}%` }}></div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
};
