package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.*;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository txRepo;
    private final UserRepository userRepo;
    private final BudgetRepository budgetRepo;

    public MonthlyReportResponse getMonthly(int year, int month, Principal principal) {
        User user = getUser(principal);
        LocalDate start = YearMonth.of(year, month).atDay(1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<Transaction> txs = txRepo.findByUserAndDateBetween(user, start, end);

        Map<String, BigDecimal> incomeMap = new HashMap<>();
        Map<String, BigDecimal> expenseMap = new HashMap<>();
        BigDecimal incomeTotal = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;

        for (Transaction tx : txs) {
            String cat = tx.getCategory().getName();
            BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            if (tx.getCategory().getType() == CategoryType.INCOME) {
                incomeMap.put(cat, incomeMap.getOrDefault(cat, BigDecimal.ZERO).add(amount));
                incomeTotal = incomeTotal.add(amount);
            } else {
                expenseMap.put(cat, expenseMap.getOrDefault(cat, BigDecimal.ZERO).add(amount));
                expenseTotal = expenseTotal.add(amount);
            }
        }

        List<BudgetResponse> budgets = buildBudgetResponses(
                budgetRepo.findByUserAndMonthStartOrderByCategory_NameAsc(user, start),
                txs
        );
        BigDecimal totalBudgetLimit = sumBudgetLimits(budgets);
        BigDecimal totalBudgetSpent = sumBudgetSpent(budgets);

        return MonthlyReportResponse.builder()
                .month(month)
                .year(year)
                .totalIncome(incomeMap)
                .totalExpenses(expenseMap)
                .netSavings(incomeTotal.subtract(expenseTotal))
                .budgets(budgets)
                .totalBudgetLimit(totalBudgetLimit)
                .totalBudgetSpent(totalBudgetSpent)
                .totalBudgetRemaining(totalBudgetLimit.subtract(totalBudgetSpent))
                .build();
    }

    public YearlyReportResponse getYearly(int year, Principal principal) {
        User user = getUser(principal);
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        List<Transaction> txs = txRepo.findByUserAndDateBetween(user, start, end);

        Map<String, BigDecimal> incomeMap = new HashMap<>();
        Map<String, BigDecimal> expenseMap = new HashMap<>();
        BigDecimal incomeTotal = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;

        for (Transaction tx : txs) {
            String cat = tx.getCategory().getName();
            BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            if (tx.getCategory().getType() == CategoryType.INCOME) {
                incomeMap.put(cat, incomeMap.getOrDefault(cat, BigDecimal.ZERO).add(amount));
                incomeTotal = incomeTotal.add(amount);
            } else {
                expenseMap.put(cat, expenseMap.getOrDefault(cat, BigDecimal.ZERO).add(amount));
                expenseTotal = expenseTotal.add(amount);
            }
        }

        List<BudgetResponse> budgets = buildBudgetResponses(
                budgetRepo.findByUserAndMonthStartBetweenOrderByMonthStartAscCategory_NameAsc(
                        user, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 1)),
                txs
        );
        BigDecimal totalBudgetLimit = sumBudgetLimits(budgets);
        BigDecimal totalBudgetSpent = sumBudgetSpent(budgets);

        return YearlyReportResponse.builder()
                .year(year)
                .totalIncome(incomeMap)
                .totalExpenses(expenseMap)
                .netSavings(incomeTotal.subtract(expenseTotal))
                .budgets(budgets)
                .totalBudgetLimit(totalBudgetLimit)
                .totalBudgetSpent(totalBudgetSpent)
                .totalBudgetRemaining(totalBudgetLimit.subtract(totalBudgetSpent))
                .build();
    }

    private List<BudgetResponse> buildBudgetResponses(List<Budget> budgets, List<Transaction> txs) {
        if (budgets == null || budgets.isEmpty()) {
            return List.of();
        }
        Map<String, BigDecimal> spentByMonthAndCategory = new HashMap<>();
        for (Transaction tx : txs) {
            Category category = tx.getCategory();
            if (category != null && category.getId() != null
                    && category.getType() == CategoryType.EXPENSE
                    && tx.getDate() != null) {
                String key = YearMonth.from(tx.getDate()) + ":" + category.getId();
                BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                spentByMonthAndCategory.merge(key, amount, BigDecimal::add);
            }
        }
        return budgets.stream()
                .map(budget -> {
                    String key = YearMonth.from(budget.getMonthStart()) + ":" + budget.getCategory().getId();
                    BigDecimal spent = spentByMonthAndCategory.getOrDefault(key, BigDecimal.ZERO);
                    return BudgetService.toBudgetResponse(budget, spent);
                })
                .toList();
    }

    private BigDecimal sumBudgetLimits(List<BudgetResponse> budgets) {
        return budgets.stream()
                .map(BudgetResponse::limitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumBudgetSpent(List<BudgetResponse> budgets) {
        return budgets.stream()
                .map(BudgetResponse::spentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private User getUser(Principal principal) {
        return userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
