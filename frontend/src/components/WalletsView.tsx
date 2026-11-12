import React, { useState } from 'react';
import {
  CreditCard,
  Edit2,
  Landmark,
  Plus,
  Trash2,
  Wallet as WalletIcon,
  X,
  AlertCircle,
  ArrowUpRight,
  ArrowDownLeft,
  ReceiptText,
  PlusCircle,
} from 'lucide-react';
import type { WalletRequest, WalletResponse, TransactionResponse } from '../types';
import { walletApi } from '../services/api';
import { WalletBankAccountDialog } from './WalletBankAccountDialog';

interface WalletsViewProps {
  wallets: WalletResponse[];
  transactions?: TransactionResponse[];
  onRefresh: () => void;
  onViewWalletTransactions?: (walletId: number) => void;
  onAddTransactionForWallet?: (walletId: number) => void;
}

const WALLET_SUGGESTIONS = [
  'Tiền mặt',
  'Vietcombank',
  'Techcombank',
  'MB Bank',
  'VPBank',
  'MoMo',
  'ZaloPay',
  'Quỹ Tiết Kiệm',
];

export const WalletsView: React.FC<WalletsViewProps> = ({
  wallets,
  transactions = [],
  onRefresh,
  onViewWalletTransactions,
  onAddTransactionForWallet,
}) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingWallet, setEditingWallet] = useState<WalletResponse | null>(null);
  const [bankAccountWallet, setBankAccountWallet] = useState<WalletResponse | null>(null);
  const [name, setName] = useState('');
  const [initialBalance, setInitialBalance] = useState('0');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);

  const openCreate = () => {
    setEditingWallet(null);
    setName('');
    setInitialBalance('0');
    setError(null);
    setIsModalOpen(true);
  };

  const openEdit = (wallet: WalletResponse) => {
    setEditingWallet(wallet);
    setName(wallet.name);
    setInitialBalance(String(wallet.initialBalance));
    setError(null);
    setIsModalOpen(true);
  };

  const closeModal = () => {
    if (!loading) setIsModalOpen(false);
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    const parsedBalance = Number(initialBalance);
    if (!name.trim()) {
      setError('Vui lòng nhập tên ví.');
      return;
    }
    if (!Number.isFinite(parsedBalance) || parsedBalance < 0) {
      setError('Số dư ban đầu phải là số không âm.');
      return;
    }

    const payload: WalletRequest = {
      name: name.trim(),
      initialBalance: parsedBalance,
    };

    setLoading(true);
    try {
      if (editingWallet) {
        await walletApi.update(editingWallet.id, payload);
      } else {
        await walletApi.create(payload);
      }
      setIsModalOpen(false);
      onRefresh();
    } catch (err: any) {
      setError(err.message || 'Không thể lưu ví.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (wallet: WalletResponse) => {
    if (wallet.isDefault) {
      alert('Ví mặc định không thể xóa.');
      return;
    }
    if (!window.confirm(`Bạn có chắc muốn xóa ví “${wallet.name}”?`)) return;

    try {
      await walletApi.delete(wallet.id);
      onRefresh();
    } catch (err: any) {
      alert(err.message || 'Không thể xóa ví.');
    }
  };

  const totalBalance = wallets.reduce((sum, wallet) => sum + Number(wallet.currentBalance), 0);
  const totalInitial = wallets.reduce((sum, wallet) => sum + Number(wallet.initialBalance), 0);

  // Tính tổng thu / chi từ transactions
  const totalIncomeAll = transactions
    .filter((t) => t.type === 'INCOME')
    .reduce((sum, t) => sum + Number(t.amount), 0);

  const totalExpenseAll = transactions
    .filter((t) => t.type === 'EXPENSE')
    .reduce((sum, t) => sum + Number(t.amount), 0);

  return (
    <div className="view-container">
      {/* Header */}
      <div className="view-header">
        <div>
          <h2>Ví & Tài Khoản</h2>
          <p>Quản lý tiền mặt, tài khoản ngân hàng và các nguồn tiền riêng biệt</p>
        </div>
        <button onClick={openCreate} className="btn-primary">
          <Plus size={18} />
          <span>Thêm Ví Mới</span>
        </button>
      </div>

      {/* KPI Cards for Wallets */}
      <div className="wallet-summary-cards">
        <div className="wallet-total-card">
          <div className="wallet-total-icon">
            <WalletIcon size={28} />
          </div>
          <div>
            <span className="wallet-total-label">Tổng tài sản hiện có</span>
            <strong className="wallet-total-value">{formatCurrency(totalBalance)}</strong>
            <span className="wallet-subtext">Số dư gộp của {wallets.length} ví hoạt động</span>
          </div>
        </div>

        <div className="wallet-stat-card">
          <div className="stat-group">
            <span className="stat-label">Số dư khởi tạo ban đầu</span>
            <strong className="stat-value">{formatCurrency(totalInitial)}</strong>
          </div>
          <div className="stat-divider"></div>
          <div className="stat-row">
            <div className="stat-item income">
              <span className="stat-item-label">
                <ArrowUpRight size={14} /> Tổng thu
              </span>
              <strong className="stat-item-val">{formatCurrency(totalIncomeAll)}</strong>
            </div>
            <div className="stat-item expense">
              <span className="stat-item-label">
                <ArrowDownLeft size={14} /> Tổng chi
              </span>
              <strong className="stat-item-val">{formatCurrency(totalExpenseAll)}</strong>
            </div>
          </div>
        </div>
      </div>

      {/* Danh sách các ví */}
      {wallets.length === 0 ? (
        <div className="empty-state">
          <CreditCard size={40} />
          <p>Chưa có ví nào được tạo. Hãy tạo ví đầu tiên để phân loại dòng tiền!</p>
          <button onClick={openCreate} className="btn-secondary">
            Tạo ví mới ngay
          </button>
        </div>
      ) : (
        <div className="wallets-grid">
          {wallets.map((wallet) => {
            // Lọc giao dịch của ví này
            const walletTxs = transactions.filter(
              (t) => t.walletId === wallet.id || (wallet.isDefault && t.walletId == null)
            );
            const walletIncome = walletTxs
              .filter((t) => t.type === 'INCOME')
              .reduce((s, t) => s + Number(t.amount), 0);
            const walletExpense = walletTxs
              .filter((t) => t.type === 'EXPENSE')
              .reduce((s, t) => s + Number(t.amount), 0);

            const hasBank = Boolean(wallet.bankName && wallet.bankAccountLastFour);

            return (
              <div key={wallet.id} className="wallet-card">
                <div className="wallet-card-header">
                  <div className="wallet-name-group">
                    <div className={`wallet-icon ${hasBank ? 'icon-bank' : 'icon-cash'}`}>
                      {hasBank ? <Landmark size={20} /> : <CreditCard size={20} />}
                    </div>
                    <div>
                      <h3>{wallet.name}</h3>
                      {wallet.isDefault && <span className="wallet-default-tag">Ví mặc định</span>}
                    </div>
                  </div>
                  <div className="action-buttons">
                    <button
                      onClick={() => openEdit(wallet)}
                      className="btn-icon"
                      title="Chỉnh sửa thông tin ví"
                    >
                      <Edit2 size={16} />
                    </button>
                    <button
                      onClick={() => handleDelete(wallet)}
                      className="btn-icon text-danger"
                      title={wallet.isDefault ? 'Ví mặc định không thể xóa' : 'Xóa ví'}
                      disabled={wallet.isDefault}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>

                <div className="wallet-balance-label">Số dư hiện tại</div>
                <div className={`wallet-balance ${wallet.currentBalance < 0 ? 'text-danger' : ''}`}>
                  {formatCurrency(Number(wallet.currentBalance))}
                </div>

                {/* Dòng tiền của ví */}
                <div className="wallet-flow-row">
                  <span className="wallet-flow-income" title="Tổng thu qua ví">
                    <ArrowUpRight size={13} /> {formatCurrency(walletIncome)}
                  </span>
                  <span className="wallet-flow-expense" title="Tổng chi qua ví">
                    <ArrowDownLeft size={13} /> {formatCurrency(walletExpense)}
                  </span>
                </div>

                <div className="wallet-card-footer">
                  <span>Ban đầu: {formatCurrency(Number(wallet.initialBalance))}</span>
                  <span>{wallet.transactionCount} giao dịch</span>
                </div>

                {/* Thông tin ngân hàng liên kết */}
                <div className="wallet-bank-account">
                  {hasBank ? (
                    <span className="wallet-bank-detail">
                      <Landmark size={15} aria-hidden="true" />
                      <span>{wallet.bankName} · •••• {wallet.bankAccountLastFour}</span>
                    </span>
                  ) : (
                    <span className="wallet-bank-empty">Chưa gắn tài khoản ngân hàng</span>
                  )}
                  <button
                    type="button"
                    className="btn-link"
                    onClick={() => setBankAccountWallet(wallet)}
                  >
                    {hasBank ? 'Sửa ngân hàng' : '+ Gắn ngân hàng'}
                  </button>
                </div>

                {/* Quick actions for wallet */}
                <div className="wallet-card-actions">
                  {onViewWalletTransactions && (
                    <button
                      type="button"
                      className="btn-wallet-action"
                      onClick={() => onViewWalletTransactions(wallet.id)}
                      title="Xem toàn bộ giao dịch của ví này"
                    >
                      <ReceiptText size={14} />
                      <span>Xem giao dịch</span>
                    </button>
                  )}
                  {onAddTransactionForWallet && (
                    <button
                      type="button"
                      className="btn-wallet-action btn-wallet-action-primary"
                      onClick={() => onAddTransactionForWallet(wallet.id)}
                      title="Ghi nhận giao dịch mới vào ví này"
                    >
                      <PlusCircle size={14} />
                      <span>Ghi thu/chi</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal gắn tài khoản ngân hàng */}
      {bankAccountWallet && (
        <WalletBankAccountDialog
          wallet={bankAccountWallet}
          onClose={() => setBankAccountWallet(null)}
          onSaved={() => {
            setBankAccountWallet(null);
            onRefresh();
          }}
        />
      )}

      {/* Modal Tạo / Chỉnh sửa Ví */}
      {isModalOpen && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <button onClick={closeModal} className="modal-close-btn" disabled={loading}>
              <X size={20} />
            </button>

            <div className="modal-header">
              <h2>{editingWallet ? 'Chỉnh Sửa Ví' : 'Thêm Ví Mới'}</h2>
              <p className="modal-subtitle">
                {editingWallet
                  ? 'Cập nhật tên và số dư ban đầu cho ví của bạn'
                  : 'Tạo ví mới để phân loại tiền mặt, ngân hàng hoặc ví điện tử'}
              </p>
            </div>

            {error && (
              <div className="alert-box alert-error">
                <AlertCircle size={18} />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="auth-form">
              {/* Suggestions chips khi tạo ví mới */}
              {!editingWallet && (
                <div className="form-group">
                  <label>Gợi ý nhanh tên ví:</label>
                  <div className="wallet-suggestions-list">
                    {WALLET_SUGGESTIONS.map((suggestion) => (
                      <button
                        key={suggestion}
                        type="button"
                        className={`suggestion-chip ${name === suggestion ? 'active' : ''}`}
                        onClick={() => setName(suggestion)}
                      >
                        {suggestion}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div className="form-group">
                <label>Tên ví / Tài khoản</label>
                <input
                  type="text"
                  required
                  value={name}
                  disabled={editingWallet?.isDefault}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="Ví dụ: Tiền mặt, Vietcombank, Ví MoMo..."
                />
              </div>

              <div className="form-group">
                <label>Số dư ban đầu (VNĐ)</label>
                <input
                  type="number"
                  min="0"
                  step="any"
                  required
                  value={initialBalance}
                  onChange={(event) => setInitialBalance(event.target.value)}
                  placeholder="Ví dụ: 5000000"
                />
                <span className="field-hint">
                  Số tiền hiện có trong ví trước khi thực hiện các giao dịch trên app.
                </span>
              </div>

              <button type="submit" disabled={loading} className="btn-submit">
                {loading ? 'Đang lưu...' : editingWallet ? 'Cập Nhật Ví' : 'Tạo Ví Mới'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
