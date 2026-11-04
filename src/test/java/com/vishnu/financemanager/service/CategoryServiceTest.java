package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.CategoryRequest;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.security.Principal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    @InjectMocks
    private CategoryService service;

    @Mock
    private UserRepository userRepo;

    @Mock
    private CategoryRepository categoryRepo;

    @Mock
    private TransactionRepository transactionRepo;

    private User user;
    private Principal principal;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        principal = () -> "test@example.com";
        user = User.builder().id(1L).username("test@example.com").build();
        when(userRepo.findByUsername("test@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void create_validCustomCategory_returnsCategory() {
        CategoryRequest request = new CategoryRequest();
        request.setName("SideBusiness");
        request.setType("INCOME");

        when(categoryRepo.existsByNameAndUser("SideBusiness", user)).thenReturn(false);

        Category saved = Category.builder().id(1L).name("SideBusiness").type(CategoryType.INCOME).user(user).isCustom(true).build();
        when(categoryRepo.save(any())).thenReturn(saved);

        var result = service.create(request, principal);
        assertEquals("SideBusiness", result.getName());
    }
}
