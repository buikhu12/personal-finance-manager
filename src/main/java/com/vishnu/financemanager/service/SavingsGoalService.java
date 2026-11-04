package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.*;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepo;
    private final UserRepository userRepo;
    private final TransactionRepository transactionRepo;

    public GoalResponse createGoal(GoalRequest request, Principal principal) {
        User user = getUser(principal);
        LocalDate start = request.getStartDate() != null
                ? LocalDate.parse(request.getStartDate())
                : LocalDate.now();

        SavingsGoal goal = SavingsGoal.builder()
                .goalName(request.getGoalName())
                .targetAmount(request.getTargetAmount())
                .targetDate(LocalDate.parse(request.getTargetDate()))
                .startDate(start)
                .user(user)
                .build();

        return map(goalRepo.save(goal), user);
    }

    public List<GoalResponse> getAll(Principal principal) {
        User user = getUser(principal);
        return goalRepo.findByUser(user).stream()
                .map(goal -> map(goal, user)).collect(Collectors.toList());
    }

    public GoalResponse getOne(Long id, Principal principal) {
        User user = getUser(principal);
        SavingsGoal goal = goalRepo.findById(id)
                .filter(g -> g.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        return map(goal, user);
    }

    public GoalResponse update(Long id, GoalRequest request, Principal principal) {
        User user = getUser(principal);
        SavingsGoal goal = goalRepo.findById(id)
                .filter(g -> g.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));

        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(LocalDate.parse(request.getTargetDate()));

        return map(goalRepo.save(goal), user);
    }

    public void delete(Long id, Principal principal) {
        User user = getUser(principal);
        SavingsGoal goal = goalRepo.findById(id)
                .filter(g -> g.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        goalRepo.delete(goal);
    }

    private GoalResponse map(SavingsGoal goal, User user) {
        List<Transaction> transactions = transactionRepo.findByUserAndDateBetween(
                user, goal.getStartDate(), LocalDate.now());

        double income = transactions.stream()
                .filter(t -> t.getCategory().getType() == CategoryType.INCOME)
                .mapToDouble(Transaction::getAmount).sum();

        double expense = transactions.stream()
                .filter(t -> t.getCategory().getType() == CategoryType.EXPENSE)
                .mapToDouble(Transaction::getAmount).sum();

        double progress = income - expense;
        double percentage = (progress / goal.getTargetAmount()) * 100;
        double remaining = goal.getTargetAmount() - progress;

        return GoalResponse.builder()
                .id(goal.getId())
                .goalName(goal.getGoalName())
                .targetAmount(goal.getTargetAmount())
                .targetDate(goal.getTargetDate().toString())
                .startDate(goal.getStartDate().toString())
                .currentProgress(progress)
                .progressPercentage(Math.max(0, Math.min(percentage, 100)))
                .remainingAmount(Math.max(0, remaining))
                .build();
    }

    private User getUser(Principal principal) {
        return userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
