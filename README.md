# Hệ thống đặt vé xem phim — Java Spring Boot

Đã thêm [Auth Service](auth-service/README.md): đăng nhập OAuth2 + PKCE, cấp JWT và JWKS cho Gateway. Chạy cùng Gateway bằng `compose.gateway.yaml`.

Hướng dẫn triển khai API Gateway: [docs/API_GATEWAY.md](docs/API_GATEWAY.md).

Module Gateway đã tạo tại [api-gateway](api-gateway/README.md), kèm `compose.gateway.yaml` để chạy cùng Redis. Các phần bên dưới mô tả hạ tầng; Gateway có hướng dẫn chạy riêng ở liên kết trên.

Bộ hạ tầng Docker Compose phục vụ phát triển local. Đã có module API Gateway; mã nghiệp vụ và tích hợp cổng thanh toán vẫn cần triển khai. Compose cơ sở gồm PostgreSQL, Redis, Kafka và tác vụ tạo topic; thêm `compose.gateway.yaml` để chạy Gateway. Dockerfile dùng để build từng service Maven.

## Các module nghiệp vụ hiện có

| Module | Port | Phạm vi dữ liệu dự kiến |
| --- | --- | --- |
| [movie-service](movie-service/README.md) | 8082 | movies |
| [cinema-service](cinema-service/README.md) | 8083 | cinemas, rooms, seats, showtimes |
| [booking-service](booking-service/README.md) | 8085 | bookings, booking_items, combos |
| [payment-service](payment-service/README.md) | 8086 | invoices, payment_methods |

Bốn module nền dùng Java 21, Spring Boot 3.5.15, Web MVC, JPA, Validation, PostgreSQL Driver và Actuator. Tất cả kết nối database `movie_booking`; kiểm tra kết nối qua `GET /actuator/health` trên port mỗi service. Chưa có entity hoặc API nghiệp vụ; các route Gateway đã được chuẩn bị và sẽ trả 404 cho đến khi có controller tương ứng.

```powershell
docker compose up -d --build movie-service cinema-service booking-service payment-service
# Chạy cùng Auth, User và Gateway:
docker compose -f compose.yaml -f compose.gateway.yaml up -d --build
```

