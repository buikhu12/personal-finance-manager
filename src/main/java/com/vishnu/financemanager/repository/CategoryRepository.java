package com.vishnu.financemanager.repository;

import com.vishnu.financemanager.model.Category;
import com.vishnu.financemanager.model.CategoryType;
import com.vishnu.financemanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser(User user);
    List<Category> findByUserOrIsCustomFalse(User user);
    Optional<Category> findByNameAndUser(String name, User user);
    boolean existsByNameAndUser(String name, User user);
}
