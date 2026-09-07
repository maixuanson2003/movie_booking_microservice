# API user-service hỗ trợ login

Cập nhật 07/09/2026. User-service giữ dữ liệu tài khoản và kiểm tra mật khẩu; auth-service tạo JWT sau khi kiểm tra thành công.

## API

### GET /api/users/username/{username}

Trả HTTP 200 với envelope `{"message":"Success","success":true,"data":{...}}`. Data là UserDTO từ UserMapper: id, username, email, fullName, role, phone, avatar, status, timestamps; password không được map từ entity nên không trả hash. Không tìm thấy username →404. Username trắng →400.

### POST /api/users/check-password

Content-Type: application/json.

```json
{"username":"alice","password":"your-password"}
```

Kết quả kiểm tra thành công:

```json
{"message":"Success","data":true,"success":true}
```

Sai mật khẩu, không có user hoặc status khác `ACTIVE` trả HTTP 200 với `data:false, success:true`: request đã xử lý thành công nhưng thông tin đăng nhập không hợp lệ. Thiếu/null/trắng username hoặc password →400. Không gửi password trên URL.

PasswordEncoder dùng BCryptPasswordEncoder.matches với hash của **đúng username**. Cột users.password phải chứa BCrypt hash (ví dụ dạng `$2a$...`/`$2b$...`), không phải plaintext hoặc hash có tiền tố `{bcrypt}`. Khi tạo/đổi mật khẩu cần dùng cùng PasswordEncoder.encode. Yêu cầu này chưa thêm API đăng ký hoặc sửa dữ liệu DB hiện có.

## Các file liên quan

- `controller/UserController.java`: khai báo hai route, không kế thừa BaseController để tránh mở thêm CRUD ngoài phạm vi login.
- `service/UserService.java`: transaction read-only; tra user, validate input, kiểm tra ACTIVE và BCrypt.
- `repository/UserRepository.java`: dùng findByUsername có sẵn, không truy vấn bằng password.
- `sharedLogic/dto/CheckPasswordRequest.java`: username/password trong body; toString che credentials.
- `sharedLogic/mapper/UserMapper.java`: dùng mapper có sẵn để loại password khỏi response.
- `config/SecurityConfig.java`: bean BCrypt; cho phép hai route trước khi có token; chỉ bỏ kiểm tra CSRF cho đường dẫn check-password. Các route khác vẫn yêu cầu xác thực. Hai route hiện không có cơ chế xác thực service-to-service riêng.
- `config/WebFluxConfig.java`: fallback WebClient.builder khi Spring chưa cấp builder, để tránh lỗi khởi tạo bean trong cấu hình Boot hiện tại.

Đường dẫn Java trong danh sách trên tính từ `src/main/java/com/example/user_service/`.

## Kết nối với auth-service

AuthService.login giữ luồng đồng bộ hiện tại: lấy user theo username → POST kiểm tra username/password → tạo JWT. UserApiClient.checkPassword nhận hai tham số username/password; JSON request được WebClient serialize từ Map. Lookup 404 hoặc kiểm tra false được AuthService chuyển thành UnauthorizedException 401 với cùng message `Invalid username or password`. AuthResponse có Builder để dùng được với hàm login.

BaseWebFlux dùng `success` của envelope để xác định lỗi giao thức; `data:false` là kết quả hợp lệ, còn data null là response không hợp lệ. Hàm login dùng block nên dành cho luồng MVC hiện tại, không gọi trực tiếp từ reactive event-loop.

Chưa thêm controller HTTP login bên auth, đăng ký, refresh token hoặc sửa JwtService trong yêu cầu này. JwtService hiện tại đã được người dùng đổi lại và không còn khớp bộ JwtServiceTests cũ; không coi kiểm chứng login bằng JWT mock là kiểm chứng tính đúng của JWT thật.

## Kiểm chứng

21 test user-service đã đạt: UserServiceTests, UserControllerTests (có Spring Security filter), BaseControllerTests, ApiResponseAdviceTests, GlobalExceptionHandlerTests, ServiceNameApiTests. Các test dùng mock repository/MVC, chưa chạy PostgreSQL hoặc HTTP giữa hai service thật. UserServiceApplicationTests mặc định cần hạ tầng DB nên không nằm trong lần chạy này.

11 test auth-service đã đạt khi kiểm tra riêng AuthServiceTests, UserApiClientTests và AuthServiceApplicationTests bằng POM tạm dưới target/login-verification để không sửa hoặc bỏ bộ test JWT cũ khỏi pom.xml chính. Lệnh mvn test đầy đủ hiện vẫn bị lỗi compile ở JwtServiceTests do constructor đã thay đổi.
