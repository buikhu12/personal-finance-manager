package com.vishnu.financemanager.controller;

import com.vishnu.financemanager.dto.LoginRequest;
import com.vishnu.financemanager.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void login_validCredentials_authenticatesAndSavesContext() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("test@example.com");
        loginRequest.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@example.com");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        ResponseEntity<?> responseEntity = authController.login(loginRequest, request, response);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(securityContextRepository).saveContext(any(), eq(request), eq(response));
    }

    @Test
    void login_invalidCredentials_returnsUnauthorized() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("test@example.com");
        loginRequest.setPassword("wrong");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        ResponseEntity<?> responseEntity = authController.login(loginRequest, request, response);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) responseEntity.getBody();
        assertEquals("Invalid credentials", body.get("error"));
    }

    @Test
    void logout_invalidatesSessionAndReturnsOk() {
        when(request.getSession(false)).thenReturn(session);

        ResponseEntity<?> responseEntity = authController.logout(request, response);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(session).invalidate();
    }
}
