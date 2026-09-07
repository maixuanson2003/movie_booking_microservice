# Auth-service: cấu trúc và ngữ cảnh phát triển

Cập nhật: 07/09/2026. Tài liệu này mô tả phiên bản sau khi tổ chức lại service theo ghi chú đã đọc trong cuộc trò chuyện. Khi bắt đầu thay đổi, workspace chỉ còn application class và application.properties trong src/main; các lớp nền được dựng lại từ Git, phần AuthService/JwtService/AuthInfo/AuthResponse được hoàn thiện từ ghi chú trước đó.

## Cập nhật login sau lần tổ chức ban đầu

Phần ghi chú bên dưới lưu trạng thái lúc tổ chức; thay đổi mới nhất của login được mô tả tại [user-service/LOGIN_API.md](../user-service/LOGIN_API.md), ưu tiên khi có khác biệt:

- UserApiClient lookup bằng GET `/api/users/username/{username}`; hàm getUser(id) đã được người dùng comment lại.
- User-service đã có UserController/UserService: lookup username và POST `/api/users/check-password` nhận `{username,password}`, kiểm tra BCrypt đúng tài khoản và status ACTIVE.
- AuthService.login gọi hai API, kiểm tra kết quả rồi tạo JWT; sai thông tin đăng nhập →UnauthorizedException. AuthResponse đã có Builder.
- JwtService đã được người dùng đổi về constructor không tham số, khóa SECRET hardcode và logic JWT bản nháp; các mô tả JWT dùng khóa inject/issuedAt hiện tại bên dưới là trạng thái cũ, không áp dụng cho source mới. JwtServiceTests vẫn dùng constructor cũ nên mvn test đầy đủ hiện không compile; lần thêm API user không sửa lại JWT.

## 1. Cấu trúc

```text
auth-service/
├── pom.xml
├── AUTH_SERVICE_CONTEXT.md
├── .mvn/wrapper/maven-wrapper.properties
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/example/auth_service/
    │   │   ├── AuthServiceApplication.java
    │   │   ├── config/        # Bean Spring, HTTP response, Kafka, WebClient
    │   │   ├── controller/    # Khung HTTP CRUD
    │   │   ├── service/       # Use case, JWT và khung persistence service
    │   │   ├── exception/     # Business/upstream exceptions và advice
    │   │   ├── model/         # Chỗ đặt entity tương lai
    │   │   ├── repository/    # Chỗ đặt repository tương lai
    │   │   └── sharedLogic/
    │   │       ├── dto/       # Dữ liệu trao đổi
    │   │       ├── mapper/    # Chuyển DTO ↔ entity
    │   │       ├── kafka/     # Transport helper
    │   │       └── webFlux/   # HTTP client tới user-service
    │   └── resources/application.properties
    └── test/java/com/example/auth_service/
        ├── AuthServiceApplicationTests.java
        ├── service/JwtServiceTests.java
        └── sharedLogic/webFlux/UserApiClientTests.java
```

Giữ tên package `sharedLogic` và `webFlux` theo cấu trúc đã ghi và quy ước hiện có của dự án. Mọi Java class trong src/main đều khai báo package khớp đường dẫn. Không có class trong default package.

## 2. Danh mục đủ 38 file src/main

Tiền tố `J/` bên dưới = `src/main/java/com/example/auth_service/`.

