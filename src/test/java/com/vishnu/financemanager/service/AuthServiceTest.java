package com.vishnu.financemanager.service;

import com.vishnu.financemanager.dto.RegisterRequest;
import com.vishnu.financemanager.model.User;
import com.vishnu.financemanager.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepo;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepo, encoder);
    }

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("test@example.com");
        req.setPassword("password123");
        req.setFullName("Test User");
        req.setPhoneNumber("+911234567890");

        when(userRepo.existsByUsername("test@example.com")).thenReturn(false);

        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(req);

        assertEquals("test@example.com", result.getUsername());
        assertTrue(encoder.matches("password123", result.getPassword()));
    }

    @Test
    void register_conflict() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("existing@example.com");

        when(userRepo.existsByUsername("existing@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }
}

