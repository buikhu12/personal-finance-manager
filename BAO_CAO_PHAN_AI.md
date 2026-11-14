# Báo cáo phân tích phần AI – Personal Finance Manager

## 1. Mục đích và phạm vi

Phần được giao diện gọi là **Cố vấn tài chính AI** tạo báo cáo tài chính tháng cho người dùng đang đăng nhập. Kết quả gồm trạng thái sức khỏe tài chính, tỷ lệ tiết kiệm, đoạn tổng quan, các điểm chi tiêu đáng chú ý, khuyến nghị và đánh giá mục tiêu tiết kiệm.

Về bản chất, hệ thống có hai cơ chế:

1. **Smart Financial Engine**: bộ quy tắc và phép tính viết trực tiếp bằng Java, luôn chạy trước và hoạt động không cần mạng hay khóa API.
2. **Gemini**: dịch vụ sinh văn bản tùy chọn. Nếu có khóa API và lời gọi thành công, Gemini chỉ thay thế trường `summary` (đoạn tổng quan); các trạng thái, số liệu, insights, recommendations và goal assessments vẫn do bộ quy tắc tạo.

Do vậy, đây là một hệ thống lai giữa phân tích quy tắc và mô hình ngôn ngữ, chứ không phải mô hình học máy tự huấn luyện trên dữ liệu giao dịch. Cụm từ “AI” ở giao diện bao gồm cả kết quả rule-based.

## 2. Các thành phần liên quan

| Thành phần | Vai trò |
|---|---|
| `frontend/src/components/AiAdvisorView.tsx` | Màn hình cố vấn; chọn tháng/năm, gọi API và hiển thị phản hồi. |
| `frontend/src/services/api.ts` | Khai báo hai hàm client lấy tư vấn tháng cụ thể hoặc tháng hiện tại. |
| `frontend/src/types/index.ts` | Định nghĩa kiểu dữ liệu TypeScript `AiAdviceResponse`. |
| `src/main/java/com/vishnu/financemanager/controller/AiController.java` | Nhận HTTP request, kiểm tra tháng và chuyển tiếp cho service. |
| `src/main/java/com/vishnu/financemanager/service/AiService.java` | Tính phân tích theo quy tắc, tùy chọn gọi Gemini và ghép phản hồi. |
| `src/main/java/com/vishnu/financemanager/service/ReportService.java` | Lấy giao dịch tháng đó và tổng hợp thu/chi theo danh mục. |
| `src/main/java/com/vishnu/financemanager/service/SavingsGoalService.java` | Lấy các mục tiêu của người dùng, trả số tiền còn thiếu và ngày đích. |
| `src/main/java/com/vishnu/financemanager/dto/AiAdviceResponse.java` | Cấu trúc dữ liệu JSON trả từ backend. |
| `src/main/resources/application.properties` | Đọc `AI_API_KEY` và `AI_MODEL` từ cấu hình/môi trường. |
| `src/main/java/com/vishnu/financemanager/FinanceManagerApplication.java` | Đọc `.env` khi khởi động và ánh xạ bí danh cấu hình cho Gemini. |

## 3. Luồng xử lý từ giao diện đến kết quả

1. Khi mở `AiAdvisorView`, frontend mặc định chọn tháng và năm hiện tại theo đồng hồ của trình duyệt. Khi người dùng đổi kỳ hoặc bấm làm mới, component gọi `aiApi.getMonthlyAdvice(year, month)`.
2. Client gửi `GET /api/ai/advice/monthly/{year}/{month}`. Endpoint tiện ích `GET /api/ai/advice/current` cũng có, và lấy kỳ hiện tại theo đồng hồ của backend.
3. `AiController` từ chối tháng ngoài khoảng 1–12. Cấu hình bảo mật yêu cầu đăng nhập với các API ngoài nhóm xác thực công khai, nên endpoint AI nhận `Principal` của phiên người dùng hiện tại.
4. `AiService` tìm user theo tên đăng nhập trong `Principal`, gọi `ReportService.getMonthly(...)` và `SavingsGoalService.getAll(...)` với cùng người dùng đó.
5. `ReportService` lấy các giao dịch nằm trong ngày đầu và ngày cuối của tháng. Giao dịch được cộng theo tên danh mục và phân loại dựa trên kiểu danh mục `INCOME`; các loại còn lại được cộng vào chi phí. Tiền thặng dư ròng là tổng thu trừ tổng chi.
6. `AiService` cộng các bản đồ thu/chi để tính tổng, tính tỷ lệ tiết kiệm, rồi chạy bộ quy tắc để dựng `AiAdviceResponse`.
7. Nếu có khóa Gemini, service gửi prompt kèm số liệu tóm tắt sang Google. Nếu nhận được nội dung hợp lệ, chỉ `summary` được thay thế và `source` chuyển thành `GEMINI_AI`. Khi không cấu hình khóa, phản hồi giữ nguyên với `source = SMART_FINANCIAL_ENGINE`. Lỗi Gemini được ghi log và không làm mất kết quả rule-based.
8. Frontend hiển thị status, tỷ lệ, tổng quan, điểm đáng chú ý, khuyến nghị và đánh giá mục tiêu. Nhãn nguồn chỉ phản ánh nguồn của phần tổng quan theo cách service gán `source`.

