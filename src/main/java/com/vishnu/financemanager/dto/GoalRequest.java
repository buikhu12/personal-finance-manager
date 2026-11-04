package com.vishnu.financemanager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class GoalRequest {
    @NotBlank
    private String goalName;

    @NotNull
    @Positive
    private Double targetAmount;

    @NotBlank
    private String targetDate;

    private String startDate; // optional
}
