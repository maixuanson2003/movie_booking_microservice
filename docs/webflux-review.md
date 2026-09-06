# Review cấu hình WebClient trong user-service

Ngày review: 06/09/2026.

## Phạm vi

- `user-service/src/main/java/com/example/user_service/config/WebFluxConfig.java`
- `user-service/src/main/java/com/example/user_service/sharedLogic/webFlux/serviceNameApi.java`
- Đối chiếu với `GlobalExceptionHandler`, `ApiResponseAdvice`, `ApiResponse` và cấu hình Eureka hiện tại.

Đây là kết quả review mã nguồn; chưa kiểm tra việc gọi service thực tế và chưa sửa các file trên.

## Kết quả

Cấu trúc bean `WebClient` và client dùng constructor injection đã có, nhưng cần hoàn thiện các phần dưới đây trước khi sử dụng.

### 1. Thay URL placeholder và xác định service đích

`WebFluxConfig` đang đặt cố định `http://service:8080`. Cần thay bằng địa chỉ service thực tế và đưa cấu hình ra `application.properties`.

Nếu gọi thông qua Eureka, cần sử dụng `WebClient.Builder` có `@LoadBalanced` và URI theo tên đăng ký service, ví dụ `http://movie-service`. Chỉ có dependency LoadBalancer và Eureka chưa khiến builder hiện tại tự tra cứu service.

Nếu gọi trực tiếp bằng hostname và port thực tế thì sử dụng builder thông thường; không bắt buộc dùng Eureka.

### 2. Xử lý lỗi từ service đích

`retrieve()` mặc định phát sinh `WebClientResponseException` khi nhận HTTP 4xx hoặc 5xx. Hiện tại chưa có ánh xạ lỗi riêng cho client này. Nếu exception truyền về controller, nhánh xử lý lỗi ngoài dự kiến trong `GlobalExceptionHandler` sẽ trả HTTP 500.

Cần xác định cách ánh xạ lỗi theo nghiệp vụ, ví dụ:

| Trường hợp | Cách xử lý đề xuất |
| --- | --- |
| Không tìm thấy tài nguyên cần truy vấn | Chuyển thành `ResourceNotFoundException` nếu phù hợp nghiệp vụ |
| Dữ liệu xung đột | Chuyển thành `ConflictException` nếu phù hợp nghiệp vụ |
| Không kết nối được service | Chuyển thành `ServiceUnavailableException` |
| Service đích trả lỗi hệ thống | Trả thông báo chung, ghi chi tiết vào log nội bộ |
| Hết thời gian chờ | Xử lý riêng, quy định mã HTTP phù hợp |

Không chuyển nguyên mọi mã lỗi hoặc nội dung lỗi từ service đích cho người gọi; lỗi xác thực giữa các service có thể khác lỗi xác thực của người dùng.

### 3. Làm rõ kiểu dữ liệu phản hồi

`getUser()` hiện trả `Mono<Object>` và dùng `bodyToMono(Object.class)`, nên không thể hiện rõ cấu trúc dữ liệu nhận được.

Nên khai báo kiểu phản hồi cụ thể tương thích với `ApiResponse`. Nếu service đích trả envelope `message`, `data`, `success`, cần bảo đảm lớp phản hồi có thể deserialize JSON và xác định kiểu dữ liệu của `data`.

Khi truyền phản hồi về controller, tránh trả nguyên một map chứa envelope để `ApiResponseAdvice` bọc thêm lần nữa. Có thể trả một đối tượng `ApiResponse` thực sự, hoặc lấy dữ liệu nghiệp vụ rồi tạo phản hồi của service hiện tại.

### 4. Bổ sung timeout

Chưa có cấu hình timeout riêng trong client. Cần quy định thời gian kết nối và thời gian chờ phản hồi phù hợp, đồng thời ánh xạ lỗi timeout sang phản hồi `ApiResponse`.

### 5. Kiểm tra đường dẫn và vị trí đặt client

Client đang gọi `/users/{id}`, nhưng tại thời điểm review chưa có `UserController` đăng ký endpoint này trong `user-service`. Cần đối chiếu với endpoint thật của service đích.

Nếu mục đích là đọc user ngay trong `user-service`, nên gọi `UserService` trực tiếp. Client HTTP phù hợp khi một service cần gọi sang service khác.

### 6. Chuẩn hóa tên

- Đổi `serviceNameApi` thành tên thể hiện đúng service đích, ví dụ `UserApiClient` nếu đây là client gọi API user.
- Đổi package `webFlux` thành `webflux` theo quy ước package Java viết thường.
- Có thể đổi `WebFluxConfig` thành `WebClientConfig` để tên phản ánh đúng chức năng: cấu hình HTTP client.

## Thứ tự thực hiện

1. Xác định service đích, đường dẫn API và cách kết nối trực tiếp hay qua Eureka.
2. Cấu hình URL, builder và timeout.
3. Khai báo kiểu phản hồi, bảo đảm chỉ có một envelope `ApiResponse`.
4. Ánh xạ lỗi kết nối, timeout và lỗi HTTP.
5. Kiểm thử các trường hợp thành công, 404, 5xx, timeout và service không khả dụng.

## Tài liệu tham khảo

- [Spring Cloud Commons — Load-balanced WebClient](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/common-abstractions.html)
- [Spring Framework — WebClient retrieve() và xử lý lỗi](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-retrieve.html)
