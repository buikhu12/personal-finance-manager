package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.AiAdviceResponse;
import com.vishnu.financemanager.dto.GoalResponse;
import com.vishnu.financemanager.dto.MonthlyReportResponse;
import com.vishnu.financemanager.model.User;
import com.vishnu.financemanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AiServiceTest {

    @InjectMocks
    private AiService aiService;

    @Mock
    private ReportService reportService;

    @Mock
    private SavingsGoalService savingsGoalService;

    @Mock
    private UserRepository userRepo;

    private User user;
    private Principal principal;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        principal = () -> "test@example.com";
        user = User.builder().id(1L).username("test@example.com").fullName("Nguyễn Văn A").build();
        when(userRepo.findByUsername("test@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void getMonthlyAdvice_excellentSavingsRate_returnsExcellentStatus() {
        // Thu nhập: 20,000,000; Chi tiêu: 12,000,000; Tiết kiệm: 8,000,000 (Tỷ lệ 40% >= 20%)
        MonthlyReportResponse report = MonthlyReportResponse.builder()
                .month(11)
                .year(2026)
                .totalIncome(Map.of("Salary", new BigDecimal("20000000")))
                .totalExpenses(Map.of(
                        "Food", new BigDecimal("6000000"), // Chiếm 50% chi tiêu
                        "Rent", new BigDecimal("4000000"),
                        "Entertainment", new BigDecimal("2000000")
                ))
                .netSavings(new BigDecimal("8000000"))
                .build();

        GoalResponse goal = GoalResponse.builder()
                .id(1L)
                .goalName("Mua xe máy")
                .targetAmount(new BigDecimal("30000000"))
                .currentProgress(new BigDecimal("10000000"))
                .remainingAmount(new BigDecimal("20000000"))
                .startDate(LocalDate.now().toString())
                .targetDate(LocalDate.now().plusMonths(5).toString())
                .build();

        when(reportService.getMonthly(2026, 11, principal)).thenReturn(report);
        when(savingsGoalService.getAll(principal)).thenReturn(List.of(goal));

        AiAdviceResponse response = aiService.getMonthlyAdvice(2026, 11, principal);

        assertNotNull(response);
        assertEquals("EXCELLENT", response.getFinancialHealthStatus());
        assertEquals(new BigDecimal("40.00"), response.getSavingsRate());
        assertEquals("SMART_FINANCIAL_ENGINE", response.getSource());
        assertFalse(response.getInsights().isEmpty());
        assertFalse(response.getRecommendations().isEmpty());
        assertFalse(response.getGoalAssessments().isEmpty());
    }

    @Test
    void getMonthlyAdvice_overspending_returnsDangerStatus() {
        // Thu nhập: 10,000,000; Chi tiêu: 15,000,000; Tiết kiệm: -5,000,000
        MonthlyReportResponse report = MonthlyReportResponse.builder()
                .month(11)
                .year(2026)
                .totalIncome(Map.of("Salary", new BigDecimal("10000000")))
                .totalExpenses(Map.of("Shopping", new BigDecimal("15000000")))
                .netSavings(new BigDecimal("-5000000"))
                .build();

        when(reportService.getMonthly(2026, 11, principal)).thenReturn(report);
        when(savingsGoalService.getAll(principal)).thenReturn(List.of());

        AiAdviceResponse response = aiService.getMonthlyAdvice(2026, 11, principal);

        assertNotNull(response);
        assertEquals("DANGER", response.getFinancialHealthStatus());
        assertTrue(response.getSavingsRate().compareTo(BigDecimal.ZERO) < 0);
        assertTrue(response.getSummary().contains("BỘI CHI"));
    }

    @Test
    void getMonthlyAdvice_lowSavingsRate_returnsWarningStatus() {
        // Thu nhập: 20,000,000; Chi tiêu: 19,000,000; Tiết kiệm: 1,000,000 (Tỷ lệ 5% < 10%)
        MonthlyReportResponse report = MonthlyReportResponse.builder()
                .month(11)
                .year(2026)
                .totalIncome(Map.of("Salary", new BigDecimal("20000000")))
                .totalExpenses(Map.of("Living", new BigDecimal("19000000")))
                .netSavings(new BigDecimal("1000000"))
                .build();

        when(reportService.getMonthly(2026, 11, principal)).thenReturn(report);
        when(savingsGoalService.getAll(principal)).thenReturn(List.of());

        AiAdviceResponse response = aiService.getMonthlyAdvice(2026, 11, principal);

        assertNotNull(response);
        assertEquals("WARNING", response.getFinancialHealthStatus());
        assertEquals(new BigDecimal("5.00"), response.getSavingsRate());
    }
}
