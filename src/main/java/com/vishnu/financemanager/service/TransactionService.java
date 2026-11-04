package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.TransactionRequest;
import com.vishnu.financemanager.dto.TransactionResponse;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public TransactionResponse create(TransactionRequest request, Principal principal) {
        User user = getUser(principal);

        LocalDate date = LocalDate.parse(request.getDate());
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date cannot be in the future");
        }

        Category category = categoryRepo.findByNameAndUser(request.getCategory(), user)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category"));

        Transaction tx = Transaction.builder()
                .amount(request.getAmount())
                .date(date)
                .category(category)
                .description(request.getDescription())
                .user(user)
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

        tx.setAmount(request.getAmount());
        tx.setDescription(request.getDescription());
        tx.setCategory(categoryRepo.findByNameAndUser(request.getCategory(), user)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category")));

        return map(transactionRepo.save(tx));
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
                .description(t.getDescription())
                .type(t.getCategory().getType().name())
                .build();
    }

    private User getUser(Principal principal) {
        return userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
