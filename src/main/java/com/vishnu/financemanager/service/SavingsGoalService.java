package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.*;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepo;
    private final UserRepository userRepo;

    public GoalResponse createGoal(GoalRequest request, Principal principal) {
        User user = getUser(principal);
        LocalDate start = request.getStartDate() != null
                ? LocalDate.parse(request.getStartDate())
                : LocalDate.now();
        LocalDate targetDate = LocalDate.parse(request.getTargetDate());

        if (!targetDate.isAfter(start)) {
            throw new IllegalArgumentException("Target date must be after start date");
        }
        if (targetDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Target date cannot be in the past");
        }

        BigDecimal initialAmount = request.getCurrentAmount() != null
                ? request.getCurrentAmount()
                : BigDecimal.ZERO;

        SavingsGoal goal = SavingsGoal.builder()
                .goalName(request.getGoalName())
                .targetAmount(request.getTargetAmount())
                .currentAmount(initialAmount)
                .targetDate(targetDate)
                .startDate(start)
                .user(user)
                .build();

        return map(goalRepo.save(goal));
    }

    public List<GoalResponse> getAll(Principal principal) {
        User user = getUser(principal);
        return goalRepo.findByUser(user).stream()
                .map(this::map).collect(Collectors.toList());
    }

    public GoalResponse getOne(Long id, Principal principal) {
        User user = getUser(principal);
        SavingsGoal goal = goalRepo.findById(id)
                .filter(g -> g.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        return map(goal);
    }

    public GoalResponse update(Long id, GoalRequest request, Principal principal) {
        User user = getUser(principal);
        SavingsGoal goal = goalRepo.findById(id)
                .filter(g -> g.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));

        LocalDate targetDate = LocalDate.parse(request.getTargetDate());
        if (!targetDate.isAfter(goal.getStartDate())) {
            throw new IllegalArgumentException("Target date must be after start date");
        }
        if (targetDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Target date cannot be in the past");
        }

        goal.setGoalName(request.getGoalName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(targetDate);
        if (request.getCurrentAmount() != null) {
            goal.setCurrentAmount(request.getCurrentAmount());
        }

        return map(goalRepo.save(goal));
    }

    public GoalResponse addSavings(Long id, BigDecimal amount, Principal principal) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Contribution amount must be positive");
        }
        User user = getUser(principal);
        SavingsGoal goal = goalRepo.findById(id)
                .filter(g -> g.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));

        BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        goal.setCurrentAmount(current.add(amount));

        return map(goalRepo.save(goal));
    }

    public void delete(Long id, Principal principal) {
        User user = getUser(principal);
        SavingsGoal goal = goalRepo.findById(id)
                .filter(g -> g.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        goalRepo.delete(goal);
    }

    private GoalResponse map(SavingsGoal goal) {
        BigDecimal target = goal.getTargetAmount();
        BigDecimal progress = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        BigDecimal remaining = target.subtract(progress).max(BigDecimal.ZERO);
        BigDecimal percentage = BigDecimal.ZERO;

        if (target.compareTo(BigDecimal.ZERO) > 0) {
            percentage = progress.divide(target, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                    .min(BigDecimal.valueOf(100))
                    .max(BigDecimal.ZERO);
        }

        return GoalResponse.builder()
                .id(goal.getId())
                .goalName(goal.getGoalName())
                .targetAmount(target)
                .targetDate(goal.getTargetDate().toString())
                .startDate(goal.getStartDate().toString())
                .currentProgress(progress)
                .progressPercentage(percentage)
                .remainingAmount(remaining)
                .build();
    }

    private User getUser(Principal principal) {
        return userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
