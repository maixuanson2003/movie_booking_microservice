# API Gateway — Eureka

Gateway dùng Spring Boot 4.1.1, Spring Cloud 2025.1.3 và WebFlux. Cấu hình tại `src/main/resources/application.properties`.

- Port: `SERVER_PORT`, mặc định 8080.
- Eureka: `EUREKA_SERVER_URL`, mặc định `http://localhost:8761/eureka/`.
- Docker Compose truyền URL mặc định `http://host.docker.internal:8761/eureka/`.
- Gateway đăng ký vào Eureka và lấy danh sách instance để phân giải instance cho các route khai báo trong application.properties.
- Route tường minh: `/api/auth/**` → Auth; `/api/users/**` → User; `/api/movies/**` → Movie; cinema/theater/room/showtime/seat → Cinema; booking/combo → Booking; payment/invoice/payment-method → Payment. Giữ nguyên đường dẫn `/api/...` tới backend. Tắt discovery locator tự tạo route theo tên service.
- Cần chạy Eureka Server riêng và backend phải đăng ký địa chỉ mà Gateway truy cập được.

Module hiện giữ Spring Security mặc định (yêu cầu xác thực); chưa có cấu hình JWT hay chính sách phân quyền riêng. Chưa cấu hình Redis rate limit và JWT trong module này.

Kiểm tra: `mvn -f api-gateway/pom.xml verify`. Test kiểm tra các đường dẫn API khớp đúng route lb:// và không tạo route tự động theo tên service; không cần Eureka Server cho test.
