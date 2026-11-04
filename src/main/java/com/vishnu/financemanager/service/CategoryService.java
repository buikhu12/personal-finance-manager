package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.CategoryRequest;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final UserRepository userRepo;
    private final CategoryRepository categoryRepo;
    private final TransactionRepository transactionRepo;

    public List<Category> getAllCategories(Principal principal) {
        User user = getUser(principal);
        return categoryRepo.findByUserOrIsCustomFalse(user);
    }

    public Category create(CategoryRequest request, Principal principal) {
        User user = getUser(principal);

        if (categoryRepo.existsByNameAndUser(request.getName(), user)) {
            throw new IllegalArgumentException("Category already exists");
        }

        CategoryType type = CategoryType.valueOf(request.getType().toUpperCase());

        return categoryRepo.save(Category.builder()
                .name(request.getName())
                .type(type)
                .user(user)
                .isCustom(true)
                .build());
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

    private User getUser(Principal principal) {
        return userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}