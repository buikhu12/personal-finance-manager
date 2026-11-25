package com.vishnu.financemanager.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotNull(message = "Vui lòng chọn danh mục chi.")
        @Positive(message = "Danh mục chi không hợp lệ.")
        Long categoryId,

        @NotBlank(message = "Vui lòng chọn tháng.")
        String month,

        @NotNull(message = "Vui lòng nhập hạn mức.")
        @Positive(message = "Hạn mức phải lớn hơn 0.")
        @Digits(integer = 17, fraction = 2, message = "Hạn mức tối đa 17 chữ số và 2 số thập phân.")
        BigDecimal limitAmount
) {}