## 4. Phân tích rule engine chi tiết

### 4.1. Công thức tỷ lệ tiết kiệm

```text
thặng dư ròng = tổng thu nhập − tổng chi tiêu
tỷ lệ tiết kiệm (%) = thặng dư ròng / tổng thu nhập × 100
```

Backend dùng `BigDecimal`, chia với độ chính xác trung gian 4 chữ số, làm tròn `HALF_UP`, sau đó làm tròn tỷ lệ đến 2 chữ số thập phân. Nếu thu nhập bằng 0, tỷ lệ được đặt bằng 0 để tránh phép chia cho 0. Khi chi vượt thu, tỷ lệ có thể âm.

### 4.2. Đánh giá sức khỏe tài chính

Các điều kiện được xét theo thứ tự sau, nên nhánh đầu tiên phù hợp sẽ quyết định status và summary ban đầu:

| Điều kiện | Trạng thái | Ý nghĩa và hành động sinh sẵn |
|---|---|---|
| Thu = 0 và chi = 0 | `NO_DATA` | Chưa ghi nhận giao dịch; đề nghị bắt đầu ghi chép. |
| Thu = 0, nhưng có chi | `DANGER` | Có chi mà không ghi nhận thu; gợi ý cập nhật thu nhập hoặc giảm khoản không cần thiết. |
| Thặng dư < 0 | `DANGER` | Bội chi; khuyên rà soát chi tùy ý và hạn chế vay/ tín dụng lãi cao. |
| Tỷ lệ < 10% | `WARNING` | Tích lũy thấp; gợi ý nguyên tắc 50/30/20 và tiết kiệm ngay khi nhận thu nhập. |
| Tỷ lệ từ 10% đến dưới 20% | `GOOD` | Mức tích lũy ổn định; duy trì kỷ luật và cân nhắc tối ưu chi phí. |
| Tỷ lệ từ 20% trở lên | `EXCELLENT` | Vượt ngưỡng 20%; gợi ý quỹ khẩn cấp 3–6 tháng và tìm hiểu đầu tư an toàn. |

Các ngưỡng này là quy tắc cố định trong code, không phải ngưỡng do Gemini suy luận. Phần mô tả trên UI nêu `GOOD` là 10–20% và `EXCELLENT` là trên 20%, trong khi backend xếp đúng 20% vào `EXCELLENT`.

### 4.3. Phân tích cơ cấu chi tiêu

Khi tổng chi lớn hơn 0 và có danh mục chi:

- Sắp xếp danh mục theo số tiền giảm dần.
- Tạo insight cho danh mục chi cao nhất, kèm số tiền và tỷ trọng trên tổng chi.
- Nếu có hơn một danh mục, tạo thêm insight liệt kê tối đa ba danh mục tốn nhiều nhất.
- Nếu danh mục đứng đầu chiếm **từ 35% trở lên**, thêm lời khuyên đặt hạn mức cho danh mục ấy vào tháng sau.

Có một điểm lệch nhỏ giữa chú thích trong source và hành vi: chú thích ghi phát hiện danh mục trên 30%, nhưng điều kiện thực tế trong code là `topPercentage >= 35`.

### 4.4. Đánh giá mục tiêu tiết kiệm

Với từng mục tiêu đang có, backend lấy `remainingAmount` và `targetDate`:

