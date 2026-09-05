# API Gateway cho hệ thống đặt vé — Java Spring Boot

**Auth Service đã triển khai:** [hướng dẫn đăng nhập và lấy JWT](../auth-service/README.md). Auth chạy trực tiếp port 9000; Gateway dùng JWKS nội bộ khi chạy Docker. Các đoạn bên dưới nói chưa có Authorization Server là mô tả giai đoạn trước khi bổ sung Auth.

**Cập nhật:** mã nguồn đã được tạo trong `api-gateway/`, dùng Boot 3.5.15 và Cloud 2025.0.3. Chạy theo [README của module](../api-gateway/README.md); Docker dùng file bổ sung `compose.gateway.yaml`. Các bước tạo file bên dưới là tài liệu giải thích, không cần tạo lại. Authorization Server và các API backend vẫn cần triển khai riêng.

## 1. Gateway làm gì?

API Gateway là ứng dụng Java riêng, chạy port **8080**, nhận request từ frontend rồi chuyển tới service phù hợp. Dùng **Spring Cloud Gateway Server WebFlux** cho project này.

```text
Frontend
   │ GET /api/movies hoặc POST /api/bookings
   ▼
API Gateway :8080
   ├── Kiểm tra JWT và quyền truy cập tuyến API
   ├── Áp dụng CORS, giới hạn request, timeout
   └── Route tới service
          ├── User         :8081
          ├── Movie        :8082
          ├── Theater      :8083
          ├── Seat         :8084
          ├── Booking      :8085
          ├── Payment      :8086
          └── Notification :8087
```

Gateway không giữ ghế, trừ tiền, truy vấn database nghiệp vụ hay điều phối Saga. Booking/Seat/Payment thực hiện những phần đó. Gateway không cần PostgreSQL hoặc Kafka; Redis chỉ cần khi bật rate limiting.

Tài liệu này cung cấp các đoạn mã để tạo Gateway. Repository hiện chưa có project Java, Authorization Server hoặc API backend chạy sẵn.

## 2. Tạo project Maven

