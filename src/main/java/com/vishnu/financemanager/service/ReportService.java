package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.*;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

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

        Map<String, Double> incomeMap = new HashMap<>();
        Map<String, Double> expenseMap = new HashMap<>();
        double incomeTotal = 0, expenseTotal = 0;

        for (Transaction tx : txs) {
            String cat = tx.getCategory().getName();
            if (tx.getCategory().getType() == CategoryType.INCOME) {
                incomeMap.put(cat, incomeMap.getOrDefault(cat, 0.0) + tx.getAmount());
                incomeTotal += tx.getAmount();
            } else {
                expenseMap.put(cat, expenseMap.getOrDefault(cat, 0.0) + tx.getAmount());
                expenseTotal += tx.getAmount();
            }
        }

        return MonthlyReportResponse.builder()
                .month(month)
                .year(year)
                .totalIncome(incomeMap)
                .totalExpenses(expenseMap)
                .netSavings(incomeTotal - expenseTotal)
                .build();
    }

    public YearlyReportResponse getYearly(int year, Principal principal) {
        User user = getUser(principal);
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        List<Transaction> txs = txRepo.findByUserAndDateBetween(user, start, end);

        Map<String, Double> incomeMap = new HashMap<>();
        Map<String, Double> expenseMap = new HashMap<>();
        double incomeTotal = 0, expenseTotal = 0;

        for (Transaction tx : txs) {
            String cat = tx.getCategory().getName();
            if (tx.getCategory().getType() == CategoryType.INCOME) {
                incomeMap.put(cat, incomeMap.getOrDefault(cat, 0.0) + tx.getAmount());
                incomeTotal += tx.getAmount();
            } else {
                expenseMap.put(cat, expenseMap.getOrDefault(cat, 0.0) + tx.getAmount());
                expenseTotal += tx.getAmount();
            }
        }

        return YearlyReportResponse.builder()
                .year(year)
                .totalIncome(incomeMap)
                .totalExpenses(expenseMap)
                .netSavings(incomeTotal - expenseTotal)
                .build();
    }

    private User getUser(Principal principal) {
        return userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
