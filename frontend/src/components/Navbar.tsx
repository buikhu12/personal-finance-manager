import React from 'react';
import {
  Wallet,
  LayoutDashboard,
  ReceiptText,
  Tags,
  Target,
  BarChart3,
  Sparkles,
  LogOut,
  User as UserIcon,
} from 'lucide-react';

interface NavbarProps {
  currentTab: string;
  setCurrentTab: (tab: string) => void;
  currentUser: string | null;
  onLogout: () => void;
  onOpenAuth: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentTab,
  setCurrentTab,
  currentUser,
  onLogout,
  onOpenAuth,
}) => {
  const navItems = [
    { id: 'dashboard', label: 'Tổng quan', icon: LayoutDashboard },
    { id: 'wallets', label: 'Ví', icon: Wallet },
    { id: 'transactions', label: 'Giao dịch', icon: ReceiptText },
    { id: 'categories', label: 'Danh mục', icon: Tags },
    { id: 'goals', label: 'Mục tiêu', icon: Target },
    { id: 'reports', label: 'Báo cáo', icon: BarChart3 },
    { id: 'ai', label: 'Cố vấn AI', icon: Sparkles, badge: 'Smart' },
  ];

  return (
    <header className="navbar-container">
      <div className="navbar-content">
        {/* Logo */}
        <div className="navbar-brand" onClick={() => setCurrentTab('dashboard')}>
          <div className="brand-icon">
            <Wallet size={24} />
          </div>
          <div>
            <h1 className="brand-title">Finance Manager</h1>
            <span className="brand-subtitle">Quản lý Tài chính Cá nhân</span>
          </div>
        </div>

        {/* Navigation Tabs (khi đã đăng nhập) */}
        {currentUser && (
          <nav className="navbar-links">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = currentTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setCurrentTab(item.id)}
                  className={`nav-button ${isActive ? 'active' : ''}`}
                >
                  <Icon size={18} />
                  <span>{item.label}</span>
                  {item.badge && <span className="nav-badge">{item.badge}</span>}
                </button>
              );
            })}
          </nav>
        )}

        {/* User Account / Auth Actions */}
        <div className="navbar-actions">
          {currentUser ? (
            <div className="user-profile">
              <div className="user-avatar">
                <UserIcon size={16} />
              </div>
              <span className="username-text">{currentUser}</span>
              <button onClick={onLogout} className="logout-btn" title="Đăng xuất">
                <LogOut size={16} />
                <span>Đăng xuất</span>
              </button>
            </div>
          ) : (
            <button onClick={onOpenAuth} className="btn-primary">
              <UserIcon size={16} />
              <span>Đăng nhập / Đăng ký</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
