package com.vishnu.financemanager.controller;

import com.vishnu.financemanager.dto.AiAdviceResponse;
import com.vishnu.financemanager.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;

/**
 * Controller cung cấp các API phân tích tài chính thông minh và đưa ra lời khuyên từ AI.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * API phân tích dữ liệu và nhận lời khuyên cho một tháng và năm cụ thể.
     * Ví dụ gọi: GET /api/ai/advice/monthly/2026/11
     *
     * @param year      Năm cần phân tích
     * @param month     Tháng cần phân tích (1 - 12)
     * @param principal Thông tin tài khoản người dùng đang đăng nhập (tự động nhận qua session)
     * @return DTO chứa đánh giá sức khỏe tài chính, điểm nổi bật và các lời khuyên thiết thực
     */
    @GetMapping("/advice/monthly/{year}/{month}")
    public ResponseEntity<AiAdviceResponse> getMonthlyAdvice(@PathVariable int year,
                                                            @PathVariable int month,
                                                            Principal principal) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Tháng không hợp lệ, vui lòng chọn từ 1 đến 12.");
        }
        return ResponseEntity.ok(aiService.getMonthlyAdvice(year, month, principal));
    }

    /**
     * API tiện ích: Lấy lời khuyên tài chính cho tháng hiện tại.
     * Ví dụ gọi: GET /api/ai/advice/current
     *
     * @param principal Người dùng hiện tại
     * @return Đánh giá và lời khuyên cho tháng hiện tại
     */
    @GetMapping("/advice/current")
    public ResponseEntity<AiAdviceResponse> getCurrentMonthAdvice(Principal principal) {
        LocalDate now = LocalDate.now();
        return ResponseEntity.ok(aiService.getMonthlyAdvice(now.getYear(), now.getMonthValue(), principal));
    }
}