- Còn thiếu bằng 0 hoặc nhỏ hơn: báo đã hoàn thành.
- Còn thiếu lớn hơn 0: tính số tháng còn lại bằng `ChronoUnit.MONTHS.between(hôm nay, ngày đích)`. Nếu kết quả bằng hoặc nhỏ hơn 0 thì ép thành 1 tháng.
- Số cần tích lũy mỗi tháng = số tiền còn thiếu / số tháng, làm tròn đến 2 chữ số `HALF_UP`.
- Nếu thặng dư tháng đang phân tích lớn hơn 0 và ít nhất bằng số cần tích lũy mỗi tháng, báo đang đúng tiến độ; nếu không, thông báo số tiền còn thiếu và mức cần tiết kiệm hàng tháng.
- Nếu không có mục tiêu, gợi ý người dùng tạo mục tiêu.

Lưu ý: thặng dư là số liệu của tháng đang xem, nhưng ngày còn lại được tính từ ngày chạy phân tích hiện tại. Vì vậy khi xem một tháng quá khứ, phép đánh giá không phải là tái dựng tiến độ tại thời điểm trong quá khứ. Phép so sánh cũng dùng cùng một thặng dư tháng cho từng mục tiêu, không phân bổ tiền giữa các mục tiêu.

## 5. Gemini được dùng như thế nào

### Cấu hình

`application.properties` ánh xạ:

```properties
ai.gemini.api-key=${AI_API_KEY:}
ai.gemini.model=${AI_MODEL:gemini-1.5-flash}
```

Khóa API là tùy chọn. Mặc định chuỗi rỗng khiến service bỏ qua gọi Gemini. `FinanceManagerApplication` đọc các dòng trong `.env` thành system properties nếu biến chưa được đặt, đồng thời ánh xạ `AI_API_KEY` ↔ `ai.gemini.api-key` và `AI_MODEL` ↔ `ai.gemini.model`. Tệp `.env.example` cung cấp tên biến mẫu.

### Request, prompt và xử lý response

Service tạo request tới endpoint Google `generateContent` cho model được cấu hình. Request là JSON có `contents[].parts[].text`; prompt yêu cầu viết tiếng Việt, dưới 150 từ. Dữ liệu trong prompt gồm tên đầy đủ người dùng, tháng/năm, tổng thu, tổng chi, thặng dư, tỷ lệ tiết kiệm, bản đồ chi theo danh mục và số lượng mục tiêu tiết kiệm. Không thấy code gửi danh sách từng giao dịch hoặc thông tin chi tiết từng mục tiêu.

Gọi mạng bằng `java.net.http.HttpClient`, thời gian chờ kết nối 5 giây và timeout request 10 giây. Khi HTTP 200, service đọc `candidates[0].content.parts[0].text`. Mã HTTP khác, response không có text hoặc exception dẫn đến `null`; kết quả rule engine được giữ lại. Prompt Gemini chỉ yêu cầu một đoạn tổng quan; không yêu cầu định dạng JSON hay danh sách khuyến nghị có cấu trúc.

### Phạm vi của Gemini và quyền riêng tư

Gemini chỉ ghi đè `summary`, không tính lại `financialHealthStatus`, `savingsRate`, `insights`, `recommendations` hay `goalAssessments`. Vì thế phần có thể mang tính ngôn ngữ tự nhiên là đoạn tổng quan; các số liệu và gợi ý còn lại là kết quả tất định từ Java.

Khi bật Gemini, các số liệu tháng, tên danh mục, tên đầy đủ và số lượng mục tiêu được gửi ra dịch vụ Google qua backend. Khóa được gắn vào query parameter `?key=...` của URL request. Cần giữ khóa ở cấu hình máy chủ và cân nhắc dữ liệu cá nhân khi bật tích hợp. Dự án hiện không có xử lý ẩn danh tên trước khi tạo prompt.

## 6. Cấu trúc phản hồi

`AiAdviceResponse` được serialize thành JSON với các trường:

| Trường | Nội dung |
|---|---|
| `month`, `year` | Kỳ phân tích. |
| `financialHealthStatus` | Một trong `NO_DATA`, `DANGER`, `WARNING`, `GOOD`, `EXCELLENT`. |
| `savingsRate` | Tỷ lệ phần trăm dạng số. |
| `summary` | Tổng quan rule-based hoặc đoạn văn do Gemini sinh. |
| `insights` | Các nhận xét rút ra từ cơ cấu chi. |
| `recommendations` | Khuyến nghị theo trạng thái và tỷ trọng danh mục. |
| `goalAssessments` | Nhận định tiến độ mục tiêu tiết kiệm. |
| `source` | `SMART_FINANCIAL_ENGINE` hoặc `GEMINI_AI`. |

