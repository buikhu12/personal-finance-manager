import React, { useState, useEffect } from 'react';
import { Navbar } from './components/Navbar';
import { AuthModal } from './components/AuthModal';
import { DashboardView } from './components/DashboardView';
import { TransactionsView } from './components/TransactionsView';
import { CategoriesView } from './components/CategoriesView';
import { GoalsView } from './components/GoalsView';
import { ReportsView } from './components/ReportsView';
import { AiAdvisorView } from './components/AiAdvisorView';
import { WalletsView } from './components/WalletsView';
import type {
  TransactionResponse,
  CategoryResponse,
  GoalResponse,
  AiAdviceResponse,
  WalletResponse,
} from './types';
import {
  transactionApi,
  categoryApi,
  goalApi,
  aiApi,
  authApi,
  walletApi,
} from './services/api';
import { Wallet, LogIn, Sparkles, ShieldCheck } from 'lucide-react';
import './App.css';

export const App: React.FC = () => {
  const [currentUser, setCurrentUser] = useState<string | null>(() => {
    return localStorage.getItem('finance_user');
  });

  const [currentTab, setCurrentTab] = useState<string>('dashboard');
  const [isAuthOpen, setIsAuthOpen] = useState(false);
  const [isOpenAddTxModal, setIsOpenAddTxModal] = useState(false);

  // Dữ liệu ứng dụng
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [goals, setGoals] = useState<GoalResponse[]>([]);
  const [wallets, setWallets] = useState<WalletResponse[]>([]);
  const [aiAdvice, setAiAdvice] = useState<AiAdviceResponse | null>(null);
  const [loading, setLoading] = useState(false);

  // Tải lại tất cả dữ liệu
  const refreshAllData = async () => {
    if (!currentUser) return;
    setLoading(true);
    try {
      const [txList, catList, goalList, walletList] = await Promise.all([
        transactionApi.getAll().catch(() => []),
        categoryApi.getAll().catch(() => []),
        goalApi.getAll().catch(() => []),
        walletApi.getAll().catch(() => []),
      ]);
      setTransactions(txList);
      setCategories(catList);
      setGoals(goalList);
      setWallets(walletList);

      // Thử lấy nhanh đánh giá AI cho tháng hiện tại
      try {
        const advice = await aiApi.getCurrentAdvice();
        setAiAdvice(advice);
      } catch {
        // Bỏ qua nếu chưa đủ giao dịch
      }
    } catch (err) {
      console.error('Lỗi khi tải dữ liệu:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (currentUser) {
      refreshAllData();
    } else {
      setTransactions([]);
      setCategories([]);
      setGoals([]);
      setWallets([]);
      setAiAdvice(null);
    }
  }, [currentUser]);

  // Xử lý đăng nhập thành công
  const handleAuthSuccess = (username: string) => {
    localStorage.setItem('finance_user', username);
    setCurrentUser(username);
    setCurrentTab('dashboard');
  };

  // Xử lý đăng xuất
  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch {
      // Bỏ qua lỗi
    } finally {
      localStorage.removeItem('finance_user');
      setCurrentUser(null);
      setCurrentTab('dashboard');
    }
  };

  return (
    <div className="app-layout">
      {/* Navbar Header */}
      <Navbar
        currentTab={currentTab}
        setCurrentTab={setCurrentTab}
        currentUser={currentUser}
        onLogout={handleLogout}
        onOpenAuth={() => setIsAuthOpen(true)}
      />

      {/* Main Content Area */}
      <main className="main-content">
        {!currentUser ? (
          /* Màn hình giới thiệu khi chưa đăng nhập */
          <div className="hero-landing">
            <div className="hero-badge">
              <Sparkles size={16} /> Quản lý tài chính cá nhân thông minh
            </div>
            <h1 className="hero-heading">
              Làm Chủ Tài Chính Cá Nhân Với <span className="highlight-text">Trợ Lý AI</span>
            </h1>
            <p className="hero-description">
              Theo dõi thu nhập, chi tiêu chặt chẽ, thiết lập các mục tiêu tiết kiệm thông minh
              và nhận các phân tích sức khỏe tài chính chuyên sâu từ thuật toán AI.
            </p>

            <div className="hero-cta-buttons">
              <button onClick={() => setIsAuthOpen(true)} className="btn-hero-primary">
                <LogIn size={20} />
                <span>Bắt Đầu Ngay - Đăng Nhập / Đăng Ký</span>
              </button>
            </div>

            <div className="hero-features-grid">
              <div className="feature-item">
                <div className="feature-icon"><Wallet size={24} /></div>
                <h3>Quản Lý Thu Chi</h3>
                <p>Ghi nhận nhanh chóng các giao dịch hàng ngày với danh mục rõ ràng.</p>
              </div>

              <div className="feature-item">
                <div className="feature-icon"><Sparkles size={24} /></div>
                <h3>Cố Vấn AI</h3>
                <p>Tự động phân tích tỷ lệ tiết kiệm, cảnh báo bội chi và đề xuất cắt giảm.</p>
              </div>

              <div className="feature-item">
                <div className="feature-icon"><ShieldCheck size={24} /></div>
                <h3>An Toàn & Bảo Mật</h3>
                <p>Mật khẩu mã hóa BCrypt chuẩn cao cấp, bảo mật phiên đăng nhập an toàn.</p>
              </div>
            </div>
          </div>
        ) : (
          /* Màn hình các chức năng khi đã đăng nhập */
          <div className="view-wrapper">
            {loading && <div className="top-progress-bar"></div>}

            {currentTab === 'dashboard' && (
              <DashboardView
                transactions={transactions}
                goals={goals}
                wallets={wallets}
                aiAdvice={aiAdvice}
                onNavigate={setCurrentTab}
                onOpenAddTransaction={() => setIsOpenAddTxModal(true)}
              />
            )}

            {currentTab === 'transactions' && (
              <TransactionsView
                transactions={transactions}
                categories={categories}
                wallets={wallets}
                onRefresh={refreshAllData}
                isOpenAddModal={isOpenAddTxModal}
                onCloseAddModal={() => setIsOpenAddTxModal(false)}
                onOpenAddModal={() => setIsOpenAddTxModal(true)}
              />
            )}

            {currentTab === 'wallets' && (
              <WalletsView
                wallets={wallets}
                onRefresh={refreshAllData}
              />
            )}

            {currentTab === 'categories' && (
              <CategoriesView
                categories={categories}
                onRefresh={refreshAllData}
              />
            )}

            {currentTab === 'goals' && (
              <GoalsView
                goals={goals}
                onRefresh={refreshAllData}
              />
            )}

            {currentTab === 'reports' && <ReportsView />}

            {currentTab === 'ai' && <AiAdvisorView />}
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="footer-bar">
        <span>© 2026 Personal Finance Manager — TypeScript & Spring Boot</span>
      </footer>

      {/* Modal Đăng nhập / Đăng ký */}
      <AuthModal
        isOpen={isAuthOpen}
        onClose={() => setIsAuthOpen(false)}
        onSuccess={handleAuthSuccess}
      />
    </div>
  );
};

export default App;
