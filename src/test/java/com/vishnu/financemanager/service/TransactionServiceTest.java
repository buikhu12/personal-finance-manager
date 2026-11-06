package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.TransactionRequest;
import com.vishnu.financemanager.dto.TransactionResponse;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @InjectMocks
    private TransactionService service;

    @Mock
    private TransactionRepository txRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private CategoryRepository categoryRepo;

    private Principal principal;
    private User user;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        principal = () -> "test@example.com";
        user = User.builder().id(1L).username("test@example.com").build();
        when(userRepo.findByUsername("test@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void create_validTransaction_withDefaultCategory_returnsSaved() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("100.00"));
        req.setDate(LocalDate.now().toString());
        req.setCategory("Food");
        req.setDescription("Groceries");

        Category defaultCategory = Category.builder()
                .id(2L)
                .name("Food")
                .type(CategoryType.EXPENSE)
                .isCustom(false)
                .user(null)
                .build();
        when(categoryRepo.findByNameAndUserOrDefault("Food", user)).thenReturn(Optional.of(defaultCategory));

        Transaction saved = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .category(defaultCategory)
                .user(user)
                .build();
        when(txRepo.save(any())).thenReturn(saved);

        TransactionResponse response = service.create(req, principal);

        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals("Food", response.getCategory());
        assertEquals("EXPENSE", response.getType());
        verify(txRepo).save(any());
    }

    @Test
    void create_futureDate_throwsException() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("100.00"));
        req.setDate(LocalDate.now().plusDays(1).toString());
        req.setCategory("Food");

        assertThrows(IllegalArgumentException.class, () -> service.create(req, principal));
    }

    @Test
    void update_validTransaction_updatesDateAndAmount() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("250.00"));
        LocalDate pastDate = LocalDate.now().minusDays(2);
        req.setDate(pastDate.toString());
        req.setCategory("Salary");
        req.setDescription("Updated Salary");

        Category category = Category.builder().id(1L).name("Salary").type(CategoryType.INCOME).isCustom(false).build();
        when(categoryRepo.findByNameAndUserOrDefault("Salary", user)).thenReturn(Optional.of(category));

        Transaction existingTx = Transaction.builder()
                .id(10L)
                .amount(new BigDecimal("200.00"))
                .date(LocalDate.now().minusDays(5))
                .category(category)
                .user(user)
                .build();

        when(txRepo.findById(10L)).thenReturn(Optional.of(existingTx));
        when(txRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.update(10L, req, principal);

        assertEquals(new BigDecimal("250.00"), response.getAmount());
        assertEquals(pastDate.toString(), response.getDate());
        assertEquals("Updated Salary", response.getDescription());
    }

    @Test
    void update_futureDate_throwsException() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("100.00"));
        req.setDate(LocalDate.now().plusDays(2).toString());
        req.setCategory("Salary");

        Transaction existingTx = Transaction.builder().id(10L).user(user).build();
        when(txRepo.findById(10L)).thenReturn(Optional.of(existingTx));

        assertThrows(IllegalArgumentException.class, () -> service.update(10L, req, principal));
    }
}
