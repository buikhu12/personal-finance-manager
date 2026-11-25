package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.WalletBankAccountRequest;
import com.vishnu.financemanager.dto.WalletRequest;
import com.vishnu.financemanager.dto.WalletResponse;
import com.vishnu.financemanager.model.CategoryType;
import com.vishnu.financemanager.model.Transaction;
import com.vishnu.financemanager.model.User;
import com.vishnu.financemanager.model.Wallet;
import com.vishnu.financemanager.repository.TransactionRepository;
import com.vishnu.financemanager.repository.UserRepository;
import com.vishnu.financemanager.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WalletService {

    public static final String DEFAULT_WALLET_NAME = "Ví mặc định";

    private final WalletRepository walletRepo;
    private final TransactionRepository transactionRepo;
    private final UserRepository userRepo;

    public List<WalletResponse> getAll(Principal principal) {
        User user = getUser(principal);
        getOrCreateDefaultWallet(user);

        List<Wallet> wallets = walletRepo.findByUserOrderByNameAsc(user);
        List<Transaction> transactions = transactionRepo.findByUser(user);
        Map<Long, List<Transaction>> transactionsByWallet = new HashMap<>();
        for (Transaction transaction : transactions) {
            if (transaction.getWallet() != null) {
                transactionsByWallet.computeIfAbsent(transaction.getWallet().getId(), ignored -> new java.util.ArrayList<>())
                        .add(transaction);
            }
        }

        return wallets.stream()
                .map(wallet -> map(wallet, transactionsByWallet.getOrDefault(wallet.getId(), List.of()),
                        legacyTransactionsForDefault(wallet, transactions)))
                .toList();
    }

    public WalletResponse create(WalletRequest request, Principal principal) {
        User user = getUser(principal);
        String name = request.getName().trim();
        if (walletRepo.existsByNameIgnoreCaseAndUser(name, user)) {
            throw new IllegalArgumentException("A wallet with this name already exists");
        }

        Wallet wallet = Wallet.builder()
                .name(name)
                .initialBalance(request.getInitialBalance())
                .user(user)
                .build();
        return map(walletRepo.save(wallet), List.of(), List.of());
    }

    @Transactional
    public WalletResponse update(Long id, WalletRequest request, Principal principal) {
        User user = getUser(principal);
        Wallet wallet = findOwnedWallet(id, user);
        String name = request.getName().trim();
        if (DEFAULT_WALLET_NAME.equalsIgnoreCase(wallet.getName())
                && !DEFAULT_WALLET_NAME.equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("The default wallet name cannot be changed");
        }
        if (!wallet.getName().equalsIgnoreCase(name) && walletRepo.existsByNameIgnoreCaseAndUser(name, user)) {
            throw new IllegalArgumentException("A wallet with this name already exists");
        }

        wallet.setName(name);
        wallet.setInitialBalance(request.getInitialBalance());
        List<Transaction> transactions = transactionRepo.findByUser(user);
        return map(walletRepo.save(wallet),
                transactions.stream()
                        .filter(t -> t.getWallet() != null && wallet.getId().equals(t.getWallet().getId()))
                        .toList(),
                legacyTransactionsForDefault(wallet, transactions));
    }

    @Transactional
    public void linkBankAccount(Long id, WalletBankAccountRequest request, Principal principal) {
        updateBankAccount(id, getUser(principal), request.bankName().strip(), request.lastFour());
    }

    @Transactional
    public void unlinkBankAccount(Long id, Principal principal) {
        updateBankAccount(id, getUser(principal), null, null);
    }

    private void updateBankAccount(Long id, User user, String bankName, String lastFour) {
        if (walletRepo.updateBankAccount(id, user, bankName, lastFour) == 0) {
            throw new IllegalArgumentException("Không tìm thấy ví.");
        }
    }

    public void delete(Long id, Principal principal) {
        User user = getUser(principal);
        Wallet wallet = findOwnedWallet(id, user);
        if (DEFAULT_WALLET_NAME.equalsIgnoreCase(wallet.getName())) {
            throw new IllegalStateException("The default wallet cannot be deleted");
        }
        if (transactionRepo.existsByWallet(wallet)) {
            throw new IllegalStateException("Cannot delete a wallet that has transactions");
        }
        walletRepo.delete(wallet);
    }

    public Wallet getOrCreateDefaultWallet(User user) {
        return walletRepo.findByNameIgnoreCaseAndUser(DEFAULT_WALLET_NAME, user)
                .orElseGet(() -> walletRepo.save(Wallet.builder()
                        .name(DEFAULT_WALLET_NAME)
                        .initialBalance(BigDecimal.ZERO)
                        .user(user)
                        .build()));
    }

    private WalletResponse map(Wallet wallet, List<Transaction> transactions, List<Transaction> legacyTransactions) {
        BigDecimal currentBalance = wallet.getInitialBalance() == null ? BigDecimal.ZERO : wallet.getInitialBalance();
        for (Transaction transaction : transactions) {
            currentBalance = applyTransaction(currentBalance, transaction);
        }
        for (Transaction transaction : legacyTransactions) {
            currentBalance = applyTransaction(currentBalance, transaction);
        }

        return WalletResponse.builder()
                .id(wallet.getId())
                .name(wallet.getName())
                .isDefault(DEFAULT_WALLET_NAME.equalsIgnoreCase(wallet.getName()))
                .initialBalance(wallet.getInitialBalance())
                .currentBalance(currentBalance)
                .transactionCount(transactions.size() + legacyTransactions.size())
                .bankName(wallet.getBankName())
                .bankAccountLastFour(wallet.getBankAccountLastFour())
                .build();
    }

    /**
     * Calculates the current balance of a wallet from its initial balance and user transactions,
     * optionally excluding a specific transaction (used when validating transaction updates).
     */
    public static BigDecimal calculateWalletBalance(
            Wallet wallet,
            List<Transaction> userTransactions,
            Long excludeTransactionId
    ) {
        BigDecimal balance = wallet.getInitialBalance() == null ? BigDecimal.ZERO : wallet.getInitialBalance();
        boolean isDefault = DEFAULT_WALLET_NAME.equalsIgnoreCase(wallet.getName());
        for (Transaction transaction : userTransactions) {
            if (excludeTransactionId != null && excludeTransactionId.equals(transaction.getId())) {
                continue;
            }
            boolean belongsToWallet = transaction.getWallet() != null
                    ? wallet.getId() != null && wallet.getId().equals(transaction.getWallet().getId())
                    : isDefault;
            if (belongsToWallet) {
                balance = applyTransaction(balance, transaction);
            }
        }
        return balance;
    }

    private static BigDecimal applyTransaction(BigDecimal balance, Transaction transaction) {
        if (transaction.getCategory().getType() == CategoryType.INCOME) {
            return balance.add(transaction.getAmount());
        }
        return balance.subtract(transaction.getAmount());
    }

    private List<Transaction> legacyTransactionsForDefault(Wallet wallet, List<Transaction> transactions) {
        if (!DEFAULT_WALLET_NAME.equalsIgnoreCase(wallet.getName())) {
            return List.of();
        }
        return transactions.stream().filter(t -> t.getWallet() == null).toList();
    }

    private Wallet findOwnedWallet(Long id, User user) {
        return walletRepo.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    }

    private User getUser(Principal principal) {
        return userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
