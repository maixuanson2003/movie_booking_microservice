# Kafka trong user-service

Cấu hình này cũng đã được áp dụng cho `auth-service`, `booking-service`,
`cinema-service`, `movie-service`, `payment-service`. Mỗi service có
`KafkaProducerConfig`, `KafkaConsumerConfig`, `BaseKafkaProducer` và
`BaseKafkaConsumer` trong package riêng. Consumer group mặc định là tên service;
khi đặt `KAFKA_CONSUMER_GROUP`, đặt riêng cho từng service nếu chúng cần nhận
độc lập cùng một sự kiện. Các replica của cùng service dùng chung group.

Các base chưa đăng ký topic hoặc listener nghiệp vụ. Dùng
`baseKafkaProducer.send(topic, key, payload)` để gửi; trong `@KafkaListener`, gọi
`baseKafkaConsumer.consume(record, service::handleEvent)` để xử lý payload.

## Cấu hình

- `KafkaProducerConfig`: tạo `ProducerFactory<String, Object>` và `KafkaTemplate<String, Object>`.
- `KafkaConsumerConfig`: tạo `ConsumerFactory<String, Map<String, Object>>` và `kafkaListenerContainerFactory`.
- `application.properties`: cấu hình broker, serializer, group và chế độ commit.

| Biến môi trường | Mặc định |
| --- | --- |
| KAFKA_BOOTSTRAP_SERVERS | localhost:9092 |
| KAFKA_CONSUMER_GROUP | user-service |
| KAFKA_CONSUMER_CONCURRENCY | 1 |

Khi chạy Docker, đặt bootstrap servers theo địa chỉ Kafka mà container truy cập được.
Broker cũng cần advertised listeners phù hợp với môi trường chạy client.

## Producer

Inject `KafkaTemplate<String, Object>` vào service rồi gửi một JSON object:

```java
return kafkaTemplate.send("user.events", userId.toString(),
        Map.of("eventType", "USER_UPDATED", "userId", userId));
```

`send()` trả CompletableFuture; xử lý kết quả hoặc lỗi của future để biết broker
đã nhận message hay chưa. Topic trong ví dụ cần được tạo trước khi sử dụng.
Chỉ gửi các trường sự kiện cần thiết, không gửi nguyên User chứa password.

Producer dùng JacksonJsonSerializer, `acks=all`, `enable.idempotence=true` và không
gửi Java type headers. Idempotence của producer không làm giao dịch database và
Kafka trở thành một transaction; chưa có cấu hình outbox hoặc transaction liên hệ thống.

## Consumer

Khai báo method sau trong một Spring bean khi đã có nghiệp vụ xử lý:

```java
@KafkaListener(topics = "user.events", containerFactory = "kafkaListenerContainerFactory")
public void consume(Map<String, Object> event) {
    // Gọi service xử lý nghiệp vụ. Để exception truyền ra nếu xử lý thất bại.
}
```

Consumer dùng ErrorHandlingDeserializer bọc JacksonJsonDeserializer. JSON được
đọc thành LinkedHashMap, bỏ qua type headers để không phụ thuộc tên package giữa
các service. Payload mong đợi là JSON object, không phải chuỗi hoặc mảng ở cấp gốc.

`enable-auto-commit=false`, `ack-mode=record`: container commit sau khi listener
xử lý thành công. `auto-offset-reset=earliest` chỉ áp dụng khi group chưa có offset hợp lệ.

Khi deserialize hoặc xử lý record lỗi, CommonContainerStoppingErrorHandler dừng
container thay vì bỏ qua record. Cần sửa nguyên nhân và khởi động lại listener
hoặc ứng dụng. Chưa cấu hình retry/DLT; consumer nghiệp vụ cần chịu được việc nhận lại message.

## Phạm vi kiểm tra

Đã biên dịch cấu hình với dependency Kafka của Spring Boot 4.1.1. Chưa kết nối
broker thực tế, chưa tạo topic hoặc listener nghiệp vụ.

## Tài liệu

- [Spring Kafka — JSON serialization](https://docs.spring.io/spring-kafka/reference/kafka/serdes.html)
- [Spring Boot 4 — KafkaProperties](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/kafka/autoconfigure/KafkaProperties.html)
