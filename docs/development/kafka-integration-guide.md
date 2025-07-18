# Kafka Integration Guide

This guide provides developers with the necessary information to effectively integrate with Kafka in the MCP Debate System.

## 1. Overview

Kafka is used for asynchronous, event-driven communication between microservices, promoting decoupling and resilience.

## 2. Key Concepts

*   **Topics:** Named streams of records.
*   **Producers:** Applications that publish records to Kafka topics.
*   **Consumers:** Applications that subscribe to topics and process records.
*   **Consumer Groups:** A group of consumers that collectively consume from one or more topics.
*   **Schema Registry:** Centralized repository for managing schemas (e.g., Avro) for Kafka messages.

## 3. Spring Kafka Integration

Add the `spring-kafka` dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### 3.1. Configuration

Configure Kafka properties in `application.yml`:

```yaml
spring:
  kafka:
    producer:
      bootstrap-servers: my-kafka-cluster-kafka-bootstrap.kafka.svc:9092
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
      properties:
        schema.registry.url: http://schema-registry.kafka.svc:8081
    consumer:
      bootstrap-servers: my-kafka-cluster-kafka-bootstrap.kafka.svc:9092
      group-id: your-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      properties:
        schema.registry.url: http://schema-registry.kafka.svc:8081
        specific.avro.reader: true
    listener:
      ack-mode: RECORD
```

### 3.2. Producing Messages

Use `KafkaTemplate` to send messages:

```java
@Service
public class MyProducer {
    private final KafkaTemplate<String, MyAvroEvent> kafkaTemplate;

    public MyProducer(KafkaTemplate<String, MyAvroEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String key, MyAvroEvent event) {
        kafkaTemplate.send("my-topic", key, event);
    }
}
```

### 3.3. Consuming Messages

Use `@KafkaListener` to consume messages:

```java
@Component
public class MyConsumer {
    @KafkaListener(topics = "my-topic", groupId = "your-service-group")
    public void listen(MyAvroEvent event) {
        // Process event
        System.out.println("Received event: " + event);
    }
}
```

## 4. Event Design and Schema Evolution

*   **Avro Schemas:** Define your event schemas in Avro (`.avsc` files) and register them with the Schema Registry.
*   **Schema Evolution:** Understand forward and backward compatibility rules for schema changes.

## 5. Robust Event Processing

*   **Idempotency:** Ensure your consumers are idempotent. Track processed message IDs or use database unique constraints.
*   **Retries and DLQs:** Utilize Spring Kafka's `@RetryableTopic` for automatic retries and Dead Letter Queues (DLQs).

```java
@KafkaListener(topics = "my-topic", groupId = "your-service-group")
@RetryableTopic(attempts = "3", dltStrategy = DltStrategy.FAIL_ON_ERROR)
public void listenWithRetries(MyAvroEvent event) {
    // Logic that might fail transiently
}
```

## 6. Transactional Outbox Pattern

For critical operations, use the Transactional Outbox pattern to ensure atomicity between database writes and event publishing. Consider using Debezium for robust implementation.

## 7. Monitoring

Monitor Kafka metrics (consumer lag, message rates) via Prometheus and Grafana. Set up alerts for DLQs.
