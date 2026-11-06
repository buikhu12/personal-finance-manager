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

        return MonthlyReportResponse.builder()
                .month(month)
                .year(year)
                .totalIncome(incomeMap)
                .totalExpenses(expenseMap)
                .netSavings(incomeTotal.subtract(expenseTotal))
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

        return YearlyReportResponse.builder()
                .year(year)
                .totalIncome(incomeMap)
                .totalExpenses(expenseMap)
                .netSavings(incomeTotal.subtract(expenseTotal))
                .build();
    }

    private User getUser(Principal principal) {
        return userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
