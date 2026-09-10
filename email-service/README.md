# Email Service

Module nền theo cấu trúc `payment-service`, dùng Java 21, Spring Boot 4.1.1 và Spring Cloud 2025.1.3.

- `controller/BaseController`, `service/BaseService`, `sharedLogic/mapper/BaseMapper`: nền CRUD. Controller kế thừa cần triển khai `setEntityId` để dùng ID trên URL khi cập nhật.
- `ApiResponse`, response advice và global exception handler: thống nhất response `{message, data, success}` với các service khác.
- `EmailTemplate`, `EmailTemplateDTO`, `EmailTemplateMapper`, `EmailTemplateRepository`: ánh xạ bảng `email_templates`, có truy vấn theo tên, loại và trạng thái.
- Kafka producer/consumer, WebClient và Eureka: cấu hình hạ tầng chung. WebClient bean nhận URL đầy đủ tại nơi gọi.

`EmailService.sendEmail(request)` gửi HTML UTF-8 qua SMTP, lấy body từ template có type tương ứng và status `ACTIVE`. Subject lấy từ request; placeholder `{{key}}` trong body được thay bằng variables (escape HTML). Listener `user-registered` gửi template `WELCOME_EMAIL`. Spring Security hiện dùng cấu hình mặc định của Spring Boot.

## Kafka và outbox

User service lưu user và `user_registration_outbox` trong cùng transaction. Tác vụ nền đọc một sự kiện mỗi 5 giây (cấu hình `app.outbox.poll-interval-ms`), chờ Kafka ACK rồi xóa dòng outbox. Khi broker lỗi, dòng được giữ lại cho lần sau. Có khóa `FOR UPDATE SKIP LOCKED` để nhiều instance không lấy cùng dòng đồng thời.

Email consumer xử lý đồng bộ, commit offset sau khi gửi thành công. Lỗi tạm thời được thử lại 3 lần, cách nhau 2 giây; payload không hợp lệ được chuyển thẳng vào `user-registered.DLT`. Nếu gửi DLT thất bại thì không bỏ qua record nguồn. DLT cần được theo dõi và phát lại có kiểm soát sau khi sửa nguyên nhân; chưa có tác vụ tự phát lại.

Luồng có thể gửi trùng nếu SMTP đã nhận email nhưng tiến trình chết trước khi commit offset, hoặc Kafka ACK thành công nhưng outbox chưa commit. Đây là giao nhận ít nhất một lần, chưa bảo đảm gửi email đúng một lần.

Database đang tồn tại cần áp dụng migration trước khi chạy user-service mới. Từ thư mục gốc, với PostgreSQL container đang chạy:

```powershell
Get-Content -Raw docker/postgres/migrations/20260910_registration_outbox.sql | docker compose exec -T postgres sh -c 'PGUSER="$POSTGRES_USER" PGOPTIONS="-c role=movie_app" psql -v ON_ERROR_STOP=1 --single-transaction -d movie_booking'
docker compose run --rm kafka-init
docker compose up -d --build user-service email-service
```

Migration chỉ thêm bảng/index; volume mới được tạo sẵn từ schema init. Cần có template `WELCOME_EMAIL` với status `ACTIVE` trong `email_templates`, cùng SMTP credentials hợp lệ. Compose tạo `user-registered` và `user-registered.DLT` với cùng 3 partition trước khi khởi động hai service.

## Cấu hình SMTP

Spring Boot tự cấu hình `JavaMailSender` từ `spring-boot-starter-mail`; không cần khai báo bean thủ công.
Đặt các biến sau trong môi trường chạy IDE/terminal, hoặc `.env` ở thư mục gốc khi dùng Docker Compose:

```dotenv
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=your-smtp-username
MAIL_PASSWORD=your-smtp-password
MAIL_FROM=no-reply@example.com
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLE=true
MAIL_STARTTLS_REQUIRED=true
MAIL_SSL_ENABLE=false
```

Thay thông tin ví dụ bằng cấu hình nhà cung cấp SMTP. `MAIL_FROM` mặc định dùng `MAIL_USERNAME` nếu không cấu hình. Java chạy trực tiếp không tự nạp file `.env`.
Nếu nhà cung cấp dùng SSL trực tiếp trên cổng 465: đặt `MAIL_PORT=465`, `MAIL_SSL_ENABLE=true`, `MAIL_STARTTLS_ENABLE=false`, `MAIL_STARTTLS_REQUIRED=false`.
Với SMTP local không xác thực/TLS, đặt `MAIL_SMTP_AUTH=false` và hai biến STARTTLS bằng `false`, cùng host/port của SMTP local. Trong container, `localhost` trỏ vào chính container.

Inject `EmailService` vào nơi xử lý nghiệp vụ và gọi:

```java
emailService.sendEmail(new SendEmailRequest("customer@example.com", "Xác nhận đặt vé", "BOOKING_CONFIRMATION", Map.of("username", "An")));
```

Kiểm tra gửi mail bằng mock, không kết nối SMTP hoặc database:

```powershell
.\mvnw.cmd -Dtest=EmailServiceTests test
```

## Chạy local

Database `movie_booking` phải có bảng `email_templates` trong `docker/postgres/schema.sql`. Hibernate dùng `ddl-auto=validate`, không tự tạo hoặc sửa bảng. Volume PostgreSQL đã tồn tại không tự chạy lại init SQL.

Đặt biến môi trường `APP_DB_PASSWORD`, rồi chạy từ thư mục `email-service`:

```powershell
.\mvnw.cmd spring-boot:run
```

Mặc định: port `8087`, PostgreSQL `localhost:5432`, user `movie_app`, Kafka `localhost:9092`, Eureka `http://localhost:8761/eureka/`.
Có thể ghi đè bằng `SERVER_PORT`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_CONSUMER_GROUP`, `EUREKA_SERVER_URL`.

Chạy bằng Docker Compose từ thư mục gốc:

```powershell
docker compose up -d --build email-service
```

`mvn test` bao gồm context test cần database và cấu hình môi trường ở trên.
