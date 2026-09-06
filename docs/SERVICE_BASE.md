# Cấu trúc base các service

Đồng bộ từ user-service: BaseController, BaseService, BaseMapper, ApiResponse,
ApiResponseAdvice, GlobalExceptionHandler, các exception và BaseWebFlux.
Các service có cùng cấu trúc package, với tên package riêng của module.

## Phân chia model theo schema.sql

| Service | Model |
| --- | --- |
| user-service | User (đã có) |
| movie-service | Movie |
| cinema-service | Cinema, Room, Seat, Showtime |
| booking-service | Booking, BookingItem, Combo |
| payment-service | Invoice, PaymentMethod |
| auth-service | Không có bảng riêng trong schema; dùng UserDTO và UserApiClient |

Mỗi model mới có JpaRepository, DTO và mapper tương ứng. Khóa ngoại được biểu diễn
bằng Long ID; không import entity của service khác. SQL hiện tại vẫn quản lý các
ràng buộc khóa ngoại. Chưa tạo controller nghiệp vụ hoặc thêm bảng ngoài schema.

## Auth WebClient

WebFluxConfig tạo WebClient từ builder được Spring cung cấp. UserApiClient kế thừa
BaseWebFlux và sử dụng bean này, đọc envelope rồi trả UserDTO cho ApiResponseAdvice.

- USER_SERVICE_URL: mặc định http://localhost:8081; khi chạy Docker đặt theo DNS/port thực tế.
- USER_SERVICE_TIMEOUT: mặc định 5s.
- getUser gọi GET /api/users/{id}; đây là đường dẫn dự kiến theo route gateway.
  UserController tương ứng chưa được triển khai, nên chưa thể gọi end-to-end.
- Kết nối trực tiếp bằng URL, chưa sử dụng Eureka load balancing cho WebClient này.
- BaseWebFlux chuyển HTTP lỗi thành ExternalServiceException. Body lỗi rỗng hoặc
  JSON không hợp lệ dùng thông báo dự phòng. GlobalExceptionHandler hiện theo mẫu
  user-service, chưa có nhánh riêng để giữ status của ExternalServiceException.

## Database và phạm vi sử dụng

Đã bổ sung PostgreSQL runtime và cấu hình datasource theo user-service cho các
module còn lại. Cần đặt APP_DB_PASSWORD, có thể cấu hình DB_HOST, DB_PORT, DB_NAME,
DB_USER. ddl-auto=validate và sql.init.mode=never: ứng dụng không tự tạo bảng.

api-gateway giữ cấu trúc gateway reactive; không thêm model hoặc base JPA/CRUD.

Các base giữ hành vi của user-service, bao gồm updateAll dùng saveAll. Đây chưa
phải cập nhật từng phần (PATCH) hoặc kiểm tra mọi ID chỉ được cập nhật bản ghi đã có.
Controller nghiệp vụ cần ràng buộc ID trên URL với ID dữ liệu trước khi sử dụng
update hiện tại. User hiện có trường role nhưng schema.sql chưa có cột này; cần
thống nhất schema trước khi chạy Hibernate validate cho user-service.

## Kiểm tra

Biên dịch từng module bằng Maven. Test UserApiClient dùng phản hồi HTTP giả lập,
không khởi động PostgreSQL hoặc Eureka; chưa kiểm thử kết nối thực tế giữa service.
