package com.vishnu.financemanager.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GoalResponse {
    private Long id;
    private String goalName;
    private BigDecimal targetAmount;
    private String targetDate;
    private String startDate;
    private BigDecimal currentProgress;
    private BigDecimal progressPercentage;
    private BigDecimal remainingAmount;
}
