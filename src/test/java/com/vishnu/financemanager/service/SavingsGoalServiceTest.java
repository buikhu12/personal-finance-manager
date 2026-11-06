package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.GoalRequest;
import com.vishnu.financemanager.dto.GoalResponse;
import com.vishnu.financemanager.model.*;
import com.vishnu.financemanager.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.math.BigDecimal;
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
        request.setTargetAmount(new BigDecimal("5000.00"));
        request.setTargetDate(LocalDate.now().plusMonths(3).toString());
        request.setStartDate(LocalDate.now().toString());

        SavingsGoal saved = SavingsGoal.builder()
                .id(1L)
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("5000.00"))
                .currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusMonths(3))
                .startDate(LocalDate.now())
                .user(user).build();

        when(goalRepo.save(any())).thenReturn(saved);

        GoalResponse response = service.createGoal(request, principal);
        assertEquals("Emergency Fund", response.getGoalName());
        assertEquals(new BigDecimal("5000.00"), response.getTargetAmount());
        assertEquals(BigDecimal.ZERO, response.getCurrentProgress());
    }

    @Test
    void create_targetDateBeforeStartDate_throwsException() {
        GoalRequest request = new GoalRequest();
        request.setGoalName("Emergency Fund");
        request.setTargetAmount(new BigDecimal("5000.00"));
        request.setStartDate(LocalDate.now().toString());
        request.setTargetDate(LocalDate.now().minusDays(1).toString());

        assertThrows(IllegalArgumentException.class, () -> service.createGoal(request, principal));
    }

    @Test
    void addSavings_increasesCurrentProgressIndividually() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(1L)
                .goalName("Vacation")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(new BigDecimal("200.00"))
                .startDate(LocalDate.now())
                .targetDate(LocalDate.now().plusMonths(1))
                .user(user)
                .build();

        when(goalRepo.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GoalResponse response = service.addSavings(1L, new BigDecimal("300.00"), principal);

        assertEquals(new BigDecimal("500.00"), response.getCurrentProgress());
        assertEquals(new BigDecimal("500.00"), response.getRemainingAmount());
        assertEquals(new BigDecimal("50.00"), response.getProgressPercentage());
    }
}
