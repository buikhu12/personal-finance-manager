import React, { useState } from 'react';
import {
  Target,
  Plus,
  Trash2,
  Calendar,
  AlertCircle,
  X,
  Coins,
} from 'lucide-react';
import type { GoalResponse, GoalRequest } from '../types';
import { goalApi } from '../services/api';

interface GoalsViewProps {
  goals: GoalResponse[];
  onRefresh: () => void;
}

export const GoalsView: React.FC<GoalsViewProps> = ({ goals, onRefresh }) => {
  // Modal tạo mục tiêu
  const [isOpenCreateModal, setIsOpenCreateModal] = useState(false);
  const [goalName, setGoalName] = useState('');
  const [targetAmount, setTargetAmount] = useState('');
  const [targetDate, setTargetDate] = useState('');
  const [initialAmount, setInitialAmount] = useState('');
  const [createError, setCreateError] = useState<string | null>(null);
  const [createLoading, setCreateLoading] = useState(false);

  // Modal nạp tiền vào mục tiêu
  const [contributeGoal, setContributeGoal] = useState<GoalResponse | null>(null);
  const [depositAmount, setDepositAmount] = useState('');
  const [depositError, setDepositError] = useState<string | null>(null);
  const [depositLoading, setDepositLoading] = useState(false);

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateError(null);

    const amountNum = parseFloat(targetAmount);
    if (isNaN(amountNum) || amountNum <= 0) {
      setCreateError('Số tiền mục tiêu phải là số dương lớn hơn 0.');
      return;
    }

    // Kiểm tra ngày mục tiêu phải ở tương lai
    const target = new Date(targetDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (target <= today) {
      setCreateError('Ngày kết thúc mục tiêu phải ở trong tương lai.');
      return;
    }

    const payload: GoalRequest = {
      goalName: goalName.trim(),
      targetAmount: amountNum,
      targetDate,
      startDate: new Date().toISOString().split('T')[0],
      currentAmount: initialAmount ? parseFloat(initialAmount) : 0,
    };

    setCreateLoading(true);
    try {
      await goalApi.create(payload);
      setIsOpenCreateModal(false);
      setGoalName('');
      setTargetAmount('');
      setTargetDate('');
      setInitialAmount('');
      onRefresh();
    } catch (err: any) {
      setCreateError(err.message || 'Lỗi khi tạo mục tiêu.');
    } finally {
      setCreateLoading(false);
    }
  };

  const handleContribute = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!contributeGoal) return;
    setDepositError(null);

    const amountNum = parseFloat(depositAmount);
    if (isNaN(amountNum) || amountNum <= 0) {
      setDepositError('Số tiền nạp thêm phải là số dương lớn hơn 0.');
      return;
    }

    setDepositLoading(true);
    try {
      await goalApi.contribute(contributeGoal.id, amountNum);
      setContributeGoal(null);
      setDepositAmount('');
      onRefresh();
    } catch (err: any) {
      setDepositError(err.message || 'Lỗi khi nạp tiền vào mục tiêu.');
    } finally {
      setDepositLoading(false);
    }
  };

  const handleDelete = async (goal: GoalResponse) => {
    if (!window.confirm(`Bạn có chắc muốn xóa mục tiêu "${goal.goalName}"?`)) return;
    try {
      await goalApi.delete(goal.id);
      onRefresh();
    } catch (err: any) {
      alert(err.message || 'Lỗi khi xóa mục tiêu.');
    }
  };

  return (
    <div className="view-container">
      {/* Header */}
      <div className="view-header">
        <div>
          <h2>Mục Tiêu Tiết Kiệm</h2>
          <p>Lập kế hoạch và theo dõi các khoản tích lũy cho những ước mơ tài chính của bạn</p>
        </div>
        <button onClick={() => setIsOpenCreateModal(true)} className="btn-primary">
          <Plus size={18} />
          <span>Tạo Mục Tiêu Mới</span>
        </button>
      </div>

      {/* Goals Grid */}
      {goals.length === 0 ? (
        <div className="empty-state">
          <Target size={40} />
          <h3>Chưa có mục tiêu tiết kiệm nào</h3>
          <p>Tạo mục tiêu để theo dõi khoản tiền cần tích lũy và nhận lời khuyên khả thi từ AI.</p>
          <button onClick={() => setIsOpenCreateModal(true)} className="btn-secondary">
            Tạo mục tiêu đầu tiên
          </button>
        </div>
      ) : (
        <div className="goals-grid">
          {goals.map((goal) => {
            const percent = Math.min(100, Math.max(0, goal.progressPercentage || 0));
            const isCompleted = percent >= 100 || Number(goal.remainingAmount) <= 0;

            return (
              <div key={goal.id} className={`goal-card ${isCompleted ? 'goal-completed' : ''}`}>
                <div className="goal-card-header">
                  <div className="goal-title-group">
                    <div className="goal-icon-badge">
                      <Target size={20} />
                    </div>
                    <div>
                      <h3>{goal.goalName}</h3>
                      <span className="goal-date-label">
                        <Calendar size={13} />
                        Hạn: {goal.targetDate}
                      </span>
                    </div>
                  </div>
                  <button
                    onClick={() => handleDelete(goal)}
                    className="btn-icon text-danger"
                    title="Xóa mục tiêu"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>

                {/* Progress bar */}
                <div className="goal-progress-section">
                  <div className="progress-labels">
                    <span className="progress-text">Tiến độ hoàn thành</span>
                    <strong className="progress-percentage">{percent.toFixed(1)}%</strong>
                  </div>
                  <div className="progress-bar-container large">
                    <div
                      className={`progress-bar-fill ${isCompleted ? 'bg-success' : ''}`}
                      style={{ width: `${percent}%` }}
                    ></div>
                  </div>
                </div>

                {/* Amount info */}
                <div className="goal-details-grid">
                  <div className="detail-item">
                    <span className="detail-label">Đã tích lũy</span>
                    <span className="detail-value text-income">{formatCurrency(goal.currentProgress)}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Còn thiếu</span>
                    <span className="detail-value text-muted">
                      {isCompleted ? 'Đã hoàn thành 🎉' : formatCurrency(goal.remainingAmount)}
                    </span>
                  </div>
                  <div className="detail-item full-width">
                    <span className="detail-label">Mục tiêu đích</span>
                    <span className="detail-value font-bold">{formatCurrency(goal.targetAmount)}</span>
                  </div>
                </div>

                {/* Action button: Nạp tiền */}
                {!isCompleted && (
                  <button
                    onClick={() => {
                      setContributeGoal(goal);
                      setDepositAmount('');
                      setDepositError(null);
                    }}
                    className="btn-contribute"
                  >
                    <Coins size={16} />
                    <span>Nạp Tiền Vào Mục Tiêu Này</span>
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Modal Tạo Mục Tiêu */}
      {isOpenCreateModal && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <button onClick={() => setIsOpenCreateModal(false)} className="modal-close-btn">
              <X size={20} />
            </button>

            <div className="modal-header">
              <h2>Tạo Mục Tiêu Tiết Kiệm Mới</h2>
              <p className="modal-subtitle">Đặt số tiền cần tích lũy và thời hạn hoàn thành</p>
            </div>

            {createError && (
              <div className="alert-box alert-error">
                <AlertCircle size={18} />
                <span>{createError}</span>
              </div>
            )}

            <form onSubmit={handleCreate} className="auth-form">
              <div className="form-group">
                <label>Tên mục tiêu</label>
                <input
                  type="text"
                  required
                  value={goalName}
                  onChange={(e) => setGoalName(e.target.value)}
                  placeholder="Ví dụ: Quỹ khẩn cấp, Mua xe, Đi du lịch Nhật,..."
                />
              </div>

              <div className="form-group">
                <label>Số tiền mục tiêu (VNĐ)</label>
                <input
                  type="number"
                  step="any"
                  required
                  value={targetAmount}
                  onChange={(e) => setTargetAmount(e.target.value)}
                  placeholder="Ví dụ: 20000000"
                />
              </div>

              <div className="form-group">
                <label>Số tiền đã có sẵn (Tùy chọn)</label>
                <input
                  type="number"
                  step="any"
                  value={initialAmount}
                  onChange={(e) => setInitialAmount(e.target.value)}
                  placeholder="Ví dụ: 5000000"
                />
              </div>

              <div className="form-group">
                <label>Ngày kết thúc mục tiêu (Hạn chót)</label>
                <input
                  type="date"
                  required
                  min={new Date().toISOString().split('T')[0]}
                  value={targetDate}
                  onChange={(e) => setTargetDate(e.target.value)}
                />
              </div>

              <button type="submit" disabled={createLoading} className="btn-submit">
                {createLoading ? 'Đang tạo...' : 'Tạo Mục Tiêu'}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Modal Nạp Tiền Vào Mục Tiêu */}
      {contributeGoal && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <button onClick={() => setContributeGoal(null)} className="modal-close-btn">
              <X size={20} />
            </button>

            <div className="modal-header">
              <h2>Nạp Tiền Vào Mục Tiêu</h2>
              <p className="modal-subtitle">
                Mục tiêu: <strong>{contributeGoal.goalName}</strong>
              </p>
            </div>

            {depositError && (
              <div className="alert-box alert-error">
                <AlertCircle size={18} />
                <span>{depositError}</span>
              </div>
            )}

            <form onSubmit={handleContribute} className="auth-form">
              <div className="form-group">
                <label>Số tiền muốn nạp thêm (VNĐ)</label>
                <input
                  type="number"
                  step="any"
                  required
                  autoFocus
                  value={depositAmount}
                  onChange={(e) => setDepositAmount(e.target.value)}
                  placeholder="Ví dụ: 1000000"
                />
              </div>

              <div className="contribute-summary-info">
                <span>Số tiền hiện có: {formatCurrency(contributeGoal.currentProgress)}</span>
                <span>Còn thiếu: {formatCurrency(contributeGoal.remainingAmount)}</span>
              </div>

              <button type="submit" disabled={depositLoading} className="btn-submit">
                {depositLoading ? 'Đang xử lý...' : 'Xác Nhận Nạp Tiền'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
