package com.vishnu.financemanager.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private BigDecimal amount;
    private String date;
    private String category;
    private String description;
    private String type; // INCOME or EXPENSE
}
