import React, { useState, useEffect } from 'react';
import {
  Sparkles,
  CheckCircle2,
  AlertTriangle,
  Lightbulb,
  Target,
  Calendar,
  RefreshCw,
  Cpu,
} from 'lucide-react';
import type { AiAdviceResponse } from '../types';
import { aiApi } from '../services/api';

export const AiAdvisorView: React.FC = () => {
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1;

  const [selectedYear, setSelectedYear] = useState<number>(currentYear);
  const [selectedMonth, setSelectedMonth] = useState<number>(currentMonth);
  const [advice, setAdvice] = useState<AiAdviceResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAdvice = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await aiApi.getMonthlyAdvice(selectedYear, selectedMonth);
      setAdvice(data);
    } catch (err: any) {
      setError(err.message || 'Lỗi khi lấy lời khuyên từ AI.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAdvice();
  }, [selectedYear, selectedMonth]);

  const getStatusLabel = (status?: string) => {
    switch (status) {
      case 'EXCELLENT':
        return { text: 'Xuất Sắc', desc: 'Tỷ lệ tiết kiệm vượt chuẩn (>20%)', class: 'status-excellent' };
      case 'GOOD':
        return { text: 'Tốt', desc: 'Duy trì tích lũy ổn định (10% - 20%)', class: 'status-good' };
      case 'WARNING':
        return { text: 'Cảnh Báo', desc: 'Tỷ lệ tiết kiệm thấp (<10%)', class: 'status-warning' };
      case 'DANGER':
        return { text: 'Nguy Hiểm', desc: 'Chi tiêu vượt quá thu nhập (Bội chi)', class: 'status-danger' };
      default:
        return { text: 'Chưa Đủ Dữ Liệu', desc: 'Chưa có đủ giao dịch trong tháng', class: 'status-nodata' };
    }
  };

  const statusInfo = getStatusLabel(advice?.financialHealthStatus);

  return (
    <div className="view-container">
      {/* Header */}
      <div className="view-header">
        <div>
          <div className="flex-center-gap">
            <h2>Cố Vấn Tài Chính AI</h2>
            <span className="badge-ai-pill">
              <Sparkles size={14} /> Smart Intelligence
            </span>
          </div>
          <p>Phân tích chuyên sâu dữ liệu thu chi, cảnh báo thâm hụt và định hướng đạt mục tiêu tiết kiệm</p>
        </div>

        {/* Month Selector */}
        <div className="ai-selector-toolbar">
          <Calendar size={18} />
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
          <button onClick={fetchAdvice} disabled={loading} className="btn-icon" title="Phân tích lại">
            <RefreshCw size={18} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {error && (
        <div className="alert-box alert-error">
          <AlertTriangle size={18} />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="ai-loading-card">
          <Sparkles size={36} className="sparkle-anim" />
          <h3>AI đang phân tích báo cáo tài chính của bạn...</h3>
          <p>Đang tính toán tỷ lệ tiết kiệm, cơ cấu chi tiêu và đo lường tiến độ mục tiêu...</p>
        </div>
      ) : advice ? (
        <div className="ai-content-grid">
          {/* Top Banner: Status + Savings Rate */}
          <div className={`ai-hero-card ${statusInfo.class}`}>
            <div className="ai-hero-left">
              <span className="ai-hero-tag">ĐÁNH GIÁ SỨC KHỎE TÀI CHÍNH</span>
              <h1 className="ai-status-title">{statusInfo.text}</h1>
              <p className="ai-status-desc">{statusInfo.desc}</p>
            </div>

            <div className="ai-hero-metrics">
              <div className="metric-box">
                <span className="metric-label">Tỷ lệ tiết kiệm tháng</span>
                <span className="metric-number">
                  {advice.savingsRate !== undefined ? `${advice.savingsRate.toFixed(1)}%` : '0%'}
                </span>
                <span className="metric-sub">Chuẩn vàng: $\ge$ 20%</span>
              </div>
            </div>
          </div>

          {/* AI Executive Summary */}
          <div className="ai-card summary-card">
            <div className="ai-card-title">
              <Sparkles size={20} className="text-primary" />
              <h3>Tổng Quan Từ AI</h3>
              <span className="source-tag">
                <Cpu size={12} />
                {advice.source === 'GEMINI_AI' ? 'Google Gemini AI' : 'Smart Rule Engine'}
              </span>
            </div>
            <p className="ai-summary-body">{advice.summary}</p>
          </div>

          {/* Two Columns: Insights & Recommendations */}
          <div className="ai-dual-columns">
            {/* Insights */}
            <div className="ai-card">
              <div className="ai-card-title">
                <Lightbulb size={20} className="text-warning" />
                <h3>Điểm Đáng Chú Ý (Insights)</h3>
              </div>
              {advice.insights.length === 0 ? (
                <p className="text-muted">Chưa ghi nhận biến động chi tiêu nổi bật nào.</p>
              ) : (
                <ul className="ai-bullet-list">
                  {advice.insights.map((item, idx) => (
                    <li key={idx} className="ai-bullet-item insight-item">
                      <span className="bullet-dot dot-insight"></span>
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {/* Recommendations */}
            <div className="ai-card">
              <div className="ai-card-title">
                <CheckCircle2 size={20} className="text-success" />
                <h3>Lời Khuyên Hành Động</h3>
              </div>
              {advice.recommendations.length === 0 ? (
                <p className="text-muted">Chưa có khuyến nghị mới.</p>
              ) : (
                <ul className="ai-bullet-list">
                  {advice.recommendations.map((item, idx) => (
                    <li key={idx} className="ai-bullet-item rec-item">
                      <span className="bullet-dot dot-rec"></span>
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          {/* Goal Assessments */}
          <div className="ai-card">
            <div className="ai-card-title">
              <Target size={20} className="text-indigo" />
              <h3>Đánh Giá Khả Năng Đạt Mục Tiêu Tiết Kiệm</h3>
            </div>
            {advice.goalAssessments.length === 0 ? (
              <p className="text-muted">Chưa có mục tiêu nào được thiết lập.</p>
            ) : (
              <div className="goal-assessments-list">
                {advice.goalAssessments.map((assessment, idx) => (
                  <div key={idx} className="goal-assessment-item">
                    <Target size={18} className="text-indigo-icon" />
                    <span>{assessment}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      ) : null}
    </div>
  );
};
