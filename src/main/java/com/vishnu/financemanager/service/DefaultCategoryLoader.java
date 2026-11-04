package com.vishnu.financemanager.service;

import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultCategoryLoader {

    private final CategoryRepository categoryRepo;

    @PostConstruct
    public void loadDefaults() {
        List<String> defaultIncome = List.of("Salary");
        List<String> defaultExpense = List.of("Food", "Rent", "Transportation", "Entertainment", "Healthcare", "Utilities");

        defaultIncome.forEach(name -> addIfMissing(name, CategoryType.INCOME));
        defaultExpense.forEach(name -> addIfMissing(name, CategoryType.EXPENSE));
    }

    private void addIfMissing(String name, CategoryType type) {
        boolean exists = categoryRepo.findAll().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name) && !c.isCustom());
        if (!exists) {
            categoryRepo.save(Category.builder()
                    .name(name)
                    .type(type)
                    .isCustom(false)
                    .build());
        }
    }
}
