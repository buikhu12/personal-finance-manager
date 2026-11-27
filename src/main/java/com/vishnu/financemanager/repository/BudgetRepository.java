package com.vishnu.financemanager.repository;

import com.vishnu.financemanager.model.Budget;
import com.vishnu.financemanager.model.Category;
import com.vishnu.financemanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserAndMonthStartOrderByCategory_NameAsc(User user, LocalDate monthStart);
    List<Budget> findByUserAndMonthStartBetweenOrderByMonthStartAscCategory_NameAsc(User user, LocalDate startDate, LocalDate endDate);
    Optional<Budget> findByIdAndUser(Long id, User user);
    Optional<Budget> findByUserAndCategoryAndMonthStart(User user, Category category, LocalDate monthStart);
    boolean existsByCategory(Category category);
}
