import React, { useState } from 'react';
import { Plus, Trash2, Lock, Tag, AlertCircle, X } from 'lucide-react';
import type { CategoryResponse, CategoryRequest } from '../types';
import { categoryApi } from '../services/api';

interface CategoriesViewProps {
  categories: CategoryResponse[];
  onRefresh: () => void;
}

export const CategoriesView: React.FC<CategoriesViewProps> = ({ categories, onRefresh }) => {
  const [isOpenModal, setIsOpenModal] = useState(false);
  const [name, setName] = useState('');
  const [type, setType] = useState<'INCOME' | 'EXPENSE'>('EXPENSE');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const incomeCategories = categories.filter((c) => c.type === 'INCOME');
  const expenseCategories = categories.filter((c) => c.type === 'EXPENSE');

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!name.trim()) {
      setError('Vui lòng nhập tên danh mục.');
      return;
    }

    setLoading(true);
    try {
      const payload: CategoryRequest = { name: name.trim(), type };
      await categoryApi.create(payload);
      setIsOpenModal(false);
      setName('');
      onRefresh();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi tạo danh mục.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (cat: CategoryResponse) => {
    if (!cat.isCustom) {
      alert('Không thể xóa danh mục mặc định của hệ thống.');
      return;
    }

    if (!window.confirm(`Bạn có chắc chắn muốn xóa danh mục tùy chỉnh "${cat.name}"?`)) {
      return;
    }

    try {
      await categoryApi.delete(cat.name);
      onRefresh();
    } catch (err: any) {
      alert(err.message || 'Không thể xóa danh mục này (có thể đang được gắn với giao dịch).');
    }
  };

  return (
    <div className="view-container">
      {/* Header */}
      <div className="view-header">
        <div>
          <h2>Quản Lý Danh Mục</h2>
          <p>Xem danh mục mặc định hệ thống và tạo thêm các danh mục chi tiêu/thu nhập tùy chỉnh</p>
        </div>
        <button onClick={() => setIsOpenModal(true)} className="btn-primary">
          <Plus size={18} />
          <span>Thêm Danh Mục Mới</span>
        </button>
      </div>

      {/* Grid Danh mục Thu nhập & Chi tiêu */}
      <div className="categories-grid">
        {/* Nhóm Thu nhập */}
        <div className="category-group-card">
          <div className="group-card-header income-header">
            <h3>Danh mục Thu nhập ({incomeCategories.length})</h3>
            <span className="badge-pill badge-income">Thu nhập</span>
          </div>

          <div className="category-chips-list">
            {incomeCategories.map((c) => (
              <div key={c.id} className="category-chip-item">
                <div className="chip-content">
                  <Tag size={16} className="text-income" />
                  <strong>{c.name}</strong>
                  {c.isCustom ? (
                    <span className="chip-tag tag-custom">Tùy chỉnh</span>
                  ) : (
                    <span className="chip-tag tag-default" title="Danh mục mặc định, không thể xóa">
                      <Lock size={12} /> Mặc định
                    </span>
                  )}
                </div>
                {c.isCustom && (
                  <button
                    onClick={() => handleDelete(c)}
                    className="btn-icon-sm text-danger"
                    title="Xóa danh mục này"
                  >
                    <Trash2 size={14} />
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Nhóm Chi tiêu */}
        <div className="category-group-card">
          <div className="group-card-header expense-header">
            <h3>Danh mục Chi tiêu ({expenseCategories.length})</h3>
            <span className="badge-pill badge-expense">Chi tiêu</span>
          </div>

          <div className="category-chips-list">
            {expenseCategories.map((c) => (
              <div key={c.id} className="category-chip-item">
                <div className="chip-content">
                  <Tag size={16} className="text-expense" />
                  <strong>{c.name}</strong>
                  {c.isCustom ? (
                    <span className="chip-tag tag-custom">Tùy chỉnh</span>
                  ) : (
                    <span className="chip-tag tag-default" title="Danh mục mặc định, không thể xóa">
                      <Lock size={12} /> Mặc định
                    </span>
                  )}
                </div>
                {c.isCustom && (
                  <button
                    onClick={() => handleDelete(c)}
                    className="btn-icon-sm text-danger"
                    title="Xóa danh mục này"
                  >
                    <Trash2 size={14} />
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Modal Thêm Danh Mục */}
      {isOpenModal && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <button onClick={() => setIsOpenModal(false)} className="modal-close-btn">
              <X size={20} />
            </button>

            <div className="modal-header">
              <h2>Thêm Danh Mục Tùy Chỉnh</h2>
              <p className="modal-subtitle">Tạo phân loại mới phù hợp với thói quen chi tiêu của bạn</p>
            </div>

            {error && (
              <div className="alert-box alert-error">
                <AlertCircle size={18} />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleCreate} className="auth-form">
              <div className="form-group">
                <label>Tên danh mục</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Ví dụ: Cà phê, Nuôi thú cưng, Học tập,..."
                />
              </div>

              <div className="form-group">
                <label>Loại danh mục</label>
                <div className="radio-group">
                  <label className={`radio-label ${type === 'EXPENSE' ? 'checked' : ''}`}>
                    <input
                      type="radio"
                      name="type"
                      checked={type === 'EXPENSE'}
                      onChange={() => setType('EXPENSE')}
                    />
                    <span>Chi tiêu (Expense)</span>
                  </label>
                  <label className={`radio-label ${type === 'INCOME' ? 'checked' : ''}`}>
                    <input
                      type="radio"
                      name="type"
                      checked={type === 'INCOME'}
                      onChange={() => setType('INCOME')}
                    />
                    <span>Thu nhập (Income)</span>
                  </label>
                </div>
              </div>

              <button type="submit" disabled={loading} className="btn-submit">
                {loading ? 'Đang tạo...' : 'Tạo Danh Mục'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
