package com.vishnu.financemanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vishnu.financemanager.dto.AiAdviceResponse;
import com.vishnu.financemanager.dto.GoalResponse;
import com.vishnu.financemanager.dto.MonthlyReportResponse;
import com.vishnu.financemanager.model.User;
import com.vishnu.financemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service AI chịu trách nhiệm phân tích báo cáo tài chính của người dùng
 * và đưa ra lời khuyên, nhận xét và đánh giá các mục tiêu tiết kiệm.
 *
 * Thiết kế bao gồm 2 lớp:
 * 1. Thuật toán chuyên gia tài chính (Smart Financial Rule Engine):
 *    - Tự động tính toán tỷ lệ tiết kiệm (Savings Rate).
 *    - Phân tích cơ cấu chi tiêu, phát hiện các khoản chi chiếm tỷ trọng lớn (>30%).
 *    - Đánh giá khả năng hoàn thành mục tiêu tiết kiệm đúng hạn dựa trên thặng dư hàng tháng.
 *    - Chạy hoàn toàn offline, tốc độ phản hồi mili-giây, không tốn phí API.
 * 2. Tích hợp Generative AI (Gemini API - tùy chọn):
 *    - Nếu có cấu hình API Key trong file .env, service sẽ gửi dữ liệu tóm tắt lên Gemini
 *      để sinh ra lời khuyên cá nhân hóa, sinh động bằng ngôn ngữ tự nhiên.
 *    - Nếu không có API Key hoặc mạng bị lỗi, service sẽ tự động dùng kết quả từ Rule Engine.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final ReportService reportService;
    private final SavingsGoalService savingsGoalService;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Đọc API Key từ file cấu hình / .env (nếu có)
    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    // Tên model Gemini muốn sử dụng
    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    /**
     * Phân tích dữ liệu tài chính của một tháng cụ thể và đưa ra tư vấn.
     *
     * @param year      Năm cần phân tích
     * @param month     Tháng cần phân tích (1 - 12)
     * @param principal Thông tin tài khoản người dùng đang đăng nhập
     * @return DTO chứa tình trạng sức khỏe tài chính, nhận xét và lời khuyên
     */
    public AiAdviceResponse getMonthlyAdvice(int year, int month, Principal principal) {
        // 1. Lấy thông tin người dùng
        User user = userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Lấy dữ liệu báo cáo thu - chi của tháng đó
        MonthlyReportResponse report = reportService.getMonthly(year, month, principal);

        // 3. Lấy danh sách các mục tiêu tiết kiệm hiện có của người dùng
        List<GoalResponse> goals = savingsGoalService.getAll(principal);

        // 4. Tính toán tổng thu nhập và tổng chi tiêu từ dữ liệu báo cáo
        BigDecimal totalIncome = report.getTotalIncome().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = report.getTotalExpenses().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netSavings = report.getNetSavings();

        // 5. Tính tỷ lệ tiết kiệm = (Tiền tiết kiệm / Tổng thu nhập) * 100
        BigDecimal savingsRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings.divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 6. Chạy thuật toán phân tích chuyên gia tài chính (Rule-based Engine)
        AiAdviceResponse advice = buildExpertAnalysis(user, year, month, totalIncome, totalExpenses,
                netSavings, savingsRate, report.getTotalExpenses(), goals);

        // 7. Nếu người dùng có cấu hình Gemini API Key, thử gọi AI tạo sinh để làm phong phú thêm lời khuyên
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty()) {
            try {
                String aiGeneratedSummary = callGeminiApi(user.getFullName(), year, month,
                        totalIncome, totalExpenses, netSavings, savingsRate, report.getTotalExpenses(), goals);

                if (aiGeneratedSummary != null && !aiGeneratedSummary.trim().isEmpty()) {
                    advice.setSummary(aiGeneratedSummary);
                    advice.setSource("GEMINI_AI");
                }
            } catch (Exception e) {
                log.warn("Không thể gọi Gemini API, chuyển sang dùng bộ phân tích tích hợp sẵn: {}", e.getMessage());
            }
        }

        return advice;
    }

    /**
     * Phân tích dữ liệu theo các nguyên tắc tài chính cá nhân chuẩn (như quy tắc 50/30/20).
     */
    private AiAdviceResponse buildExpertAnalysis(User user, int year, int month,
                                                 BigDecimal totalIncome, BigDecimal totalExpenses,
                                                 BigDecimal netSavings, BigDecimal savingsRate,
                                                 Map<String, BigDecimal> expensesByCategory,
                                                 List<GoalResponse> goals) {

        String healthStatus;
        String summary;
        List<String> insights = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        List<String> goalAssessments = new ArrayList<>();

        // -------------------------------------------------------------
        // BƯỚC 1: ĐÁNH GIÁ SỨC KHỎE TÀI CHÍNH TỔNG THỂ
        // -------------------------------------------------------------
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0 && totalExpenses.compareTo(BigDecimal.ZERO) == 0) {
            healthStatus = "NO_DATA";
            summary = String.format("Tháng %d/%d bạn chưa ghi nhận giao dịch thu chi nào.", month, year);
            recommendations.add("Hãy bắt đầu ghi chép các khoản chi tiêu hằng ngày để AI có thể phân tích chính xác.");
        } else if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            healthStatus = "DANGER";
            summary = String.format("Cảnh báo: Tháng %d/%d có tổng chi tiêu %s nhưng chưa ghi nhận thu nhập.",
                    month, year, totalExpenses);
            recommendations.add("Bạn đang chi tiêu từ tiền dự trữ hoặc vay nợ. Hãy cập nhật nguồn thu nhập hoặc cắt giảm các chi phí chưa cần thiết.");
        } else if (netSavings.compareTo(BigDecimal.ZERO) < 0) {
            healthStatus = "DANGER";
            summary = String.format("Tháng %d/%d bạn đang bị BỘI CHI (âm %s). Chi tiêu đã vượt quá thu nhập của bạn.",
                    month, year, netSavings.abs());
            recommendations.add("Cần rà soát và cắt giảm ngay lập tức các khoản chi tùy ý (giải trí, mua sắm).");
            recommendations.add("Hạn chế tối đa dùng thẻ tín dụng hoặc các khoản vay tiêu dùng lãi suất cao trong giai đoạn này.");
        } else if (savingsRate.compareTo(BigDecimal.valueOf(10)) < 0) {
            healthStatus = "WARNING";
            summary = String.format("Tháng %d/%d tỷ lệ tiết kiệm đạt %.1f%% (thặng dư %s). Mức này còn khá khiêm tốn.",
                    month, year, savingsRate, netSavings);
            recommendations.add("Áp dụng quy tắc 50/30/20: Dành 50% cho nhu cầu thiết yếu, 30% cho sở thích và ít nhất 20% cho tiết kiệm.");
            recommendations.add("Nên trích tiền tiết kiệm ngay khi nhận lương đầu tháng thay vì đợi đến cuối tháng.");
        } else if (savingsRate.compareTo(BigDecimal.valueOf(20)) < 0) {
            healthStatus = "GOOD";
            summary = String.format("Tháng %d/%d sức khỏe tài chính ở mức TỐT. Tỷ lệ tiết kiệm đạt %.1f%% với thặng dư %s.",
                    month, year, savingsRate, netSavings);
            recommendations.add("Duy trì thói quen chi tiêu kỷ luật này để đạt mục tiêu tự do tài chính.");
            recommendations.add("Cân nhắc tối ưu thêm 3-5% chi phí không thiết yếu để nâng tỷ lệ tích lũy lên mốc 20%.");
        } else {
            healthStatus = "EXCELLENT";
            summary = String.format("Tuyệt vời! Tháng %d/%d tỷ lệ tiết kiệm của bạn đạt %.1f%% (thặng dư %s), vượt chuẩn 20%%.",
                    month, year, savingsRate, netSavings);
            recommendations.add("Bạn quản lý tài chính rất xuất sắc! Hãy duy trì quỹ khẩn cấp tương đương 3-6 tháng chi phí sinh hoạt.");
            recommendations.add("Với lượng tiền nhàn rỗi ổn định, bạn có thể tìm hiểu thêm các kênh đầu tư an toàn sinh lời.");
        }

        // -------------------------------------------------------------
        // BƯỚC 2: PHÂN TÍCH CHI TIẾT CÁC DANH MỤC CHI TIÊU
        // -------------------------------------------------------------
        if (totalExpenses.compareTo(BigDecimal.ZERO) > 0 && !expensesByCategory.isEmpty()) {
            // Sắp xếp các danh mục có số tiền chi tiêu nhiều nhất giảm dần
            List<Map.Entry<String, BigDecimal>> sortedExpenses = expensesByCategory.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .collect(Collectors.toList());

            // Nhận diện danh mục chiếm nhiều tiền nhất
            Map.Entry<String, BigDecimal> topExpense = sortedExpenses.get(0);
            BigDecimal topPercentage = topExpense.getValue()
                    .divide(totalExpenses, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);

            insights.add(String.format("Khoản chi lớn nhất tháng là '%s': %s (chiếm %.1f%% tổng chi tiêu).",
                    topExpense.getKey(), topExpense.getValue(), topPercentage));

            // Nếu một danh mục chiếm trên 35% chi tiêu, đưa ra lời khuyên cụ thể
            if (topPercentage.compareTo(BigDecimal.valueOf(35)) >= 0) {
                recommendations.add(String.format("Hạng mục '%s' đang chiếm tỷ trọng rất cao (%.1f%%). Bạn nên thiết lập hạn mức ngân sách tối đa cho danh mục này vào tháng tới.",
                        topExpense.getKey(), topPercentage));
            }

            // Nhận diện top 3 danh mục tiêu tốn nhất
            if (sortedExpenses.size() > 1) {
                String top3Names = sortedExpenses.stream().limit(3)
                        .map(e -> String.format("%s (%s)", e.getKey(), e.getValue()))
                        .collect(Collectors.joining(", "));
                insights.add("Top các danh mục chi nhiều nhất: " + top3Names + ".");
            }
        }

        // -------------------------------------------------------------
        // BƯỚC 3: ĐÁNH GIÁ TÍNH KHẢ THI CỦA CÁC MỤC TIÊU TIẾT KIỆM
        // -------------------------------------------------------------
        if (goals.isEmpty()) {
            goalAssessments.add("Bạn chưa tạo mục tiêu tiết kiệm nào. Hãy tạo mục tiêu (như Quỹ khẩn cấp, Mua xe, Du lịch) để có kế hoạch tích lũy rõ ràng.");
        } else {
            LocalDate today = LocalDate.now();

            for (GoalResponse goal : goals) {
                LocalDate targetDate = LocalDate.parse(goal.getTargetDate());
                long monthsLeft = ChronoUnit.MONTHS.between(today, targetDate);
                if (monthsLeft <= 0) monthsLeft = 1; // Tối thiểu tính 1 tháng

                BigDecimal remainingAmount = goal.getRemainingAmount();

                if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    goalAssessments.add(String.format("Mục tiêu '%s': Đã hoàn thành 100%%! Chúc mừng bạn.",
                            goal.getGoalName()));
                } else {
                    // Số tiền cần tiết kiệm mỗi tháng để đạt mục tiêu đúng hạn
                    BigDecimal neededPerMonth = remainingAmount.divide(BigDecimal.valueOf(monthsLeft), 2, RoundingMode.HALF_UP);

                    if (netSavings.compareTo(BigDecimal.ZERO) > 0 && netSavings.compareTo(neededPerMonth) >= 0) {
                        goalAssessments.add(String.format("Mục tiêu '%s': Cần tích lũy khoảng %s/tháng (còn %d tháng). Với mức thặng dư hiện tại (%s), bạn đang đi đúng tiến độ!",
                                goal.getGoalName(), neededPerMonth, monthsLeft, netSavings));
                    } else {
                        goalAssessments.add(String.format("Mục tiêu '%s': Còn thiếu %s (hạn %s, khoảng %d tháng nữa). Bạn cần tiết kiệm %s/tháng để kịp tiến độ.",
                                goal.getGoalName(), remainingAmount, goal.getTargetDate(), monthsLeft, neededPerMonth));
                    }
                }
            }
        }

        return AiAdviceResponse.builder()
                .month(month)
                .year(year)
                .financialHealthStatus(healthStatus)
                .savingsRate(savingsRate)
                .summary(summary)
                .insights(insights)
                .recommendations(recommendations)
                .goalAssessments(goalAssessments)
                .source("SMART_FINANCIAL_ENGINE")
                .build();
    }

    /**
     * Gọi API Google Gemini để sinh lời khuyên tài chính cá nhân hóa tự nhiên bằng tiếng Việt.
     * Sử dụng thư viện java.net.http.HttpClient có sẵn trong Java 11+, không cần thêm dependency ngoài.
     */
    private String callGeminiApi(String userName, int year, int month,
                                 BigDecimal totalIncome, BigDecimal totalExpenses,
                                 BigDecimal netSavings, BigDecimal savingsRate,
                                 Map<String, BigDecimal> expensesByCategory,
                                 List<GoalResponse> goals) {
        try {
            String endpoint = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    geminiModel, geminiApiKey);

            // Tạo prompt tóm tắt dữ liệu cho Gemini
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Bạn là một chuyên gia tư vấn tài chính cá nhân thông thái và thân thiện. ");
            promptBuilder.append(String.format("Hãy phân tích dữ liệu tài chính tháng %d/%d của người dùng tên '%s':\n", month, year, userName));
            promptBuilder.append(String.format("- Tổng thu nhập: %s\n", totalIncome));
            promptBuilder.append(String.format("- Tổng chi tiêu: %s\n", totalExpenses));
            promptBuilder.append(String.format("- Tiền tiết kiệm thuần: %s\n", netSavings));
            promptBuilder.append(String.format("- Tỷ lệ tiết kiệm: %s%%\n", savingsRate));
            promptBuilder.append("- Chi tiêu theo danh mục: " + expensesByCategory.toString() + "\n");
            promptBuilder.append("- Số mục tiêu tiết kiệm đang theo dõi: " + goals.size() + "\n");
            promptBuilder.append("Yêu cầu: Hãy viết một đoạn tổng quan ngắn gọn (dưới 150 từ), súc tích, mang tính động viên và đưa ra định hướng quan trọng nhất bằng tiếng Việt.");

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", promptBuilder.toString())
                            ))
                    )
            );

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidateText = root.path("candidates").path(0)
                        .path("content").path("parts").path(0).path("text");
                if (!candidateText.isMissingNode()) {
                    return candidateText.asText().trim();
                }
            } else {
                log.warn("Gemini API trả về mã lỗi HTTP {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Lỗi khi kết nối tới Gemini API: {}", e.getMessage());
        }
        return null;
    }
}
