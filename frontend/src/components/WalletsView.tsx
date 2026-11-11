import React, { useState } from 'react';
import { CreditCard, Edit2, Landmark, Plus, Trash2, Wallet as WalletIcon, X, AlertCircle } from 'lucide-react';
import type { WalletRequest, WalletResponse } from '../types';
import { walletApi } from '../services/api';
import { WalletBankAccountDialog } from './WalletBankAccountDialog';

interface WalletsViewProps {
  wallets: WalletResponse[];
  onRefresh: () => void;
}

export const WalletsView: React.FC<WalletsViewProps> = ({ wallets, onRefresh }) => {
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

  return (
    <div className="view-container">
      <div className="view-header">
        <div>
          <h2>Ví & Tài Khoản</h2>
          <p>Quản lý tiền mặt, tài khoản ngân hàng và các nguồn tiền riêng biệt</p>
        </div>
        <button onClick={openCreate} className="btn-primary">
          <Plus size={18} />
          <span>Thêm Ví</span>
        </button>
      </div>

      <div className="wallet-total-card">
        <div className="wallet-total-icon"><WalletIcon size={24} /></div>
        <div>
          <span className="wallet-total-label">Tổng số dư tất cả các ví</span>
          <strong className="wallet-total-value">{formatCurrency(totalBalance)}</strong>
        </div>
      </div>

      {wallets.length === 0 ? (
        <div className="empty-state">
          <CreditCard size={32} />
          <p>Chưa có ví nào. Hãy tạo ví đầu tiên của bạn.</p>
          <button onClick={openCreate} className="btn-secondary">Tạo ví mới</button>
        </div>
      ) : (
        <div className="wallets-grid">
          {wallets.map((wallet) => (
            <div key={wallet.id} className="wallet-card">
              <div className="wallet-card-header">
                <div className="wallet-name-group">
                  <div className="wallet-icon"><CreditCard size={20} /></div>
                  <div>
                    <h3>{wallet.name}</h3>
                    {wallet.isDefault && <span className="wallet-default-tag">Ví mặc định</span>}
                  </div>
                </div>
                <div className="action-buttons">
                  <button onClick={() => openEdit(wallet)} className="btn-icon" title="Chỉnh sửa">
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

              <div className="wallet-card-footer">
                <span>Ban đầu: {formatCurrency(Number(wallet.initialBalance))}</span>
                <span>{wallet.transactionCount} giao dịch</span>
              </div>

              <div className="wallet-bank-account">
                {wallet.bankName && wallet.bankAccountLastFour && (
                  <span className="wallet-bank-detail">
                    <Landmark size={16} aria-hidden="true" />
                    <span>{wallet.bankName} · •••• {wallet.bankAccountLastFour}</span>
                  </span>
                )}
                <button type="button" className="btn-link" onClick={() => setBankAccountWallet(wallet)}>
                  {wallet.bankName ? 'Sửa thông tin ngân hàng' : 'Gắn tài khoản ngân hàng'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

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

      {isModalOpen && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <button onClick={closeModal} className="modal-close-btn" disabled={loading}>
              <X size={20} />
            </button>

            <div className="modal-header">
              <h2>{editingWallet ? 'Chỉnh Sửa Ví' : 'Thêm Ví Mới'}</h2>
              <p className="modal-subtitle">Số dư ví sẽ được cập nhật tự động theo các giao dịch.</p>
            </div>

            {error && (
              <div className="alert-box alert-error">
                <AlertCircle size={18} />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="form-group">
                <label>Tên ví</label>
                <input
                  type="text"
                  required
                  value={name}
                  disabled={editingWallet?.isDefault}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="Ví dụ: Tiền mặt, Tài khoản ngân hàng"
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
              </div>

              <button type="submit" disabled={loading} className="btn-submit">
                {loading ? 'Đang lưu...' : editingWallet ? 'Cập Nhật Ví' : 'Tạo Ví'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