| File | Trách nhiệm |
|---|---|
| `J/AuthServiceApplication.java` | Entry point Spring Boot, component scan từ package gốc. |
| `J/config/ApiResponseAdvice.java` | Bọc HTTP body thành ApiResponse; giữ wrapper có sẵn; bỏ qua HEAD, 1xx, 204, 304; xử lý riêng String converter. |
| `J/config/KafkaConsumerConfig.java` | ConsumerFactory và listener factory; concurrency/ack theo properties; CommonContainerStoppingErrorHandler. |
| `J/config/KafkaProducerConfig.java` | ProducerFactory và KafkaTemplate<String,Object>. |
| `J/config/WebConfig.java` | Configuration placeholder, hiện rỗng. |
| `J/config/WebFluxConfig.java` | Bean WebClient với base URL user-service; dùng builder được Spring cung cấp hoặc fallback WebClient.builder() nếu chưa có bean. |
| `J/config/package-info.java` | Mô tả package cấu hình. |
| `J/controller/BaseController.java` | Abstract CRUD controller; cần subclass có RestController/RequestMapping. |
| `J/controller/package-info.java` | Mô tả HTTP layer. |
| `J/service/BaseService.java` | Generic JpaRepository CRUD; transaction cho updateAll/deleteById. |
| `J/service/AuthService.java` | Spring Service; gọi UserApiClient và map UserDTO thành AuthInfo. Không gọi WebClient trực tiếp. |
| `J/service/JwtService.java` | Component tạo JWT HS256, đọc claims, kiểm tra hạn/chữ ký và role. |
| `J/service/package-info.java` | Mô tả use cases. |
| `J/model/package-info.java` | Package placeholder, chưa có entity. |
| `J/repository/package-info.java` | Package placeholder, chưa có repository cụ thể. |
| `J/exception/BusinessException.java` | Abstract RuntimeException giữ HttpStatus. |
| `J/exception/BadRequestException.java` | 400. |
| `J/exception/UnauthorizedException.java` | 401. |
| `J/exception/ForbiddenException.java` | 403. |
| `J/exception/ResourceNotFoundException.java` | 404. |
| `J/exception/ConflictException.java` | 409. |
| `J/exception/UpstreamServiceException.java` | 502. |
| `J/exception/ServiceUnavailableException.java` | 503. |
| `J/exception/GatewayTimeoutException.java` | 504. |
| `J/exception/ExternalServiceException.java` | RuntimeException độc lập giữ status/message/data upstream. |
| `J/exception/GlobalExceptionHandler.java` | Chuẩn hóa lỗi MVC, business, validation, security tại controller và lỗi bất ngờ. |
| `J/exception/package-info.java` | Mô tả exception layer. |
| `J/sharedLogic/package-info.java` | Mô tả shared helpers. |
| `J/sharedLogic/dto/ApiResponse.java` | message:String, data:Object, success:boolean; constructor/getters. |
| `J/sharedLogic/dto/AuthInfo.java` | id:Long; username/email/fullName/role:String; Lombok constructors/getters/setters. |
| `J/sharedLogic/dto/AuthResponse.java` | token:String, timeLogin:LocalDateTime; Lombok constructors/getters/setters. |
| `J/sharedLogic/dto/UserDTO.java` | id, username, email, password, fullName, role, phone, avatar, status, createdAt, updatedAt. |
| `J/sharedLogic/mapper/BaseMapper.java` | Abstract toEntity/toDto. |
| `J/sharedLogic/kafka/BaseKafkaProducer.java` | send(topic,key,payload), overload không key; kiểm tra topic và trả CompletableFuture. |
| `J/sharedLogic/kafka/BaseKafkaConsumer.java` | Gọi handler với record.value, cho phép tombstone null; để lỗi lan lên listener container. |
| `J/sharedLogic/webFlux/BaseWebFlux.java` | Decode/unwrap ResponseFromWebFlux<T> bằng ParameterizedTypeReference; xử lý HTTP error, envelope rỗng/sai, timeout và lỗi kết nối. |
| `J/sharedLogic/webFlux/UserApiClient.java` | Validate input, gọi user API với URI variables dạng Map; truyền kiểu envelope UserDTO và timeout xuống BaseWebFlux; bỏ password. |
| `src/main/resources/application.properties` | Cấu hình runtime: service/DB/Eureka/user client/JWT/Kafka. |

## 3. Phân chia luồng xử lý

### User lookup

`AuthService.getAuthInfo(username)` → `UserApiClient.getUserByUsername(username)` → WebClient → BaseWebFlux decode `ResponseFromWebFlux<UserDTO>` (message, data, success) → kiểm tra success/data → xóa password khỏi DTO → map AuthInfo.

- getUser(id): GET `/api/users/{id}`, ID phải >0.
- getUserByUsername(username): GET `/api/users/find_user_name/{username}`, username không được null/trắng.
- Body rỗng, JSON không decode được, success không true hoặc data null: UpstreamServiceException 502.
- Timeout: GatewayTimeoutException 504; lỗi kết nối: ServiceUnavailableException 503.
- HTTP upstream 4xx/5xx: ExternalServiceException giữ status/message/data. BaseWebFlux đọc getSuccess/getData trên envelope generic, trả payload T; UserApiClient cung cấp ParameterizedTypeReference<ResponseFromWebFlux<UserDTO>>. Timeout được inject qua constructor base; overload cũ dùng mặc định 5 giây. Hai lời gọi uri dùng Map.of với tên biến để chọn overload Map rõ ràng.

