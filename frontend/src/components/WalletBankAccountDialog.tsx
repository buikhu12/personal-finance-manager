import { useState } from 'react';
import type { FormEvent } from 'react';
import { AlertCircle, X } from 'lucide-react';
import { walletApi } from '../services/api';
import type { WalletBankAccountRequest, WalletResponse } from '../types';

interface WalletBankAccountDialogProps {
  wallet: WalletResponse;
  onClose: () => void;
  onSaved: () => void;
}

export const WalletBankAccountDialog = ({ wallet, onClose, onSaved }: WalletBankAccountDialogProps) => {
  const [bankName, setBankName] = useState(wallet.bankName ?? '');
  const [lastFour, setLastFour] = useState(wallet.bankAccountLastFour ?? '');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const saveBankAccount = async (details: WalletBankAccountRequest | null) => {
    setSaving(true);
    setError('');

    try {
      if (details) {
        await walletApi.linkBankAccount(wallet.id, details);
      } else {
        await walletApi.unlinkBankAccount(wallet.id);
      }
      onSaved();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể cập nhật thông tin ngân hàng.');
    } finally {
      setSaving(false);
    }
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedBankName = bankName.trim();
    if (!trimmedBankName) {
      setError('Vui lòng nhập tên ngân hàng.');
      return;
    }
    void saveBankAccount({ bankName: trimmedBankName, lastFour });
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-card" role="dialog" aria-modal="true" aria-labelledby="wallet-bank-title">
        <button type="button" className="modal-close-btn" aria-label="Đóng" onClick={onClose} disabled={saving}>
          <X size={20} />
        </button>

        <div className="modal-header">
          <h2 id="wallet-bank-title">Tài khoản ngân hàng</h2>
          <p className="modal-subtitle">
            Lưu thông tin nhận diện tài khoản cho ví “{wallet.name}”. Giao dịch vẫn được ghi thủ công.
          </p>
        </div>

        {error && (
          <div className="alert-box alert-error" role="alert">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="wallet-bank-name">Tên ngân hàng</label>
            <input
              id="wallet-bank-name"
              required
              maxLength={100}
              value={bankName}
              onChange={(event) => setBankName(event.target.value)}
              placeholder="Ví dụ: Vietcombank"
              disabled={saving}
            />
          </div>
          <div className="form-group">
            <label htmlFor="wallet-bank-last-four">4 số cuối tài khoản</label>
            <input
              id="wallet-bank-last-four"
              type="text"
              inputMode="numeric"
              pattern="[0-9]{4}"
              title="Nhập đúng 4 chữ số cuối tài khoản"
              required
              value={lastFour}
              onChange={(event) => setLastFour(event.target.value)}
              placeholder="Ví dụ: 0042"
              disabled={saving}
            />
          </div>
          <button type="submit" className="btn-submit" disabled={saving}>
            {saving ? 'Đang lưu...' : 'Lưu thông tin ngân hàng'}
          </button>
          {wallet.bankName && (
            <button type="button" className="btn-secondary" onClick={() => void saveBankAccount(null)} disabled={saving}>
              Gỡ liên kết
            </button>
          )}
        </form>
      </div>
    </div>
  );
};
