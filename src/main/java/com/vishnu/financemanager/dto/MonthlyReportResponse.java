package com.vishnu.financemanager.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class MonthlyReportResponse {
    private int month;
    private int year;
    private Map<String, BigDecimal> totalIncome;
    private Map<String, BigDecimal> totalExpenses;
    private BigDecimal netSavings;
    private List<BudgetResponse> budgets;
    private BigDecimal totalBudgetLimit;
    private BigDecimal totalBudgetSpent;
    private BigDecimal totalBudgetRemaining;
}
