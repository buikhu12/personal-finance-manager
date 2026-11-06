package com.vishnu.financemanager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoalRequest {
    @NotBlank
    private String goalName;

    @NotNull
    @Positive
    private BigDecimal targetAmount;

    @PositiveOrZero
    private BigDecimal currentAmount;

    @NotBlank
    private String targetDate;

    private String startDate; // optional
}
