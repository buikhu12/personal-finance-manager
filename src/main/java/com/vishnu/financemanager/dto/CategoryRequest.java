package com.vishnu.financemanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String type; // INCOME or EXPENSE
}
