package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.TransactionRequest;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

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
    void create_validTransaction_returnsSaved() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(100.0);
        req.setDate(LocalDate.now().toString());
        req.setCategory("Salary");
        req.setDescription("Test");

        Category cat = Category.builder().id(1L).name("Salary").type(CategoryType.INCOME).user(user).build();
        when(categoryRepo.findByNameAndUser("Salary", user)).thenReturn(Optional.of(cat));

        Transaction saved = Transaction.builder().id(1L).amount(100.0).date(LocalDate.now()).category(cat).user(user).build();
        when(txRepo.save(any())).thenReturn(saved);

        var response = service.create(req, principal);

        assertEquals(100.0, response.getAmount());
        verify(txRepo).save(any());
    }
}
