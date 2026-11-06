package com.vishnu.financemanager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String date;  // YYYY-MM-DD

    @NotBlank
    private String category;

    private String description;
}
