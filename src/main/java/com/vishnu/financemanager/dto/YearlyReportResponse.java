package com.vishnu.financemanager.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class YearlyReportResponse {
    private int year;
    private Map<String, Double> totalIncome;
    private Map<String, Double> totalExpenses;
    private double netSavings;
}
