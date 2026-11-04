package com.vishnu.financemanager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TransactionRequest {
    @NotNull
    @Positive
    private Double amount;

    @NotNull
    private String date;  // YYYY-MM-DD

    @NotBlank
    private String category;

    private String description;
}
