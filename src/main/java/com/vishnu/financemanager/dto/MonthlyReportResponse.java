package com.vishnu.financemanager.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class MonthlyReportResponse {
    private int month;
    private int year;
    private Map<String, Double> totalIncome;
    private Map<String, Double> totalExpenses;
    private double netSavings;
}