Gateway dùng Eureka với các route `lb://<service-name>`; xem [cấu hình và điều kiện chạy Eureka](api-gateway/README.md#eureka). Route ghế dùng Cinema; combo dùng Booking; invoice và payment-method dùng Payment.

## Database dùng chung và chạy các service hiện có

PostgreSQL tự tạo database `movie_booking`, tài khoản `movie_app` và 11 bảng từ `docker/postgres/schema.sql` khi khởi động với volume trống. Việc này diễn ra lúc **chạy container**, không phải lúc build image. `user-service` đợi PostgreSQL sẵn sàng rồi kết nối qua `postgres:5432/movie_booking`.

```powershell
# Chỉ copy khi chưa có .env, rồi sửa mật khẩu.
Copy-Item .env.example .env
# Build và chạy User + Auth + Gateway cùng hạ tầng:
docker compose -f compose.yaml -f compose.gateway.yaml up -d --build
# Kiểm tra danh sách bảng:
docker compose exec postgres psql -U movie_admin -d movie_booking -c '\dt'
```

Nếu đổi `POSTGRES_USER`, thay `movie_admin` trong lệnh kiểm tra. Auth vẫn dùng tài khoản demo trong bộ nhớ; Gateway không cần datasource. Các module nghiệp vụ mới dùng `config/application-local.example.yml`, cùng `DB_HOST=postgres`, `DB_NAME=movie_booking`, `DB_USER=movie_app`, `APP_DB_PASSWORD` và `depends_on.postgres.condition=service_healthy` như User service.

Schema giữ tên cột, độ dài, nullable và giá trị mặc định đã cung cấp; `datetime` được chuyển thành `timestamp` của PostgreSQL. Riêng quan hệ invoice → payment method dùng nhiều-một để nhiều hóa đơn cùng dùng Stripe/PayPal. Chưa bổ sung cơ chế chống đặt trùng ghế; cần triển khai ràng buộc và transaction nghiệp vụ trước khi sử dụng luồng booking thực tế.

**Nếu đã chạy cấu hình cũ:** init không chạy lại trên volume có dữ liệu. Sao lưu trước; có thể tạo database chung mà giữ nguyên các database cũ bằng lệnh dưới đây, chỉ chạy khi chưa có role `movie_app` và database `movie_booking`:

```powershell
docker compose up -d postgres
docker compose exec postgres sh /docker-entrypoint-initdb.d/01-init-databases.sh
docker compose -f compose.yaml -f compose.gateway.yaml up -d --build
```

Lệnh này tạo schema trống, không chuyển dữ liệu từ các database cũ. Không chạy `down -v` nếu cần giữ dữ liệu. Các thay đổi schema tiếp theo cần migration SQL có kiểm soát; sửa file init không cập nhật database đã tồn tại.

## 1. Kiến trúc

```text
Client → Gateway (Spring Cloud Gateway)
           ├── User         → movie_booking
           ├── Movie        → movie_booking
           ├── Theater      → movie_booking
           ├── Seat         → movie_booking       ↔ Redis
           ├── Booking      → movie_booking
           ├── Payment      → movie_booking
           └── Notification → movie_booking
                 Các service trao đổi sự kiện qua Kafka
```

Sơ đồ có 7 service nghiệp vụ và 1 Gateway. Các service nghiệp vụ dùng chung database `movie_booking`, schema `public` và tài khoản `movie_app`. Gateway không cần database mặc định.

| Thành phần | Vai trò |
| --- | --- |
| Java 21 + Spring Boot | API, nghiệp vụ, transaction trong mỗi service |
| PostgreSQL 17 | Dữ liệu bền vững, unique constraint, optimistic/pessimistic locking |
| Redis 7.4 | Distributed lock ngắn hạn, cache, TTL |
| Kafka 3.9.1, KRaft | Event bất đồng bộ, retry và Saga; không cần ZooKeeper |
| Spring Cloud Gateway | Routing và xác thực ở cửa vào; triển khai bằng Java sau |
| Flyway | Tắt ở các service; schema ban đầu do PostgreSQL init tạo |

Kafka chạy một broker/controller, replication factor 1, dành cho local. Các tag PostgreSQL/Redis và Java cập nhật trong dòng version; khi cần build tái lập hoàn toàn, pin image digest đã kiểm thử. Gateway cần Eureka Server và các backend đăng ký Eureka; xem api-gateway/README.md. Config Server chưa được tích hợp.

## 2. Chạy hạ tầng

Cần Docker Desktop chạy Linux containers và Docker Compose v2. Có thể bắt đầu với khoảng 4 GB RAM dành cho Docker, tăng thêm khi chạy nhiều service Java.

PowerShell tại thư mục project:

```powershell
Copy-Item .env.example .env
# Sửa các mật khẩu trong .env trước khi chạy.
docker compose config --quiet
docker compose up -d --wait postgres redis kafka
docker compose run --rm kafka-init
docker compose ps -a
```

Chỉ copy `.env.example` lần đầu, tránh ghi đè cấu hình đang dùng. Có thể dùng `docker compose up -d` để khởi động toàn bộ; `kafka-init` hoàn tất với exit code 0 là bình thường. Chờ tác vụ này xong trước khi chạy Java. Chạy lại tác vụ không tạo trùng topic.

```powershell
docker compose logs --tail=100 postgres redis kafka kafka-init
docker compose exec postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d postgres'
docker compose exec redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli ping'
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list
docker compose stop
docker compose start
docker compose down
```

`down` giữ dữ liệu trong named volumes. **`docker compose down -v` xóa toàn bộ dữ liệu local**, chỉ dùng khi chủ động reset. Script tạo database chỉ chạy khi volume PostgreSQL trống; sửa mật khẩu trong `.env` không tự đổi mật khẩu đã lưu trong PostgreSQL. Với dữ liệu cần giữ, đổi bằng SQL thay vì reset volume.

## 3. Địa chỉ kết nối

| Hạ tầng | Spring Boot chạy trong IDE trên máy | Spring Boot trong cùng Compose network |
| --- | --- | --- |
| PostgreSQL | localhost:5432 | postgres:5432 |
| Redis | localhost:6379 | redis:6379 |
| Kafka | localhost:9092 | kafka:29092 |

Port trên máy có thể đổi bằng `.env`. Nếu đổi, cập nhật cấu hình IDE tương ứng; port nội bộ container không đổi. Kafka có hai advertised listeners để client trong và ngoài Docker đều nhận đúng địa chỉ broker. Container Java không dùng `localhost` để kết nối container khác.

Database/user tương ứng: `movie_booking/movie_app`, `movie_booking/movie_app`, `movie_booking/movie_app`, `movie_booking/movie_app`, `movie_booking/movie_app`, `movie_booking/movie_app`, `movie_booking/movie_app`. Các user ứng dụng không phải superuser; dùng `APP_DB_PASSWORD` cho local. Tài khoản admin lấy từ `POSTGRES_USER`/`POSTGRES_PASSWORD`.

## 4. Kết nối Java Spring Boot

Tạo project Maven với Java 21 và một bản Spring Boot đang được hỗ trợ. Chọn Spring Cloud release train tương thích nếu dùng Gateway. Thêm các dependency theo nhu cầu của từng service:

- Spring Web, Validation, Actuator.
- Spring Data JPA, PostgreSQL Driver, Flyway Core và Flyway Database PostgreSQL.
- Spring Data Redis; có thể dùng Redisson cho distributed lock.
- Spring for Apache Kafka (`spring-kafka`).
- Spring Security/OAuth2 Resource Server cho API cần xác thực.

Dùng dependency management của Spring Boot cho thư viện được quản lý. Không cần thêm JPA/Redis/Kafka vào mọi service hoặc Gateway. Với Gateway WebFlux, tránh dùng thao tác JPA blocking trực tiếp.

Copy `config/application-local.example.yml` vào `src/main/resources/application-local.yml` của service. Mẫu dùng database chung; schema được tạo bởi `docker/postgres/schema.sql`. Giữ `ddl-auto=validate` và tắt Flyway ở service để tránh nhiều service cùng quản lý schema. Bỏ các phần cấu hình không dùng.

Ví dụ chạy Booking từ thư mục Maven của nó:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:APP_DB_PASSWORD = '<giống APP_DB_PASSWORD trong .env>'
$env:REDIS_PASSWORD = '<giống REDIS_PASSWORD trong .env>'
$env:SERVICE_NAME = 'booking-service'
$env:SERVER_PORT = '8085'
$env:DB_NAME = 'movie_booking'
$env:DB_USER = 'movie_app'
./mvnw.cmd spring-boot:run
```

`.env` được Docker Compose đọc, không tự được Spring Boot trong IDE đọc. Cấu hình environment variables trong IDE hoặc terminal. Gợi ý port: Gateway 8080, User 8081, Movie 8082, Theater 8083, Seat 8084, Booking 8085, Payment 8086, Notification 8087.

### Build service bằng Dockerfile

Dockerfile yêu cầu một module Maven có `pom.xml`, `src/`, Spring Boot Maven Plugin đóng gói executable JAR và đúng một JAR ứng dụng. Hiện có thể build User, Auth và Gateway; Movie, Cinema, Booking và Payment đã có module nền; Notification chưa có module. Với Maven multi-module, cần điều chỉnh build context và lệnh build theo cấu trúc thực tế.

Ví dụ sau khi tạo `booking-service/pom.xml` và `booking-service/src/`:

```powershell
docker build -f Dockerfile -t movie-booking/booking-service:local ./booking-service
```

Dockerfile dùng Java 21, runtime user không phải root. Bước build bỏ qua test; chạy `./mvnw.cmd verify` trước khi build hoặc trong CI.

Khi thêm service Java vào `compose.yaml`, có thể dùng cấu hình này dưới `services:`:

```yaml
  booking-service:
    build:
      context: ./booking-service
      dockerfile: ../Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: local
      SERVICE_NAME: booking-service
      DB_HOST: postgres
      DB_NAME: movie_booking
      DB_USER: movie_app
      APP_DB_PASSWORD: ${APP_DB_PASSWORD}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    ports:
      - "127.0.0.1:8085:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka-init:
        condition: service_completed_successfully
```

## 5. Chống hai người cùng đặt ghế A1

Ghế cần định danh theo **suất chiếu và ghế**, ví dụ `(showtime_id, seat_id)`, không chỉ `A1`. Seat service là nơi duy nhất thay đổi quyền giữ/bán ghế. Các cơ chế dưới đây cần được viết trong Java và migration; Compose chỉ cung cấp hạ tầng.

1. Client gửi yêu cầu với `Idempotency-Key`. Booking lưu khóa, user, hash nội dung và kết quả; đặt unique constraint cho khóa trong phạm vi user/operation. Cùng khóa khác nội dung phải bị từ chối; yêu cầu trùng đang xử lý trả trạng thái ổn định.
2. Seat có thể lấy Redis lock `lock:seat:{showtimeId}:{seatId}` bằng `SET key token NX PX ...`. Token phải riêng cho mỗi lần lấy lock. Giải phóng bằng Lua so sánh token rồi xóa, không dùng `DEL` mù. Lock chỉ bảo vệ thao tác ngắn, không giữ suốt thời gian thanh toán.
3. Trong PostgreSQL transaction, claim hàng ghế bằng `SELECT ... FOR UPDATE` rồi kiểm tra trạng thái/hạn giữ, hoặc dùng conditional UPDATE với `version` (`@Version`). Chỉ một claim được thành công; kiểm tra affected rows và xử lý optimistic lock conflict. Đặt unique/primary key trên `(showtime_id, seat_id)` trong bảng inventory của Seat.
4. Lưu `hold_id`, chủ sở hữu, `expires_at`, trạng thái HELD trong DB. Redis không phải nguồn xác nhận cuối cùng vì lock có thể hết TTL hoặc Redis khởi động lại. Mọi confirm/release đều kiểm tra đúng `hold_id`, trạng thái và hạn giữ trong DB transaction.
5. Booking tạo trạng thái PENDING_PAYMENT; giữ ghế ví dụ 5 phút bằng `expires_at`. Worker định kỳ tìm hold quá hạn, giải phóng có điều kiện. Không chỉ dựa vào Redis keyspace notification vì có thể mất sự kiện. Khi giữ nhiều ghế, khóa theo thứ tự cố định và claim tất cả trong một transaction của Seat.

`@Transactional` chỉ bao phủ database cục bộ; không tự gộp HTTP, Kafka, Redis và các database service thành một transaction. Cấu hình lock/query/HTTP timeout rõ ràng và retry có giới hạn cho lỗi tạm thời.

## 6. Kafka, Saga và payment rollback

Topic đã tạo: `booking.events`, `seat.events`, `payment.events`, `notification.events` và các topic `.dlt` tương ứng. Mỗi topic có 3 partition; chỉ bảo đảm thứ tự trong một partition. Dùng key theo aggregate cần giữ thứ tự, như bookingId cho booking/payment, hoặc showtimeId + seatId cho seat.

Luồng đề xuất: Booking điều phối Saga → giữ ghế → thanh toán → xác nhận ghế → xác nhận booking → Notification. Mỗi bước lưu trạng thái bền vững trước khi tiếp tục.

- Dùng **transactional outbox**: thay đổi dữ liệu và ghi event vào bảng outbox trong cùng transaction; publisher gửi Kafka sau commit. Publisher có thể gửi trùng nếu chết giữa lúc publish và đánh dấu đã gửi.
- Consumer dùng **inbox/processed_events** với unique `event_id`, ghi dấu xử lý và thay đổi dữ liệu trong cùng DB transaction. Commit offset sau khi xử lý thành công. Idempotent Kafka producer không thay thế idempotency nghiệp vụ.
- Thanh toán lỗi: chuyển booking sang thất bại/hủy và gửi yêu cầu nhả đúng hold. Nếu ghế đã được người khác giữ thì yêu cầu cũ không được nhả ghế của họ.
- Timeout thanh toán chưa chứng minh là thất bại. Lưu trạng thái cần đối soát, truy vấn nhà cung cấp bằng payment reference và retry với cùng idempotency key.
- Thanh toán thành công nhưng ghế hết hạn/không xác nhận được: thực hiện **refund/void bù trừ**, lưu REFUND_PENDING và retry cho tới khi có kết quả xác nhận. Không thể rollback giao dịch bên ngoài chỉ bằng rollback SQL.
- Webhook phải kiểm tra chữ ký và chống xử lý trùng; webhook đến muộn cũng phải đi qua state machine. Không giữ DB lock khi gọi cổng thanh toán.
- Cấu hình error handler, retry/backoff và dead-letter publishing trong Java; việc tạo `.dlt` chưa tự bật retry/DLT. Nếu chưa ghi được DLT, không được bỏ qua lỗi và commit offset tùy tiện.

## 7. Kịch bản kiểm chứng khi có API

- Gửi đồng thời nhiều yêu cầu khác khóa idempotency cho cùng suất chiếu/ghế: chỉ một hold còn hiệu lực thành công, các yêu cầu khác nhận lỗi xung đột.
- Gửi lại cùng yêu cầu/cùng khóa: chỉ một booking và một payment effect.
- Cho Redis lock hết TTL giữa thao tác: ràng buộc DB vẫn ngăn bán trùng.
- Dừng publisher sau khi gửi event nhưng trước khi đánh dấu outbox: consumer xử lý bản gửi lại không gây tác dụng lặp.
- Cho hold hết hạn đồng thời webhook thành công: hoặc xác nhận hợp lệ, hoặc refund; không lấy ghế đã thuộc hold khác.
- Mô phỏng refund lỗi rồi thành công: trạng thái cuối nhất quán, có lịch sử đối soát.

## 8. Phạm vi và xử lý lỗi local

Các port chỉ bind loopback máy host. Mật khẩu mẫu chỉ dùng local, không commit `.env`. Khi triển khai thật cần secret riêng, TLS/auth Kafka, replica/backup, quan sát hệ thống và giới hạn tài nguyên phù hợp. Chưa tích hợp cổng thanh toán hay SMTP; Notification có thể log khi học, sau đó bổ sung nhà cung cấp.

Nếu báo port bận: đổi port host trong `.env`. Nếu Kafka không kết nối từ IDE: kiểm tra `localhost:${KAFKA_PORT}` và advertised listener; từ Docker dùng `kafka:29092`. Nếu script PostgreSQL lỗi `\r`: lưu script với LF; `.gitattributes` đã thiết lập cho file `.sh`. Nếu Docker báo không đọc được `~/.docker/config.json` hoặc không kết nối daemon: kiểm tra quyền truy cập và Docker Desktop trước khi chạy lại.

## 9. Tài liệu chính thức

- [Kafka Docker 3.9.1](https://kafka.apache.org/39/getting-started/docker/)
- [Kafka broker configuration](https://kafka.apache.org/39/configuration/broker-configs/)
- [PostgreSQL official image và initialization scripts](https://hub.docker.com/_/postgres)
- [Spring Boot container images](https://docs.spring.io/spring-boot/reference/packaging/container-images/)


