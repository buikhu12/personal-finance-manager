package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.GoalRequest;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SavingsGoalServiceTest {

    @InjectMocks
    private SavingsGoalService service;

    @Mock
    private SavingsGoalRepository goalRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private TransactionRepository txRepo;

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
    void create_validGoal_returnsResponse() {
        GoalRequest request = new GoalRequest();
        request.setGoalName("Emergency Fund");
        request.setTargetAmount(5000.0);
        request.setTargetDate(LocalDate.now().plusMonths(3).toString());
        request.setStartDate(LocalDate.now().toString());

        SavingsGoal saved = SavingsGoal.builder()
                .id(1L)
                .goalName("Emergency Fund")
                .targetAmount(5000.0)
                .targetDate(LocalDate.now().plusMonths(3))
                .startDate(LocalDate.now())
                .user(user).build();

        when(goalRepo.save(any())).thenReturn(saved);

        var response = service.createGoal(request, principal);
        assertEquals("Emergency Fund", response.getGoalName());
        assertEquals(5000.0, response.getTargetAmount());
    }
}
