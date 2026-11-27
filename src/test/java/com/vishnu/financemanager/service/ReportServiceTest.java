package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.BudgetResponse;
import com.vishnu.financemanager.dto.MonthlyReportResponse;
import com.vishnu.financemanager.dto.YearlyReportResponse;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.BudgetRepository;
import com.vishnu.financemanager.repository.TransactionRepository;
import com.vishnu.financemanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private TransactionRepository txRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private BudgetRepository budgetRepo;

    private User user;
    private Principal principal;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        principal = () -> "test@example.com";
        user = User.builder().id(1L).username("test@example.com").build();
        when(userRepo.findByUsername("test@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void getMonthly_calculatesNetSavingsWithBigDecimal() {
        Category salaryCat = Category.builder().name("Salary").type(CategoryType.INCOME).build();
        Category foodCat = Category.builder().name("Food").type(CategoryType.EXPENSE).build();

        Transaction incomeTx = Transaction.builder()
                .amount(new BigDecimal("3000.50"))
                .category(salaryCat)
                .date(LocalDate.of(2026, 5, 10))
                .user(user)
                .build();

        Transaction expenseTx = Transaction.builder()
                .amount(new BigDecimal("1200.25"))
                .category(foodCat)
                .date(LocalDate.of(2026, 5, 15))
                .user(user)
                .build();

        when(txRepo.findByUserAndDateBetween(eq(user), any(), any()))
                .thenReturn(List.of(incomeTx, expenseTx));

        MonthlyReportResponse report = reportService.getMonthly(2026, 5, principal);

        assertEquals(new BigDecimal("1800.25"), report.getNetSavings());
        assertEquals(new BigDecimal("3000.50"), report.getTotalIncome().get("Salary"));
        assertEquals(new BigDecimal("1200.25"), report.getTotalExpenses().get("Food"));
    }

    @Test
    void getYearly_calculatesNetSavingsWithBigDecimal() {
        Category salaryCat = Category.builder().name("Salary").type(CategoryType.INCOME).build();
        Category rentCat = Category.builder().name("Rent").type(CategoryType.EXPENSE).build();

        Transaction incomeTx = Transaction.builder()
                .amount(new BigDecimal("50000.00"))
                .category(salaryCat)
                .date(LocalDate.of(2026, 1, 10))
                .user(user)
                .build();

        Transaction expenseTx = Transaction.builder()
                .amount(new BigDecimal("15000.00"))
                .category(rentCat)
                .date(LocalDate.of(2026, 2, 10))
                .user(user)
                .build();

        when(txRepo.findByUserAndDateBetween(eq(user), any(), any()))
                .thenReturn(List.of(incomeTx, expenseTx));

        YearlyReportResponse report = reportService.getYearly(2026, principal);

        assertEquals(new BigDecimal("35000.00"), report.getNetSavings());
        assertEquals(new BigDecimal("50000.00"), report.getTotalIncome().get("Salary"));
        assertEquals(new BigDecimal("15000.00"), report.getTotalExpenses().get("Rent"));
    }

    @Test
    void getMonthly_includesBudgetBreakdownAndTotals() {
        Category foodCat = Category.builder().id(10L).name("Food").type(CategoryType.EXPENSE).build();
        Budget foodBudget = Budget.builder()
                .id(1L)
                .category(foodCat)
                .monthStart(LocalDate.of(2026, 5, 1))
                .limitAmount(new BigDecimal("2000.00"))
                .user(user)
                .build();
        Transaction expenseTx = Transaction.builder()
                .amount(new BigDecimal("1700.00"))
                .category(foodCat)
                .date(LocalDate.of(2026, 5, 15))
                .user(user)
                .build();

        when(txRepo.findByUserAndDateBetween(eq(user), any(), any()))
                .thenReturn(List.of(expenseTx));
        when(budgetRepo.findByUserAndMonthStartOrderByCategory_NameAsc(user, LocalDate.of(2026, 5, 1)))
                .thenReturn(List.of(foodBudget));

        MonthlyReportResponse report = reportService.getMonthly(2026, 5, principal);

        assertEquals(1, report.getBudgets().size());
        assertEquals(new BigDecimal("2000.00"), report.getTotalBudgetLimit());
        assertEquals(new BigDecimal("1700.00"), report.getTotalBudgetSpent());
        assertEquals(new BigDecimal("300.00"), report.getTotalBudgetRemaining());
        assertEquals(BudgetResponse.Status.NEAR_LIMIT, report.getBudgets().get(0).status());
    }
}
