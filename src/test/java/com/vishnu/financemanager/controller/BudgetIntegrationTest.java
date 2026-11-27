package com.vishnu.financemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vishnu.financemanager.model.Category;
import com.vishnu.financemanager.model.CategoryType;
import com.vishnu.financemanager.model.User;
import com.vishnu.financemanager.model.Wallet;
import com.vishnu.financemanager.repository.CategoryRepository;
import com.vishnu.financemanager.repository.UserRepository;
import com.vishnu.financemanager.repository.WalletRepository;
import com.vishnu.financemanager.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:budget-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class BudgetIntegrationTest {

    private static final String MONTH = "2024-03";
    private static final String BUDGETS = "/api/budgets";
    private static final String TRANSACTIONS = "/api/transactions";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UserRepository users;
    @Autowired
    private CategoryRepository categories;
    @Autowired
    private WalletRepository wallets;

    @Test
    void recalculatesBudgetFromCurrentTransactionsInTheSelectedMonth() throws Exception {
        User owner = createUser("budget-owner@example.com");
        User other = createUser("budget-other@example.com");
        Category food = categories.findByNameAndUserOrDefault("Food", owner).orElseThrow();
        createBudget(owner, food, MONTH, 1_000_000).andExpect(status().isCreated());

        addTransaction(other, "Food", "2024-03-01", 400_000);
        addTransaction(owner, "Food", "2024-02-29", 400_000);
        addTransaction(owner, "Food", "2024-04-01", 400_000);
        addTransaction(owner, "Salary", "2024-03-01", 400_000);
        addTransaction(owner, "Rent", "2024-03-01", 400_000);
        assertBudget(owner, 0, 1_000_000, 0, "SAFE");

        addTransaction(owner, "Food", "2024-03-01", 600_000);
        assertBudget(owner, 600_000, 400_000, 60, "SAFE");

        long extra = addTransaction(owner, "Food", "2024-03-31", 200_000);
        assertBudget(owner, 800_000, 200_000, 80, "NEAR_LIMIT");

        changeTransaction(owner, extra, 400_000);
        assertBudget(owner, 1_000_000, 0, 100, "AT_LIMIT");

        changeTransaction(owner, extra, 450_000);
        assertBudget(owner, 1_050_000, -50_000, 105, "EXCEEDED");

        mvc.perform(delete(TRANSACTIONS + "/" + extra).with(user(owner.getUsername())))
                .andExpect(status().isOk());
        assertBudget(owner, 600_000, 400_000, 60, "SAFE");
    }

    @Test
    void validatesTheLimitAndCategoryAndRestrictsChangesToTheOwner() throws Exception {
        User owner = createUser("budget-editor@example.com");
        User other = createUser("budget-viewer@example.com");
        Category expense = categories.save(Category.builder().name("Books").type(CategoryType.EXPENSE)
                .isCustom(true).user(owner).build());
        Category salary = categories.findByNameAndUserOrDefault("Salary", owner).orElseThrow();

        createBudget(owner, expense, "2024-13", 100).andExpect(status().isBadRequest());
        createBudget(owner, expense, MONTH, 0).andExpect(status().isBadRequest());
        createBudget(owner, salary, MONTH, 100).andExpect(status().isBadRequest());
        createBudget(other, expense, MONTH, 100).andExpect(status().isBadRequest());

        long id = responseId(createBudget(owner, expense, MONTH, 100).andExpect(status().isCreated()));
        String path = BUDGETS + "/" + id;
        createBudget(owner, expense, MONTH, 200).andExpect(status().isConflict());
        createBudget(owner, expense, "2024-04", 200).andExpect(status().isCreated());

        mvc.perform(get(BUDGETS).param("month", MONTH).with(user(other.getUsername())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        String update = budgetPayload(expense, MONTH, 250);
        mvc.perform(put(path).with(user(other.getUsername())).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(path).with(user(other.getUsername()))).andExpect(status().isBadRequest());
        mvc.perform(put(path).with(user(owner.getUsername())).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.limitAmount").value(250));

        mvc.perform(delete("/api/categories/{name}", expense.getName()).with(user(owner.getUsername())))
                .andExpect(status().isConflict());
        mvc.perform(delete(path).with(user(owner.getUsername()))).andExpect(status().isNoContent());
        mvc.perform(get(BUDGETS).param("month", MONTH).with(user(owner.getUsername())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    private void assertBudget(User owner, int spent, int remaining, int usage, String state) throws Exception {
        mvc.perform(get(BUDGETS).param("month", MONTH).with(user(owner.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spentAmount").value(spent))
                .andExpect(jsonPath("$[0].remainingAmount").value(remaining))
                .andExpect(jsonPath("$[0].usagePercentage").value((double) usage))
                .andExpect(jsonPath("$[0].status").value(state));
    }

    private ResultActions createBudget(User owner, Category category, String month, int limit) throws Exception {
        return mvc.perform(post(BUDGETS).with(user(owner.getUsername()))
                .contentType(MediaType.APPLICATION_JSON).content(budgetPayload(category, month, limit)));
    }

    private String budgetPayload(Category category, String month, int limit) {
        return """
                {"categoryId":%d,"month":"%s","limitAmount":"%d"}
                """.formatted(category.getId(), month, limit);
    }

    private long addTransaction(User owner, String category, String date, int amount) throws Exception {
        return responseId(mvc.perform(post(TRANSACTIONS).with(user(owner.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON).content(transactionPayload(category, date, amount)))
                .andExpect(status().isCreated()));
    }

    private void changeTransaction(User owner, long id, int amount) throws Exception {
        mvc.perform(put(TRANSACTIONS + "/" + id).with(user(owner.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON).content(transactionPayload("Food", "2024-03-31", amount)))
                .andExpect(status().isOk());
    }

    private String transactionPayload(String category, String date, int amount) {
        return """
                {"category":"%s","date":"%s","amount":%d}
                """.formatted(category, date, amount);
    }

    private long responseId(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsByteArray()).path("id").asLong();
    }

    private User createUser(String username) {
        User saved = users.save(User.builder().username(username).password("unused")
                .fullName("Budget Test User").phoneNumber("0000000000").build());
        wallets.save(Wallet.builder()
                .name(WalletService.DEFAULT_WALLET_NAME)
                .initialBalance(new BigDecimal("10000000"))
                .user(saved)
                .build());
        return saved;
    }
}
