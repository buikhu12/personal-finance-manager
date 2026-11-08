package com.vishnu.financemanager.controller;

import com.vishnu.financemanager.dto.AiAdviceResponse;
import com.vishnu.financemanager.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AiControllerTest {

    @InjectMocks
    private AiController aiController;

    @Mock
    private AiService aiService;

    private Principal principal;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        principal = () -> "test@example.com";
    }

    @Test
    void getMonthlyAdvice_validMonth_returnsOk() {
        AiAdviceResponse mockAdvice = AiAdviceResponse.builder()
                .month(11)
                .year(2026)
                .financialHealthStatus("GOOD")
                .savingsRate(new BigDecimal("15.50"))
                .summary("Tình hình tài chính tốt")
                .insights(List.of("Điểm nổi bật"))
                .recommendations(List.of("Lời khuyên 1"))
                .goalAssessments(List.of("Đánh giá mục tiêu"))
                .source("SMART_FINANCIAL_ENGINE")
                .build();

        when(aiService.getMonthlyAdvice(2026, 11, principal)).thenReturn(mockAdvice);

        ResponseEntity<AiAdviceResponse> response = aiController.getMonthlyAdvice(2026, 11, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("GOOD", response.getBody().getFinancialHealthStatus());
    }

    @Test
    void getMonthlyAdvice_invalidMonth_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                aiController.getMonthlyAdvice(2026, 13, principal));
    }
}