Đường dẫn username là contract đã ghi trong tài liệu trước, **chưa có controller cụ thể triển khai trong user-service hiện tại**. Không coi lookup đã được kiểm thử tích hợp. Hàm checkPassword(password) qua GET URL trong bản nháp trước chưa được dựng lại: cần xác định contract xác thực có danh tính user trước, không đưa mật khẩu vào URL.

### JWT

- Khóa đọc từ `auth.jwt.secret`/JWT_SECRET, Base64 của ít nhất 32 byte ngẫu nhiên; không có secret hardcode trong production source.
- Hạn mặc định 30 phút, phải dương. issuedAt dùng thời điểm hiện tại.
- Subject = username; claim auth chứa id/username/email/fullName/role; claim role cấp cao dùng cùng giá trị để validRoleUser đọc nhất quán.
- isTokenValid kiểm tra chữ ký qua JJWT và expiration hiện tại; token lỗi/hết hạn/rỗng trả false.
- validRoleUser kiểm tra token và đối chiếu role không phân biệt hoa thường; role array null trả false.
- getAllClaim/extractClaims trả Claims, ném lỗi khi token không hợp lệ. extractClaims là instance method để dùng khóa được inject.
- Chưa có HTTP login/register, JWT filter, refresh/revoke/logout hoặc OAuth2/JWKS endpoint. AuthService.getAuthInfo chỉ lấy danh tính, không xác thực mật khẩu và không cấp token tự động.

### CRUD và HTTP response

BaseController có POST create (201), PUT /{id}, PUT /batch, GET all, GET /{id}, DELETE /{id} (200). Lớp abstract không tự expose route. BaseService dùng save/saveAll/findAll/findById/existsById/delete; findById thiếu dữ liệu →404; updateAll chặn list rỗng/null/chứa null.

Response chung `{message,data,success}`. Advice không bọc lặp ApiResponse; body String được serialize JSON; Content-Length cũ bị xóa khi bọc. GlobalExceptionHandler xử lý BusinessException theo status, validation map field/object → lỗi đầu tiên, vi phạm constraint DB →409, AuthenticationException →401, AccessDeniedException →403; lỗi bất ngờ log và trả 500. Helper đặt EXCEPTION_HANDLED request attribute.

Giữ nguyên các hạn chế của khung CRUD: update đơn chưa gắn ID đường dẫn vào entity; phần lớn endpoint trả entity, chỉ batch update map về DTO; chưa có @Valid. ExternalServiceException chưa có handler riêng, nếu tới GlobalExceptionHandler sẽ vào nhánh 500. Đây là phần nghiệp vụ cần hoàn thiện riêng trước khi expose API, không phải luồng đăng nhập đã hoàn thành.

### Kafka

Producer JSON, key String, acks=all, idempotence=true, không gắn type header. Consumer decode JSON thành LinkedHashMap, ErrorHandlingDeserializer bọc key/value delegate, không auto-commit, earliest, ack RECORD. Handler lỗi làm dừng container, cần xử lý nguyên nhân rồi khởi động lại. Chưa có @KafkaListener nghiệp vụ, auth topic hoặc DLT publisher.

## 4. Config và dependency

pom.xml: Java 21; Spring Boot 4.1.1; Spring Cloud BOM 2025.1.3; JJWT 0.11.5 (api compile, impl/jackson runtime). Các starter: MVC, WebFlux, WebSocket, Security, JPA, Kafka, Eureka client, loadbalancer; PostgreSQL runtime, Lombok, DevTools; test starters tương ứng. Maven compiler dùng Lombok annotation processor cho main/test; Spring Boot plugin đóng gói.

| Biến môi trường | Mặc định / ý nghĩa |
|---|---|
| SERVER_PORT | 9000 |
| EUREKA_SERVER_URL | http://localhost:8761/eureka/ |
| DB_HOST / DB_PORT / DB_NAME | localhost / 5432 / movie_booking |
| DB_USER | movie_app |
| APP_DB_PASSWORD | Bắt buộc cho cấu hình datasource |
| USER_SERVICE_URL | http://localhost:8081 |
| USER_SERVICE_TIMEOUT | 5s |
| JWT_SECRET | Bắt buộc, Base64 key >=32 byte; không dùng khóa mẫu test khi chạy thật |
| JWT_EXPIRATION | 30m |
| KAFKA_BOOTSTRAP_SERVERS | localhost:9092 |
| KAFKA_CONSUMER_GROUP | auth-service |
| KAFKA_CONSUMER_CONCURRENCY | 1 |

