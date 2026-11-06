package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.CategoryRequest;
import com.vishnu.financemanager.dto.CategoryResponse;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final UserRepository userRepo;
    private final CategoryRepository categoryRepo;
    private final TransactionRepository transactionRepo;

    public List<CategoryResponse> getAllCategories(Principal principal) {
        User user = getUser(principal);
        return categoryRepo.findByUserOrIsCustomFalse(user)
                .stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    public CategoryResponse create(CategoryRequest request, Principal principal) {
        User user = getUser(principal);

        if (categoryRepo.existsByNameAndUser(request.getName(), user)
                || categoryRepo.existsByNameIgnoreCaseAndIsCustomFalse(request.getName())) {
            throw new IllegalArgumentException("Category already exists");
        }

        CategoryType type = CategoryType.valueOf(request.getType().toUpperCase());

        Category saved = categoryRepo.save(Category.builder()
                .name(request.getName())
                .type(type)
                .user(user)
                .isCustom(true)
                .build());

        return map(saved);
    }

    public void delete(String name, Principal principal) {
        User user = getUser(principal);
        Category category = categoryRepo.findByNameAndUser(name, user)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!category.isCustom()) {
            throw new IllegalArgumentException("Default categories cannot be deleted");
        }

        boolean inUse = transactionRepo.findByUser(user).stream()
                .anyMatch(tx -> tx.getCategory().getId().equals(category.getId()));

        if (inUse) {
            throw new IllegalStateException("Cannot delete category that is in use");
        }

        categoryRepo.delete(category);
    }

    private CategoryResponse map(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType().name())
                .isCustom(category.isCustom())
                .build();
    }

    private User getUser(Principal principal) {
        return userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
