package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.TransactionRequest;
import com.vishnu.financemanager.dto.TransactionResponse;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepo;
    private final UserRepository userRepo;
    private final CategoryRepository categoryRepo;
    private final WalletRepository walletRepo;

    public TransactionResponse create(TransactionRequest request, Principal principal) {
        User user = getUser(principal);

        LocalDate date = LocalDate.parse(request.getDate());
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date cannot be in the future");
        }

        Category category = categoryRepo.findByNameAndUserOrDefault(request.getCategory(), user)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category"));

        Wallet wallet = resolveWallet(request.getWalletId(), user);
        validateSufficientBalance(wallet, category, request.getAmount(), user, null);

        Transaction tx = Transaction.builder()
                .amount(request.getAmount())
                .date(date)
                .category(category)
                .description(request.getDescription())
                .user(user)
                .wallet(wallet)
                .build();

        return map(transactionRepo.save(tx));
    }

    public List<TransactionResponse> getAll(Principal principal) {
        User user = getUser(principal);
        return transactionRepo.findByUser(user)
                .stream().map(this::map).collect(Collectors.toList());
    }

    public TransactionResponse update(Long id, TransactionRequest request, Principal principal) {
        User user = getUser(principal);
        Transaction tx = transactionRepo.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        LocalDate date = LocalDate.parse(request.getDate());
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date cannot be in the future");
        }

        Category category = categoryRepo.findByNameAndUserOrDefault(request.getCategory(), user)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category"));

        Wallet wallet = request.getWalletId() == null
                ? tx.getWallet()
                : resolveWallet(request.getWalletId(), user);
        if (wallet == null) {
            wallet = getOrCreateDefaultWallet(user);
        }

        validateSufficientBalance(wallet, category, request.getAmount(), user, tx.getId());

        tx.setAmount(request.getAmount());
        tx.setDate(date);
        tx.setDescription(request.getDescription());
        tx.setCategory(category);
        tx.setWallet(wallet);

        return map(transactionRepo.save(tx));
    }

    private void validateSufficientBalance(
            Wallet wallet,
            Category category,
            BigDecimal amount,
            User user,
            Long excludeTransactionId
    ) {
        if (category.getType() != CategoryType.EXPENSE) {
            return;
        }
        List<Transaction> userTransactions = transactionRepo.findByUser(user);
        BigDecimal availableBalance = WalletService.calculateWalletBalance(wallet, userTransactions, excludeTransactionId);
        if (amount.compareTo(availableBalance) > 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Số dư ví \"%s\" không đủ để thực hiện giao dịch chi tiêu (Hiện có: %s, cần chi: %s)",
                            wallet.getName(),
                            availableBalance.stripTrailingZeros().toPlainString(),
                            amount.stripTrailingZeros().toPlainString()
                    )
            );
        }
    }

    public void delete(Long id, Principal principal) {
        User user = getUser(principal);
        Transaction tx = transactionRepo.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        transactionRepo.delete(tx);
    }

    private TransactionResponse map(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .date(t.getDate().toString())
                .category(t.getCategory().getName())
                .walletId(t.getWallet() != null ? t.getWallet().getId() : null)
                .walletName(t.getWallet() != null ? t.getWallet().getName() : null)
                .description(t.getDescription())
                .type(t.getCategory().getType().name())
                .build();
    }

    private User getUser(Principal principal) {
        return userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Wallet resolveWallet(Long walletId, User user) {
        if (walletId == null) {
            return getOrCreateDefaultWallet(user);
        }
        return walletRepo.findByIdAndUser(walletId, user)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    }

    private Wallet getOrCreateDefaultWallet(User user) {
        return walletRepo.findByNameIgnoreCaseAndUser(WalletService.DEFAULT_WALLET_NAME, user)
                .orElseGet(() -> walletRepo.save(Wallet.builder()
                        .name(WalletService.DEFAULT_WALLET_NAME)
                        .initialBalance(java.math.BigDecimal.ZERO)
                        .user(user)
                        .build()));
    }
}
