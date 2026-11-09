import React, { useState } from 'react';
import {
  Plus,
  Trash2,
  Edit2,
  Search,
  Filter,
  ArrowUpRight,
  ArrowDownLeft,
  Calendar,
  X,
  AlertCircle,
} from 'lucide-react';
import type { TransactionResponse, TransactionRequest, CategoryResponse } from '../types';
import { transactionApi } from '../services/api';

interface TransactionsViewProps {
  transactions: TransactionResponse[];
  categories: CategoryResponse[];
  onRefresh: () => void;
  isOpenAddModal: boolean;
  onCloseAddModal: () => void;
  onOpenAddModal: () => void;
}

export const TransactionsView: React.FC<TransactionsViewProps> = ({
  transactions,
  categories,
  onRefresh,
  isOpenAddModal,
  onCloseAddModal,
  onOpenAddModal,
}) => {
  const [filterType, setFilterType] = useState<'ALL' | 'INCOME' | 'EXPENSE'>('ALL');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // State cho Modal Tạo / Chỉnh sửa
  const [editingTx, setEditingTx] = useState<TransactionResponse | null>(null);
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [category, setCategory] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Khi mở form tạo mới
  const handleOpenCreate = () => {
    setEditingTx(null);
    setAmount('');
    setDate(new Date().toISOString().split('T')[0]);
    setCategory(categories[0]?.name || '');
    setDescription('');
    setError(null);
    onOpenAddModal();
  };

  // Khi mở form sửa
  const handleOpenEdit = (tx: TransactionResponse) => {
    setEditingTx(tx);
    setAmount(tx.amount.toString());
    setDate(tx.date);
    setCategory(tx.category);
    setDescription(tx.description || '');
    setError(null);
    onOpenAddModal();
  };

  // Submit Lưu
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      setError('Số tiền phải là số dương lớn hơn 0.');
      return;
    }

    if (!category) {
      setError('Vui lòng chọn danh mục.');
      return;
    }

    // Kiểm tra ngày không được ở tương lai
    const selectedDate = new Date(date);
    const today = new Date();
    today.setHours(23, 59, 59, 999);
    if (selectedDate > today) {
      setError('Ngày giao dịch không thể ở tương lai.');
      return;
    }

    const payload: TransactionRequest = {
      amount: numAmount,
      date,
      category,
      description,
    };

    setLoading(true);
    try {
      if (editingTx) {
        await transactionApi.update(editingTx.id, payload);
      } else {
        await transactionApi.create(payload);
      }
      onCloseAddModal();
      onRefresh();
    } catch (err: any) {
      setError(err.message || 'Lỗi khi lưu giao dịch.');
    } finally {
      setLoading(false);
    }
  };

  // Xóa giao dịch
  const handleDelete = async (id: number) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa giao dịch này?')) return;
    try {
      await transactionApi.delete(id);
      onRefresh();
    } catch (err: any) {
      alert(err.message || 'Lỗi khi xóa giao dịch.');
    }
  };

  // Bộ lọc danh sách
  const filtered = transactions.filter((t) => {
    if (filterType !== 'ALL' && t.type !== filterType) return false;
    if (selectedCategory !== 'ALL' && t.category !== selectedCategory) return false;
    if (searchQuery.trim() !== '') {
      const q = searchQuery.toLowerCase();
      const matchDesc = t.description?.toLowerCase().includes(q);
      const matchCat = t.category.toLowerCase().includes(q);
      if (!matchDesc && !matchCat) return false;
    }
    return true;
  });

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  return (
    <div className="view-container">
      {/* Header */}
      <div className="view-header">
        <div>
          <h2>Quản Lý Giao Dịch</h2>
          <p>Theo dõi thu nhập và chi tiêu của bạn một cách trực quan</p>
        </div>
        <button onClick={handleOpenCreate} className="btn-primary">
          <Plus size={18} />
          <span>Thêm Giao Dịch</span>
        </button>
      </div>

      {/* Filter Toolbar */}
      <div className="filter-bar">
        {/* Search */}
        <div className="search-input-wrapper">
          <Search size={18} />
          <input
            type="text"
            placeholder="Tìm theo mô tả hoặc danh mục..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Type Filter */}
        <div className="filter-group">
          <button
            className={`filter-chip ${filterType === 'ALL' ? 'active' : ''}`}
            onClick={() => setFilterType('ALL')}
          >
            Tất cả
          </button>
          <button
            className={`filter-chip ${filterType === 'INCOME' ? 'active' : ''}`}
            onClick={() => setFilterType('INCOME')}
          >
            Thu nhập
          </button>
          <button
            className={`filter-chip ${filterType === 'EXPENSE' ? 'active' : ''}`}
            onClick={() => setFilterType('EXPENSE')}
          >
            Chi tiêu
          </button>
        </div>

        {/* Category Select */}
        <div className="category-select-wrapper">
          <Filter size={16} />
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
          >
            <option value="ALL">Tất cả danh mục</option>
            {categories.map((c) => (
              <option key={c.id} value={c.name}>
                {c.name} ({c.type === 'INCOME' ? 'Thu' : 'Chi'})
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Transactions Table */}
      <div className="table-card">
        {filtered.length === 0 ? (
          <div className="empty-state">
            <AlertCircle size={32} />
            <p>Không tìm thấy giao dịch nào phù hợp.</p>
          </div>
        ) : (
          <table className="custom-table">
            <thead>
              <tr>
                <th>Loại</th>
                <th>Danh mục</th>
                <th>Mô tả</th>
                <th>Ngày</th>
                <th style={{ textAlign: 'right' }}>Số tiền</th>
                <th style={{ textAlign: 'center' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((tx) => (
                <tr key={tx.id}>
                  <td>
                    <span className={`badge-pill ${tx.type === 'INCOME' ? 'badge-income' : 'badge-expense'}`}>
                      {tx.type === 'INCOME' ? <ArrowUpRight size={14} /> : <ArrowDownLeft size={14} />}
                      {tx.type === 'INCOME' ? 'Thu' : 'Chi'}
                    </span>
                  </td>
                  <td>
                    <span className="font-medium">{tx.category}</span>
                  </td>
                  <td className="text-muted">{tx.description || '—'}</td>
                  <td>
                    <div className="flex-center-gap">
                      <Calendar size={14} className="text-muted" />
                      <span>{tx.date}</span>
                    </div>
                  </td>
                  <td style={{ textAlign: 'right' }} className={tx.type === 'INCOME' ? 'text-income font-bold' : 'text-expense font-bold'}>
                    {tx.type === 'INCOME' ? '+' : '-'} {formatCurrency(tx.amount)}
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <div className="action-buttons">
                      <button
                        onClick={() => handleOpenEdit(tx)}
                        className="btn-icon"
                        title="Chỉnh sửa"
                      >
                        <Edit2 size={16} />
                      </button>
                      <button
                        onClick={() => handleDelete(tx.id)}
                        className="btn-icon text-danger"
                        title="Xóa"
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Modal Thêm / Chỉnh Sửa Giao Dịch */}
      {isOpenAddModal && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <button onClick={onCloseAddModal} className="modal-close-btn">
              <X size={20} />
            </button>

            <div className="modal-header">
              <h2>{editingTx ? 'Chỉnh Sửa Giao Dịch' : 'Thêm Giao Dịch Mới'}</h2>
              <p className="modal-subtitle">Ghi lại các khoản thu hoặc chi để AI phân tích chính xác</p>
            </div>

            {error && (
              <div className="alert-box alert-error">
                <AlertCircle size={18} />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="form-group">
                <label>Số tiền (VNĐ)</label>
                <input
                  type="number"
                  step="any"
                  required
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="Ví dụ: 500000"
                />
              </div>

              <div className="form-group">
                <label>Danh mục</label>
                <select
                  required
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                >
                  <option value="" disabled>-- Chọn danh mục --</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.name}>
                      {c.name} ({c.type === 'INCOME' ? 'Thu nhập' : 'Chi tiêu'})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Ngày giao dịch (Không chọn ngày tương lai)</label>
                <input
                  type="date"
                  required
                  max={new Date().toISOString().split('T')[0]}
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label>Mô tả chi tiết (Tùy chọn)</label>
                <input
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Ví dụ: Mua sắm siêu thị cuối tuần"
                />
              </div>

              <button type="submit" disabled={loading} className="btn-submit">
                {loading ? 'Đang lưu...' : editingTx ? 'Cập Nhật Giao Dịch' : 'Thêm Giao Dịch'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
