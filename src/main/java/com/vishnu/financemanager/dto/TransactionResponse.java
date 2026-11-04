package com.vishnu.financemanager.dto;

import lombok.*;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private Double amount;
    private String date;
    private String category;
    private String description;
    private String type; // INCOME or EXPENSE
}
