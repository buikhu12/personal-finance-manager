import React, { useState } from 'react';
import { authApi } from '../services/api';
import { Lock, Mail, User, Phone, X, AlertCircle, CheckCircle2 } from 'lucide-react';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (username: string) => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('testuser@example.com');
  const [password, setPassword] = useState('Test@1234');
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);
    setLoading(true);

    try {
      if (isRegister) {
        if (!fullName || !phoneNumber) {
          throw new Error('Vui lòng điền đầy đủ họ tên và số điện thoại.');
        }
        await authApi.register({
          username,
          password,
          fullName,
          phoneNumber,
        });
        setSuccessMsg('Đăng ký tài khoản thành công! Đang chuyển sang đăng nhập...');
        setTimeout(() => {
          setIsRegister(false);
          setSuccessMsg(null);
        }, 1200);
      } else {
        const res = await authApi.login({ username, password });
        onSuccess(res.username || username);
        onClose();
      }
    } catch (err: any) {
      setError(err.message || 'Đã xảy ra lỗi, vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-card">
        {/* Close Button */}
        <button onClick={onClose} className="modal-close-btn">
          <X size={20} />
        </button>

        <div className="modal-header">
          <h2>{isRegister ? 'Tạo Tài Khoản Mới' : 'Chào Mừng Trở Lại'}</h2>
          <p className="modal-subtitle">
            {isRegister
              ? 'Điền thông tin để bắt đầu quản lý tài chính thông minh'
              : 'Đăng nhập vào tài khoản Personal Finance Manager của bạn'}
          </p>
        </div>

        {/* Tab switch */}
        <div className="auth-tabs">
          <button
            type="button"
            className={`auth-tab ${!isRegister ? 'active' : ''}`}
            onClick={() => {
              setIsRegister(false);
              setError(null);
            }}
          >
            Đăng nhập
          </button>
          <button
            type="button"
            className={`auth-tab ${isRegister ? 'active' : ''}`}
            onClick={() => {
              setIsRegister(true);
              setError(null);
            }}
          >
            Đăng ký
          </button>
        </div>

        {error && (
          <div className="alert-box alert-error">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        {successMsg && (
          <div className="alert-box alert-success">
            <CheckCircle2 size={18} />
            <span>{successMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          {/* Email / Username */}
          <div className="form-group">
            <label>Email / Tên đăng nhập</label>
            <div className="input-with-icon">
              <Mail size={18} className="input-icon" />
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="vidu@example.com"
              />
            </div>
          </div>

          {/* Password */}
          <div className="form-group">
            <label>Mật khẩu</label>
            <div className="input-with-icon">
              <Lock size={18} className="input-icon" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
              />
            </div>
          </div>

          {/* Trường bổ sung khi Đăng ký */}
          {isRegister && (
            <>
              <div className="form-group">
                <label>Họ và tên</label>
                <div className="input-with-icon">
                  <User size={18} className="input-icon" />
                  <input
                    type="text"
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Nguyễn Văn A"
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Số điện thoại</label>
                <div className="input-with-icon">
                  <Phone size={18} className="input-icon" />
                  <input
                    type="tel"
                    required
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    placeholder="0912345678"
                  />
                </div>
              </div>
            </>
          )}

          <button type="submit" disabled={loading} className="btn-submit">
            {loading ? 'Đang xử lý...' : isRegister ? 'Đăng Ký Tài Khoản' : 'Đăng Nhập'}
          </button>
        </form>

        <div className="demo-credentials-note">
          💡 <strong>Tài khoản Demo có sẵn:</strong><br />
          Email: <code>testuser@example.com</code> | Mật khẩu: <code>Test@1234</code>
        </div>
      </div>
    </div>
  );
};
