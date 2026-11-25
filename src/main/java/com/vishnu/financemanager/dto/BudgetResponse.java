package com.vishnu.financemanager.dto;

import java.math.BigDecimal;

public record BudgetResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String month,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal usagePercentage,
        Status status
) {
    public enum Status {
        SAFE, NEAR_LIMIT, AT_LIMIT, EXCEEDED
    }
}
