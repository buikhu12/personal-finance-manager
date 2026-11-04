package com.vishnu.financemanager.repository;

import com.vishnu.financemanager.model.SavingsGoal;
import com.vishnu.financemanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUser(User user);
}
