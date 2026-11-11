package com.vishnu.financemanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WalletResponse {
    private Long id;
    private String name;
    @JsonProperty("isDefault")
    private boolean isDefault;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private long transactionCount;
    private String bankName;
    private String bankAccountLastFour;
}
