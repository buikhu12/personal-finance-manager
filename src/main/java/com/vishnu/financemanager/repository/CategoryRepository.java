package com.vishnu.financemanager.repository;

import com.vishnu.financemanager.model.Category;
import com.vishnu.financemanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser(User user);
    List<Category> findByUserOrIsCustomFalse(User user);
    Optional<Category> findByNameAndUser(String name, User user);
    boolean existsByNameAndUser(String name, User user);
    boolean existsByNameIgnoreCaseAndIsCustomFalse(String name);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND (c.user = :user OR c.isCustom = false) ORDER BY c.isCustom DESC")
    List<Category> findByNameAndUserOrDefaultList(@Param("name") String name, @Param("user") User user);

    default Optional<Category> findByNameAndUserOrDefault(String name, User user) {
        return findByNameAndUserOrDefaultList(name, user).stream().findFirst();
    }
}
