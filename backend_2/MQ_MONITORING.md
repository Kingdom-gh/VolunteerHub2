# RabbitMQ — Cấu hình hiện tại và mục cần giám sát

Tài liệu này mô tả cấu hình RabbitMQ hiện tại trong repo và danh sách các chỉ số / cảnh báo nên theo dõi vận hành.

## Vị trí cấu hình
- File cấu hình Spring AMQP: `backend_2/src/main/java/com/example/backend/messaging/RabbitConfig.java`

## Các exchange / queue / routing key hiện có
- Exchange chính: `volunteer.request.exchange` (TopicExchange, durable)
- Routing key chính: `volunteer.request.created`
- Main queue: `volunteer.request.queue` (durable)

Retry / dead-letter setup (đã thêm):
- Retry exchange: `volunteer.request.retry.exchange` (durable)
- Retry queue 1: `volunteer.request.retry.queue.1`
  - `x-message-ttl` = 5000 ms (5s)
  - `x-dead-letter-exchange` = `volunteer.request.retry.exchange`
  - `x-dead-letter-routing-key` = `volunteer.request.retry.2`
- Retry queue 2: `volunteer.request.retry.queue.2`
  - `x-message-ttl` = 30000 ms (30s)
  - `x-dead-letter-exchange` = `volunteer.request.exchange` (main)
  - `x-dead-letter-routing-key` = `volunteer.request.created`

- Dead-letter exchange (DLX): `volunteer.request.dlx` (durable)
- Dead-letter queue (DLQ): `volunteer.request.dlq` (durable), bound to DLX with `#`

Behavior tổng quan:
- Khi message bị dead-letter trên main queue sẽ gửi tới `RETRY_EXCHANGE` với routing key `retry.1` → message vào `retryQueue1` và bị giữ 5s → sau TTL sẽ dead-letter tới `retry.2` → vào `retryQueue2` giữ 30s → sau TTL sẽ dead-letter về `volunteer.request.exchange` và quay lại main queue.
- Ngoài ra listener container có `RetryOperationsInterceptor` (stateless) cài `maxAttempts=3` với exponential backoff (500ms initial, multiplier 2.0, max 5000ms). Sau vượt maxAttempts, interceptor gọi recoverer để publish message sang `DLX` → `volunteer.request.dlq`.

> Lưu ý: chain TTL queues cung cấp delay/backoff giữa lần gửi lại; interceptor đảm bảo message được đưa vào DLQ khi vượt threshold retry.

## Tính chất lưu trữ
- Các queue đã tạo là durable (durable = true) → queue tồn tại qua restart broker.
- Mức độ persistence của từng message phụ thuộc vào `MessageProperties.deliveryMode` (persistent vs transient). Nên đảm bảo khi publish thiết lập message persistent nếu muốn giữ message qua restart.

## Hành vi consumer liên quan MQ (current code)
- Consumer: `backend_2/src/main/java/com/example/backend/messaging/VolunteerRequestConsumer.java`.
- Khi consumer xử lý message:
  - Nếu message invalid (thiếu trường) → consumer `return` (ACK) và message bị loại bỏ.
  - Nếu `post` không tồn tại → consumer log warn và `return` (ACK) — hiện hành vi là DROP (business decision đã áp dụng).
  - Nếu `volunteer` không tồn tại → consumer log warn và `return` (ACK) — DROP.
  - Nếu consumer ném exception khi save DB → container/interceptor sẽ retry; nếu vẫn fail sau maxAttempts → message sẽ được gửi vào DLQ.

## Các thông tin & chỉ số cần giám sát (Monitoring checklist)
1. Queue-level metrics:
   - `messages_ready` (số message đang chờ deliver) trên:
     - `volunteer.request.queue`
     - `volunteer.request.retry.queue.1`
     - `volunteer.request.retry.queue.2`
     - `volunteer.request.dlq`
   - `messages_unacknowledged` (unacked) trên main queue — cảnh báo khi lớn
   - Message publish rate / deliver rate / redeliver rate
2. DLQ metrics:
   - Số message trong `volunteer.request.dlq` (tăng đột biến → cảnh báo)
   - `x-death` headers (số lần death per message) — dùng để debug
3. Consumer metrics:
   - Consumer count / consumer connections for queue
   - Consumer processing errors / exception rate
   - Average processing time per message
4. Broker health:
   - RabbitMQ node availability (up/down)
   - File descriptor and socket usage
   - Memory usage & memory alarms
   - Disk alarm (disk free space) — RabbitMQ blocks publishers when disk alarm
   - Connection counts / channels
5. Infra metrics:
   - CPU / Memory usage of RabbitMQ container
   - Disk IO and network IO
6. Application-side metrics & logs:
   - Backend consumer/error log lines (stack traces) — correlate with message timestamps
   - Publish failures in service (exceptions from `rabbitTemplate.convertAndSend`)
   - CircuitBreaker state & counts (Resilience4j metrics): open/closed counts for `volunteerRequestService`

## Suggested alerting rules (examples)
- `volunteer.request.dlq` size > 10 → PagerDuty / Slack alert (indicates persistent failures)
- `messages_unacknowledged` on main queue > 100 → warn (consumer slow or stuck)
- Publish failure rate > 1% over 5m → warn (broker connectivity problems)
- RabbitMQ memory alarm or node down → critical
- Consumer error rate > threshold (e.g., 5 errors/min) → warn

## How to inspect quickly
- RabbitMQ Management UI: `http://localhost:15672` (guest/guest). Xem tab `Queues` và `Exchanges`.
- rabbitmqctl (inside rabbit container):
  - `docker exec -it volunteerhub-rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged` 
  - `docker exec -it volunteerhub-rabbitmq rabbitmqctl list_bindings`
- Inspect message headers (x-death): open queue messages in Management UI and click a message.

## Recommended operational actions
1. Ensure publishers set message persistent when message must survive broker restart.
2. Add DLQ consumer or admin endpoint to review `volunteer.request.dlq` and reprocess or store to `volunteer_request_backlog` table.
3. Add idempotency at consumer (unique constraint on `volunteer_post_id + volunteer_email` or equivalent) to prevent duplicates.
4. Expose Prometheus metrics (RabbitMQ exporter) and configure alerts (DLQ size, unacked, memory/disk alarms).
5. Add structured logs for all consumer failures including `postId`, `volunteerEmail`, message timestamp, and exception.

## Quick next-steps (implementation pointers)
- To make messages persistent on publish: set delivery mode persistent or configure `RabbitTemplate` default. Example when publishing:
  ```java
  rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, message, m -> {
      m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
      return m;
  });
  ```
- To reprocess DLQ messages: create a small admin endpoint that reads `volunteer.request.dlq` via RabbitMQ API or add a DLQ consumer that stores to DB table for manual review.

---
File này mô tả trạng thái hệ thống tại thời điểm chỉnh sửa mã nguồn. Nếu bạn muốn, tôi có thể thêm:
- một consumer để tiêu thụ DLQ và lưu `volunteer_request_backlog` (patch), hoặc
- một file Playbook ngắn để xử lý DLQ (manual steps + curl/examples).