Tạo thư mục `api-gateway` với Java 21, Maven và Spring Boot. Ví dụ trong tài liệu nhắm tới **Spring Boot 3.5.x + Spring Cloud 2025.0.x + Gateway 4.3.x**; chọn bản vá phù hợp trong cùng dòng, không ghép tùy ý các major version. Đây là cặp tương thích được Spring công bố trong [thông báo Spring Cloud 2025.0](https://spring.io/blog/2025/05/29/spring-cloud-2025-0-0-is-abvailable/). Kiểm tra [bảng phiên bản](https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions) khi tạo project mới.

Có thể tạo project bằng Spring Initializr, chọn Gateway, OAuth2 Resource Server, Spring Data Reactive Redis và Actuator. Giữ parent Spring Boot và BOM Spring Cloud do công cụ sinh ra. Với POM tự viết, import `org.springframework.cloud:spring-cloud-dependencies` trong `dependencyManagement`, đặt version bản vá 2025.0.x đã chọn, `type=pom`, `scope=import`.

Các dependency cần có trong phần `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Thêm `spring-boot-maven-plugin` vào build plugins để tạo executable JAR cho Dockerfile hiện có. Không thêm `spring-boot-starter-web`, Gateway MVC hoặc JPA vào Gateway WebFlux này. Không gọi `.block()` hoặc chạy JDBC trong filter.

```text
api-gateway/
├── pom.xml
├── mvnw.cmd
└── src/main/
    ├── java/com/example/gateway/
    │   ├── GatewayApplication.java
    │   ├── SecurityConfig.java
    │   └── RateLimitConfig.java
    └── resources/application.yml
```

`GatewayApplication.java`:

```java
package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

## 3. Route tới 7 service

Quy ước: backend giữ nguyên đường dẫn `/api/...`. Ví dụ Gateway nhận `GET /api/movies/42` thì Movie nhận đúng `GET /api/movies/42`. Vì vậy **không thêm StripPrefix**. Nếu controller backend dùng `/movies`, phải thống nhất lại và thêm `StripPrefix=1` ở route đó.

Tạo `api-gateway/src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI}
          jwk-set-uri: ${JWT_JWK_SET_URI}
          audiences: movie-booking-api
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      timeout: 2s
  cloud:
    gateway:
      server:
        webflux:
          httpclient:
            connect-timeout: 2000
            response-timeout: 5s
          default-filters:
            - RemoveRequestHeader=X-User-Id
            - RemoveRequestHeader=X-User-Roles
          routes:
            - id: user-service
              uri: lb://user-service
              predicates:
                - Path=/api/users,/api/users/**
            - id: movie-service
              uri: lb://movie-service
              predicates:
                - Path=/api/movies,/api/movies/**
            - id: theater-service
              uri: lb://cinema-service
              predicates:
                - Path=/api/theaters,/api/theaters/**,/api/showtimes,/api/showtimes/**
            - id: seat-service
              uri: lb://cinema-service
              predicates:
                - Path=/api/seats,/api/seats/**
            - id: booking-service
              uri: lb://booking-service
              predicates:
                - Path=/api/bookings,/api/bookings/**
              filters:
                - name: RequestRateLimiter
                  args:
                    key-resolver: "#{@userKeyResolver}"
                    redis-rate-limiter.replenishRate: 5
                    redis-rate-limiter.burstCapacity: 10
                    redis-rate-limiter.requestedTokens: 1
            - id: payment-service
              uri: lb://payment-service
              predicates:
                - Path=/api/payments,/api/payments/**
            - id: notification-service
              uri: lb://notification-service
              predicates:
                - Path=/api/notifications,/api/notifications/**

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

Dùng prefix cấu hình `spring.cloud.gateway.server.webflux` cho dòng Gateway này; tránh trộn ví dụ từ các phiên bản cũ. Các route Notification chỉ dành cho đọc thông báo của người dùng; thao tác gửi thông báo nội bộ không public qua Gateway.

Timeout kết nối ở trên là 2.000 ms; timeout phản hồi là Duration 5 giây. Các đơn vị timeout được mô tả trong [tài liệu Gateway](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/http-timeouts-configuration.html). Đây là giá trị khởi đầu cho local, cần điều chỉnh theo API.

## 4. JWT và phân quyền

Frontend lấy **access token** từ Authorization Server rồi gửi `Authorization: Bearer <token>`. Gateway kiểm tra chữ ký bằng JWKS, issuer, hạn dùng và audience. Authorization Server chưa có trong Compose hiện tại: cần triển khai hoặc sử dụng một hệ thống cấp token trước khi thử API có bảo vệ. User service quản lý hồ sơ không mặc nhiên trở thành Authorization Server.

`JWT_ISSUER_URI` phải khớp chính xác claim `iss`; `JWT_JWK_SET_URI` phải là URL JWKS mà Gateway truy cập được. Trong Docker, issuer có thể vẫn là URL public trong token, trong khi JWKS dùng URL nội bộ. JWKS chỉ công bố khóa công khai. Xem [Spring Security JWT](https://docs.spring.io/spring-security/reference/7.0/reactive/oauth2/resource-server/jwt.html) để hiểu cơ chế issuer/JWKS; mã dưới đây dùng API Security tương ứng Boot 3.5.

Tạo `SecurityConfig.java`:

```java
package com.example.gateway;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(Customizer.withDefaults())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange(auth -> auth
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .pathMatchers(HttpMethod.GET,
                    "/api/movies", "/api/movies/**",
                    "/api/theaters", "/api/theaters/**",
                    "/api/showtimes", "/api/showtimes/**",
                    "/api/seats", "/api/seats/**").permitAll()
                .pathMatchers("/api/movies", "/api/movies/**",
                    "/api/theaters", "/api/theaters/**",
                    "/api/showtimes", "/api/showtimes/**")
                    .hasAuthority("SCOPE_catalog.write")
                .pathMatchers("/api/users", "/api/users/**",
                    "/api/bookings", "/api/bookings/**",
                    "/api/payments", "/api/payments/**",
                    "/api/notifications", "/api/notifications/**")
                    .authenticated()
                .anyExchange().denyAll())
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
            .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("http://localhost:3000"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
```

Mẫu sử dụng bearer token trong header, không dùng cookie đăng nhập; đó là lý do tắt CSRF. Nếu chuyển sang phiên đăng nhập/cookie, phải thiết kế lại CSRF và credentials. CORS được cấu hình tại Security để preflight được xử lý trước yêu cầu xác thực; không cần khai báo thêm một cấu hình CORS trùng tại Gateway hoặc backend.

Các GET catalog/seat chỉ được public nếu dữ liệu không chứa thông tin riêng tư. Token có scope `catalog.write` mới được sửa catalog. Các thay đổi ghế từ client đang bị chặn: Booking gọi Seat qua API nội bộ đã xác thực. Backend vẫn phải kiểm tra quyền cụ thể và quyền sở hữu tài nguyên; `authenticated()` không cho phép đọc booking của người khác, tự gán quyền admin hoặc tự đánh dấu thanh toán thành công.

Gateway chuyển tiếp header Authorization đến backend trong luồng proxy này. Mỗi backend nên tự xác thực JWT; lấy user từ `sub`, không tin `X-User-Id` hoặc userId trong body. `TokenRelay` không cần cho trường hợp chuyển tiếp bearer token sẵn có này; nó phục vụ thêm các luồng OAuth2 Client.

## 5. Rate limiting bằng Redis hiện có

Tạo `RateLimitConfig.java`:

```java
package com.example.gateway;

import java.security.Principal;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {
    @Bean
    KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal().map(Principal::getName);
    }
}
```

Trong cấu hình JWT mặc định, tên principal lấy từ `sub`. Không lấy khóa giới hạn từ query `userId` do client tự khai báo. Route Booking dùng bucket theo người dùng: nạp 5 token/giây, sức chứa 10, mỗi request tốn 1 token. Vượt giới hạn trả HTTP 429; đây là token bucket, không phải đếm cố định theo từng giây. Xem [RequestRateLimiter](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html).

Mẫu chỉ bật limiter cho Booking có xác thực. API public/login cần policy khác, thường giới hạn ở ingress theo IP đáng tin cậy. Không tin trực tiếp `X-Forwarded-For` từ Internet. Redis limiter không thay thế khóa ghế hoặc idempotency. Cần thử hành vi khi Redis lỗi; không mặc định coi limiter là hàng rào từ chối mọi request khi Redis mất kết nối.

## 6. Chạy local

Từ thư mục gốc, chạy Redis theo Compose hiện có:

```powershell
docker compose up -d --wait redis
```

Từ thư mục `api-gateway`, đặt biến và chạy Maven:

```powershell
$env:REDIS_PASSWORD = '<REDIS_PASSWORD trong .env>'
$env:JWT_ISSUER_URI = '<issuer thực tế của Authorization Server>'
$env:JWT_JWK_SET_URI = '<URL JWKS thực tế>'
./mvnw.cmd spring-boot:run
```

Nếu đã đổi Redis port trên máy, đặt thêm `$env:REDIS_PORT`. Không copy cấu hình JPA trong `config/application-local.example.yml` vào Gateway; dùng `application.yml` ở tài liệu này. Các placeholder issuer/JWKS phải được thay thế; không phải URL chạy sẵn.

Chạy backend muốn thử trên port ở mục 1. Nếu chưa có backend, route chưa trả được dữ liệu dù Gateway đã chạy.

## 7. Chạy Gateway trong Docker

Sau khi tạo project Java, thêm đoạn sau dưới `services:` của `compose.yaml`. Đây là hướng dẫn, chưa được thêm vào Compose hiện tại:

```yaml
  api-gateway:
    build:
      context: ./api-gateway
      dockerfile: ../Dockerfile
    ports:
      - "127.0.0.1:8080:8080"
    environment:
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_ISSUER_URI: ${JWT_ISSUER_URI}
      JWT_JWK_SET_URI: ${JWT_JWK_SET_URI}
      EUREKA_SERVER_URL: http://host.docker.internal:8761/eureka/
    depends_on:
      redis:
        condition: service_healthy
```

Đoạn trên giả định đã thêm các container backend cùng network và chúng nghe **port nội bộ 8080**. Nếu backend dùng port nội bộ khác, đổi URL tương ứng. Nếu backend vẫn chạy IDE trên Windows, dùng `http://host.docker.internal:8081` ... `:8087` thay cho tên service và kiểm tra backend có thể được Docker truy cập.

Thêm `JWT_ISSUER_URI`/`JWT_JWK_SET_URI` thật vào `.env`, rồi chạy từ thư mục gốc:

```powershell
docker compose config --quiet
docker compose up -d --build api-gateway
docker compose logs -f api-gateway
```

Gateway hiện dùng Eureka Client và Spring Cloud LoadBalancer. Cần Eureka Server riêng và các backend đăng ký cùng registry; xem [hướng dẫn Eureka](../api-gateway/README.md#eureka). Cấu hình thực tế nằm trong `api-gateway/src/main/resources/application.yml`, bao gồm `eureka.client.service-url.defaultZone` lấy từ `EUREKA_SERVER_URL`.

## 8. Luồng đặt vé qua Gateway

```text
POST /api/bookings
Authorization: Bearer <access-token>
Idempotency-Key: <khóa riêng cho lần đặt>
         │
Gateway: JWT → quyền tuyến API → rate limit → chuyển tiếp
         │
Booking: kiểm tra dữ liệu + idempotency → gọi Seat giữ ghế
         │
Seat: transaction/constraint xác định ai giữ được ghế
         │
Booking trả kết quả → Gateway trả lại client
```

Gateway giữ nguyên `Idempotency-Key`; Booking phải lưu và xử lý nó. **Không cấu hình Retry toàn cục cho POST booking/payment**: tự gửi lại có thể tạo tác dụng phụ. Khi timeout, thao tác backend có thể vẫn đã commit; client dùng khóa cũ để truy vấn/thử lại theo hợp đồng API, không tự tạo khóa mới.

Thanh toán dài nên trả trạng thái đang xử lý rồi frontend truy vấn trạng thái. Nếu dùng circuit breaker, không trả kết quả giả thành công cho việc đặt ghế/thanh toán.

Webhook nhà cung cấp thanh toán cần tuyến riêng theo hợp đồng tích hợp. Cấu hình hiện tại yêu cầu JWT trên toàn bộ Payment, nên webhook không mang JWT sẽ nhận 401. Khi triển khai, chỉ permit đúng HTTP method/path webhook cần thiết, đặt rule trước rule Payment; Payment kiểm tra chữ ký trên raw body, thời gian và event ID. Không mở public toàn bộ `/api/payments/**`.

## 9. Kiểm tra sau khi triển khai

```powershell
curl.exe -i http://localhost:8080/actuator/health
curl.exe -i http://localhost:8080/api/movies
curl.exe -i http://localhost:8080/api/bookings
$env:ACCESS_TOKEN = '<access token thực tế>'
curl.exe -i -H "Authorization: Bearer $env:ACCESS_TOKEN" http://localhost:8080/api/bookings
curl.exe -i -X OPTIONS http://localhost:8080/api/bookings -H "Origin: http://localhost:3000" -H "Access-Control-Request-Method: POST" -H "Access-Control-Request-Headers: authorization,content-type,idempotency-key"
```

| Trường hợp | Kết quả cần kiểm chứng |
| --- | --- |
| GET movies không token, Movie đang chạy | Kết quả backend đúng đường dẫn |
| GET bookings không token/hết hạn/sai chữ ký/audience | 401 |
| Sửa catalog không có scope cần thiết | 403 |
| Truy cập booking người khác với JWT hợp lệ | Backend từ chối theo quyền sở hữu |
| Nhiều request Booking cùng user vượt bucket | 429 khi Redis hoạt động |
| Preflight origin được phép | Có CORS headers phù hợp, không bị 401 |
| Origin khác | Không có quyền CORS cho origin đó |
| Backend dừng hoặc timeout | Lỗi được trả rõ ràng, không trả booking thành công giả |
| Gửi lặp POST cùng Idempotency-Key | Một booking ở backend |

Tài liệu và cấu hình mẫu chưa được compile hoặc kiểm thử end-to-end vì repository chưa có mã Gateway, backend và hệ thống cấp JWT. Khi tạo ứng dụng, chạy `./mvnw.cmd verify`, thử routing bằng backend thật hoặc stub, rồi kiểm tra các trường hợp trên.

## 10. Các lỗi hay gặp

- **404**: controller backend không dùng prefix `/api`, route sai đường dẫn hoặc cấu hình prefix của phiên bản Gateway không khớp.
- **401 dù đã có token**: sai issuer/audience, token hết hạn, JWKS không truy cập được hoặc dùng ID token thay cho access token.
- **403 khi sửa catalog**: thiếu scope `catalog.write`; claim role tùy biến không tự thành scope.
- **CORS lỗi nhưng curl gọi được**: origin khác `http://localhost:3000`, hoặc backend cũng thêm CORS gây header trùng.
- **Docker không gọi được backend**: dùng localhost bên trong Gateway container hoặc backend chưa tham gia cùng network.
- **Gateway không khởi động đúng WebFlux**: vô tình thêm starter MVC/Servlet hoặc ghép Boot/Cloud không tương thích.

Khi triển khai, chỉ Gateway/ingress nên nhận lưu lượng public; backend kiểm tra token và quyền nghiệp vụ. Dùng TLS và không ghi Authorization/token vào log.
