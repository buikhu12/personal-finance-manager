package com.vishnu.financemanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletRequest {
    @NotBlank
    private String name;

    @NotNull
    @PositiveOrZero
    private BigDecimal initialBalance = BigDecimal.ZERO;
}