spring.application.name=auth-service; Eureka prefer-ip-address/fetch-registry/register-with-eureka=true. JDBC URL ghép từ DB_HOST/PORT/NAME. JPA open-in-view=false, ddl-auto=validate; spring.sql.init.mode=never. WebClient dùng base URL trực tiếp, chưa @LoadBalanced.

Kafka properties: producer key StringSerializer, value JacksonJsonSerializer; consumer key/value ErrorHandlingDeserializer với delegate StringDeserializer/JacksonJsonDeserializer; spring.json.value.default.type=java.util.LinkedHashMap, spring.json.use.type.headers=false. Xem application.properties để lấy đầy đủ tên property.

### File cấu hình hỗ trợ

- .mvn/wrapper/maven-wrapper.properties: Wrapper 3.3.4, only-script, Maven 3.9.16. mvnw/mvnw.cmd là script launcher.
- .gitignore bỏ build output/IDE metadata; .gitattributes đặt LF cho mvnw, CRLF cho cmd; .vscode chỉ có marker Spring Initializr. HELP.md là tài liệu Initializr.
- ../Dockerfile: Maven/Java 21 build, package -DskipTests, runtime Java 21 Alpine user spring, java -jar.
- ../compose.yaml: PostgreSQL, Redis, Kafka, business services; Kafka nội bộ kafka:29092, ngoài host localhost:9092; auto-create topics tắt. Kafka-init tạo booking/seat/payment/notification events và dlt, không có auth topic.
- ../compose.gateway.yaml: auth-service port 9000, volume signing key và AUTH_* theo cấu hình cũ; gateway chờ /oauth2/jwks, trong khi source này chưa có endpoint đó.
- ../.env.example: mẫu biến PostgreSQL/Redis/Kafka/AUTH/JWT; chưa có JWT_SECRET cho implementation HS256 này.
- ../config/application-local.example.yml: template chung, chưa được load trong auth; default tên booking-service/port 8080, Kafka StringSerializer khác auth.

Docker chưa đồng bộ với source: cần truyền APP_DB_PASSWORD, DB_HOST=postgres, USER_SERVICE_URL=http://user-service:8081, KAFKA_BOOTSTRAP_SERVERS=kafka:29092 và JWT_SECRET cho auth. Các AUTH_USERNAME/PASSWORD/KEY_FILE/REDIRECT_URI/ALLOWED_ORIGIN và JWT_ISSUER_URI trong compose chưa được source auth sử dụng. Biến trong .env không tự truyền hết vào container. Việc tổ chức source lần này chưa thay đổi Compose hoặc gateway.

## 5. Kiểm chứng và cách chạy

Kết quả ngày 07/09/2026: biên dịch 37 file Java thành công; **10 test chạy, 0 failures, 0 errors, 0 skipped** (1 context, 3 JWT, 6 WebClient). Đã clean build khi phát hiện output class cũ bị thiếu, sau đó sửa các lỗi test phát hiện và chạy lại toàn bộ bộ test thành công. Báo cáo tại `target/surefire-reports/` (build artifact, không commit).

Từ thư mục auth-service:

```powershell
mvn compile
mvn test
```

Có thể thay mvn bằng `./mvnw.cmd`. Môi trường làm việc này dùng Maven cache của workspace qua `-Dmaven.repo.local=.../.maven-cache`.

- AuthServiceApplicationTests: smoke test khởi tạo bean, tắt Eureka và loại datasource auto-configuration chỉ trong test; khóa JWT test riêng; không chứng minh tích hợp PostgreSQL/Kafka.
- UserApiClientTests: mock HTTP exchange, kiểm tra envelope/password, 404 rỗng/JSON, timeout, input không hợp lệ, envelope rỗng/sai, lookup username.
- JwtServiceTests: kiểm tra identity/issuedAt/TTL/role, token hết hạn/sai chữ ký/malformed, cấu hình khóa yếu và TTL không dương.

Khi chạy ứng dụng thật cần cung cấp JWT_SECRET, APP_DB_PASSWORD và địa chỉ hạ tầng phù hợp, sau đó dùng `mvn spring-boot:run`. Biên dịch thành công không có nghĩa login hay gateway JWT integration đã hoạt động; các endpoint đó chưa được triển khai.