## 7. Giao diện người dùng

`AiAdvisorView` tải kết quả mỗi khi tháng/năm thay đổi; có nút tải lại, trạng thái loading và thông báo lỗi. Màn hình chuyển mã trạng thái thành nhãn tiếng Việt, hiển thị tỷ lệ tiết kiệm và tổng quan, sau đó render hai danh sách insight/khuyến nghị cùng danh sách đánh giá mục tiêu. Huy hiệu nguồn hiện “Google Gemini AI” khi `source` là `GEMINI_AI`, còn lại hiện “Smart Rule Engine”. Dashboard cũng tải lời khuyên tháng hiện tại sau đăng nhập và dùng summary/status để hiển thị banner nhanh.

## 8. Kiểm thử hiện có

`AiServiceTest` kiểm tra ba tình huống rule-based chính: tỷ lệ cao dẫn đến `EXCELLENT`, bội chi dẫn đến `DANGER`, và tỷ lệ thấp dẫn đến `WARNING`; test đầu cũng kiểm tra có insight, recommendation và đánh giá mục tiêu. `AiControllerTest` kiểm tra tháng hợp lệ trả HTTP 200 và tháng 13 bị từ chối.

Các test này mock báo cáo, mục tiêu và user repository. Chúng không kiểm tra HTTP call thực tế tới Gemini, prompt, timeout, cấu hình khóa hoặc fallback khi Gemini lỗi. Chưa thấy test riêng cho `NO_DATA`, trường hợp không có thu nhập nhưng có chi, ranh giới chính xác 10%/20%/35%, hay mục tiêu ở tháng quá khứ.

## 9. Giới hạn và điểm cần hiểu đúng

1. Rule engine dùng ngưỡng tĩnh và một tháng dữ liệu; không có dự báo xu hướng, phân tích lịch sử, học từ hành vi hoặc phân loại giao dịch bằng AI.
2. Gemini chỉ viết lại summary. Việc bật Gemini không làm các phép tính trở nên chính xác hơn và nội dung sinh tự do có thể không khớp hoàn toàn với các trường số liệu còn lại.
3. Lời khuyên tài chính là nội dung tổng quát được tạo từ dữ liệu ghi nhận trong ứng dụng; kết quả phụ thuộc độ đầy đủ/chính xác của giao dịch và danh mục.
4. Endpoint kiểm tra khoảng tháng, nhưng không kiểm tra năm trong controller; giá trị năm bất thường có thể gây lỗi ở bước tạo ngày trong `ReportService`.
5. Endpoint “tháng hiện tại” dựa trên ngày/giờ máy chủ; giao diện mặc định dựa trên đồng hồ trình duyệt. Hai môi trường lệch múi giờ hoặc cấu hình ngày có thể chọn khác tháng.
6. Tỷ lệ phần trăm và định dạng tiền gửi tới Gemini được tạo bằng `BigDecimal.toString()`/`Map.toString()`, không có lớp định dạng theo đơn vị tiền tệ/locale riêng trong prompt.
7. Khi Gemini trả lời thành công, nhãn `source` gắn cho toàn DTO là `GEMINI_AI`, mặc dù chỉ riêng summary đến từ Gemini. Có thể hiểu nhầm toàn bộ phân tích do Gemini tạo.

## 10. Tóm tắt

Luồng cốt lõi đáng tin cậy ở mức quy tắc là: tổng hợp thu/chi tháng → tính tỷ lệ tiết kiệm → phân loại sức khỏe tài chính → phát hiện danh mục chi lớn → đối chiếu thặng dư với mục tiêu. Luồng này luôn chạy và là nguồn của phần lớn phản hồi. Gemini là tùy chọn, nhận một bản tóm tắt một phần dữ liệu để viết lại đoạn tổng quan; nếu Gemini vắng mặt hoặc lỗi thì ứng dụng vẫn trả lời bằng rule engine.

Các điểm quan trọng nhất khi đọc hoặc trình bày phần này là phân biệt **phân tích có cấu trúc theo quy tắc** với **đoạn văn sinh bởi mô hình ngôn ngữ**, lưu ý dữ liệu được gửi ra Google nếu cấu hình khóa, và hiểu rằng assessment mục tiêu dùng thặng dư tháng được chọn nhưng ngày còn lại tính theo ngày hiện tại.
