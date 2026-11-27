package com.vishnu.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO chứa kết quả phân tích dữ liệu tài chính và lời khuyên từ AI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAdviceResponse {

    // Tháng và năm được phân tích
    private int month;
    private int year;

    // Đánh giá tình trạng sức khỏe tài chính: EXCELLENT, GOOD, WARNING, DANGER
    private String financialHealthStatus;

    // Tỷ lệ tiết kiệm (%) = (Thu nhập - Chi tiêu) / Thu nhập * 100
    private BigDecimal savingsRate;

    // Tổng quan ngắn gọn về bức tranh tài chính trong tháng
    private String summary;

    // Danh sách các điểm đáng chú ý rút ra từ dữ liệu thu chi
    private List<String> insights;

    // Danh sách các lời khuyên thiết thực để tối ưu tài chính
    private List<String> recommendations;

    // Đánh giá tính khả thi của các mục tiêu tiết kiệm hiện tại
    private List<String> goalAssessments;

    // Đánh giá tình hình thực hiện ngân sách (Budget) trong tháng
    private List<String> budgetAssessments;

    // Nguồn phân tích: "SMART_FINANCIAL_ENGINE" (thuật toán tài chính) hoặc "GEMINI_AI"
    private String source;
}
