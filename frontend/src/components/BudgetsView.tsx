import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { AlertCircle, ChartPie, Edit2, Plus, Trash2, X } from 'lucide-react';
import { ApiError, budgetApi, transactionApi } from '../services/api';
import type { BudgetResponse, CategoryResponse } from '../types';

const currency = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});
const percentage = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 });
const statusLabels: Record<BudgetResponse['status'], string> = {
  SAFE: 'Trong ngân sách',
  NEAR_LIMIT: 'Gần hạn mức',
  AT_LIMIT: 'Đã chạm hạn mức',
  EXCEEDED: 'Vượt ngân sách',
};

export function BudgetsView({ categories, onSessionExpired }: { categories: CategoryResponse[]; onSessionExpired: () => void }) {
  const [month, setMonth] = useState(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });

  return (
    <section className="view-container">
      <div className="view-header">
        <div>
          <h2>Ngân sách</h2>
          <p>Đặt hạn mức và theo dõi chi tiêu theo danh mục.</p>
        </div>
        <div className="form-group budget-month">
          <label htmlFor="budget-month">Tháng</label>
          <input
            id="budget-month"
            type="month"
            min="0001-01"
            max="9999-12"
            value={month}
            onChange={(event) => {
              if (/^(?!0000)[0-9]{4}-(0[1-9]|1[0-2])$/.test(event.target.value)) {
                setMonth(event.target.value);
              }
            }}
          />
        </div>
      </div>
      <MonthlyBudgets key={month} month={month} categories={categories.filter((category) => category.type === 'EXPENSE')} onSessionExpired={onSessionExpired} />
    </section>
  );
}

function MonthlyBudgets({ month, categories, onSessionExpired }: { month: string; categories: CategoryResponse[]; onSessionExpired: () => void }) {
  const [budgets, setBudgets] = useState<BudgetResponse[] | null>(null);
  const [error, setError] = useState('');
  const [loadAttempt, setLoadAttempt] = useState(0);
  const [editor, setEditor] = useState<{ budget?: BudgetResponse } | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    budgetApi.getAll(month, controller.signal).then(setBudgets).catch(async (err: Error) => {
      if (controller.signal.aborted) return;
      if (err instanceof ApiError && err.status === 403) {
        try {
          await transactionApi.getAll();
          if (!controller.signal.aborted) setError('Backend đang chạy phiên bản cũ. Hãy dừng backend, chạy lại file JAR mới rồi tải lại trang.');
        } catch (sessionError) {
          if (!controller.signal.aborted && sessionError instanceof ApiError && (sessionError.status === 401 || sessionError.status === 403)) {
            onSessionExpired();
          } else if (!controller.signal.aborted) {
            setError(err.message);
          }
        }
      } else {
        setError(err.message);
      }
    });
    return () => controller.abort();
  }, [month, loadAttempt, onSessionExpired]);

  const removeBudget = async (budget: BudgetResponse) => {
    if (!window.confirm(`Xóa ngân sách “${budget.categoryName}” của tháng ${month}?`)) return;
    setDeletingId(budget.id);
    setError('');
    try {
      await budgetApi.delete(budget.id);
      setBudgets((current) => current?.filter((item) => item.id !== budget.id) ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể xóa ngân sách.');
    } finally {
      setDeletingId(null);
    }
  };

  const availableCategories = categories.filter((category) =>
    !budgets?.some((budget) => budget.categoryId === category.id && budget.id !== editor?.budget?.id));
  const canAdd = budgets !== null && categories.some((category) =>
    !budgets.some((budget) => budget.categoryId === category.id));

  return (
    <>
      {error && (
        <div className="alert-box alert-error" role="alert">
          <AlertCircle size={18} />
          <span>{error}</span>
          {budgets === null && (
            <button type="button" className="btn-link" onClick={() => {
              setError('');
              setLoadAttempt((attempt) => attempt + 1);
            }}>Thử lại</button>
          )}
        </div>
      )}

      {budgets === null ? (
        !error && <p className="text-muted" role="status">Đang tải ngân sách…</p>
      ) : (
        <>
          <div className="budget-actions">
            <button type="button" className="btn-primary" onClick={() => setEditor({})} disabled={!canAdd}>
              <Plus size={18} /> Thêm ngân sách
            </button>
            {!canAdd && (
              <span className="text-muted">
                {categories.length === 0 ? 'Hãy tạo danh mục chi trước.' : 'Tất cả danh mục chi đã có ngân sách trong tháng.'}
              </span>
            )}
          </div>
          {budgets.length === 0 ? (
            <div className="empty-state">
              <ChartPie size={32} aria-hidden="true" />
              <p>Tháng này chưa có ngân sách.</p>
            </div>
          ) : (
            <div className="budget-grid">
              {budgets.map((budget) => (
                <article className="budget-card" key={budget.id}>
                  <div className="budget-card-header">
                    <h3>{budget.categoryName}</h3>
                    <div className="action-buttons">
                      <button type="button" className="btn-icon" aria-label={`Sửa ngân sách ${budget.categoryName}`}
                        onClick={() => setEditor({ budget })} disabled={deletingId === budget.id}>
                        <Edit2 size={16} />
                      </button>
                      <button type="button" className="btn-icon text-danger" aria-label={`Xóa ngân sách ${budget.categoryName}`}
                        onClick={() => void removeBudget(budget)} disabled={deletingId !== null}>
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </div>
                  <div className="budget-card-summary">
                    <span>Đã chi <strong>{currency.format(budget.spentAmount)}</strong> / {currency.format(budget.limitAmount)}</span>
                    <strong>{percentage.format(budget.usagePercentage)}%</strong>
                  </div>
                  <div className="progress-bar-container" role="progressbar" aria-label={`Ngân sách ${budget.categoryName}`}
                    aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.min(budget.usagePercentage, 100)}
                    aria-valuetext={`${percentage.format(budget.usagePercentage)}% hạn mức`}>
                    <div className={`progress-bar-fill budget-fill ${budget.status}`}
                      style={{ width: `${Math.min(budget.usagePercentage, 100)}%` }} />
                  </div>
                  <div className="budget-card-summary">
                    <span className={`budget-status ${budget.status}`}>{statusLabels[budget.status]}</span>
                    <span>
                      {budget.remainingAmount < 0 ? 'Vượt ' : 'Còn '}
                      <strong>{currency.format(Math.abs(budget.remainingAmount))}</strong>
                    </span>
                  </div>
                </article>
              ))}
            </div>
          )}
        </>
      )}

      {editor && (
        <BudgetEditor
          month={month}
          budget={editor.budget}
          categories={availableCategories}
          onClose={() => setEditor(null)}
          onSaved={(saved) => {
            setBudgets((current) => [...(current ?? []).filter((budget) => budget.id !== saved.id), saved]
              .sort((a, b) => a.categoryName.localeCompare(b.categoryName, 'vi')));
            setEditor(null);
            setError('');
          }}
        />
      )}
    </>
  );
}

