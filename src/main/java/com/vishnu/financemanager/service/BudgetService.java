package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.BudgetRequest;
import com.vishnu.financemanager.dto.BudgetResponse;
import com.vishnu.financemanager.dto.BudgetResponse.Status;
import com.vishnu.financemanager.model.Budget;
import com.vishnu.financemanager.model.Category;
import com.vishnu.financemanager.model.CategoryType;
import com.vishnu.financemanager.model.Transaction;
import com.vishnu.financemanager.model.User;
import com.vishnu.financemanager.repository.BudgetRepository;
import com.vishnu.financemanager.repository.CategoryRepository;
import com.vishnu.financemanager.repository.TransactionRepository;
import com.vishnu.financemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final BigDecimal WARNING_RATIO = new BigDecimal("0.80");
    private static final String DUPLICATE_BUDGET = "Danh mục này đã có ngân sách trong tháng.";

    private final BudgetRepository budgetRepo;
    private final CategoryRepository categoryRepo;
    private final TransactionRepository transactionRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAll(String month, Principal principal) {
        User user = getUser(principal);
        YearMonth period = parseMonth(month);
        List<Budget> budgets = budgetRepo.findByUserAndMonthStartOrderByCategory_NameAsc(user, period.atDay(1));
        if (budgets.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> spending = spendingByCategory(user, period);
        return budgets.stream().map(budget -> toResponse(budget, spending)).toList();
    }

    @Transactional
    public BudgetResponse create(BudgetRequest request, Principal principal) {
        User user = getUser(principal);
        return save(Budget.builder().user(user).build(), request, user);
    }

    @Transactional
    public BudgetResponse update(Long id, BudgetRequest request, Principal principal) {
        User user = getUser(principal);
        return save(findOwnedBudget(id, user), request, user);
    }

    @Transactional
    public void delete(Long id, Principal principal) {
        budgetRepo.delete(findOwnedBudget(id, getUser(principal)));
    }

    private BudgetResponse save(Budget budget, BudgetRequest request, User user) {
        YearMonth period = parseMonth(request.month());
        Category category = categoryRepo.findByUserOrIsCustomFalse(user).stream()
                .filter(candidate -> candidate.getId().equals(request.categoryId()))
                .filter(candidate -> candidate.getType() == CategoryType.EXPENSE)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn danh mục chi hợp lệ."));

        boolean duplicate = budgetRepo.findByUserAndCategoryAndMonthStart(user, category, period.atDay(1))
                .filter(existing -> !existing.getId().equals(budget.getId()))
                .isPresent();
        if (duplicate) {
            throw new IllegalStateException(DUPLICATE_BUDGET);
        }

        budget.setCategory(category);
        budget.setMonthStart(period.atDay(1));
        budget.setLimitAmount(request.limitAmount());
        try {
            budgetRepo.saveAndFlush(budget);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Không thể lưu ngân sách. Danh mục có thể đã thay đổi hoặc đã có ngân sách trong tháng.", ex);
        }
        return toResponse(budget, spendingByCategory(user, period));
    }

    private Map<Long, BigDecimal> spendingByCategory(User user, YearMonth period) {
        Map<Long, BigDecimal> spending = new HashMap<>();
        for (Transaction transaction : transactionRepo.findByUserAndDateBetween(user, period.atDay(1), period.atEndOfMonth())) {
            Category category = transaction.getCategory();
            if (category != null && category.getType() == CategoryType.EXPENSE) {
                spending.merge(category.getId(), transaction.getAmount(), BigDecimal::add);
            }
        }
        return spending;
    }

    private BudgetResponse toResponse(Budget budget, Map<Long, BigDecimal> spending) {
        BigDecimal spent = spending.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
        return toBudgetResponse(budget, spent);
    }

    public static BudgetResponse toBudgetResponse(Budget budget, BigDecimal spent) {
        Category category = budget.getCategory();
        BigDecimal limit = budget.getLimitAmount();
        BigDecimal percentage = limit.compareTo(BigDecimal.ZERO) > 0
                ? spent.movePointRight(2).divide(limit, 1, RoundingMode.DOWN)
                : BigDecimal.ZERO;
        return new BudgetResponse(budget.getId(), category.getId(), category.getName(),
                YearMonth.from(budget.getMonthStart()).toString(), limit, spent, limit.subtract(spent),
                percentage, statusFor(spent, limit));
    }

    public static Status statusFor(BigDecimal spent, BigDecimal limit) {
        int comparison = spent.compareTo(limit);
        if (comparison > 0) return Status.EXCEEDED;
        if (comparison == 0) return Status.AT_LIMIT;
        if (spent.compareTo(limit.multiply(WARNING_RATIO)) >= 0) return Status.NEAR_LIMIT;
        return Status.SAFE;
    }

    private YearMonth parseMonth(String month) {
        if (!month.matches("[0-9]{4}-(0[1-9]|1[0-2])") || month.startsWith("0000")) {
            throw new IllegalArgumentException("Tháng phải có dạng YYYY-MM.");
        }
        return YearMonth.parse(month);
    }

    private Budget findOwnedBudget(Long id, User user) {
        return budgetRepo.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ngân sách."));
    }

    private User getUser(Principal principal) {
        return userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