interface BudgetEditorProps {
  month: string;
  budget?: BudgetResponse;
  categories: CategoryResponse[];
  onClose: () => void;
  onSaved: (budget: BudgetResponse) => void;
}

function BudgetEditor({ month, budget, categories, onClose, onSaved }: BudgetEditorProps) {
  const [categoryId, setCategoryId] = useState(budget ? String(budget.categoryId) : '');
  const [limit, setLimit] = useState(budget ? String(budget.limitAmount) : '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    if (!/^[0-9]{1,17}(\.[0-9]{1,2})?$/.test(limit) || Number(limit) <= 0) {
      setError('Hạn mức phải lớn hơn 0 và có tối đa 2 chữ số thập phân.');
      return;
    }

    setSaving(true);
    try {
      const data = { categoryId: Number(categoryId), month, limitAmount: limit };
      onSaved(budget ? await budgetApi.update(budget.id, data) : await budgetApi.create(data));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lưu ngân sách.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-card" role="dialog" aria-modal="true" aria-labelledby="budget-dialog-title">
        <button type="button" className="modal-close-btn" aria-label="Đóng" onClick={onClose} disabled={saving}>
          <X size={20} />
        </button>
        <div className="modal-header">
          <h2 id="budget-dialog-title">{budget ? 'Sửa ngân sách' : 'Thêm ngân sách'}</h2>
          <p className="modal-subtitle">Tháng {month.slice(5)}/{month.slice(0, 4)}</p>
        </div>
        {error && <div className="alert-box alert-error" role="alert">{error}</div>}
        <form onSubmit={save} className="auth-form">
          <div className="form-group">
            <label htmlFor="budget-category">Danh mục chi</label>
            <select id="budget-category" value={categoryId}
              onChange={(event) => setCategoryId(event.target.value)} disabled={saving} required>
              <option value="" disabled>Chọn danh mục</option>
              {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="budget-limit">Hạn mức (VNĐ)</label>
            <input id="budget-limit" type="number" min="0.01" step="0.01" value={limit}
              onChange={(event) => setLimit(event.target.value)} placeholder="1000000" disabled={saving} required />
          </div>
          <button type="submit" className="btn-submit" disabled={saving}>
            {saving ? 'Đang lưu…' : 'Lưu ngân sách'}
          </button>
        </form>
      </div>
    </div>
  );
}
