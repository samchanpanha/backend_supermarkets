20. Payment Service
20.1 Module Structure
text
payment-service/
├── pom.xml
├── Dockerfile
└── src/main/java/com/supermarket/payment/
    ├── PaymentApplication.java
    ├── entity/
    │   ├── PaymentTransaction.java
    │   └── RefundTransaction.java
    ├── repository/
    │   ├── PaymentRepository.java
    │   └── RefundRepository.java
    ├── controller/
    │   └── PaymentController.java
    ├── service/
    │   ├── PaymentService.java
    │   └── gateway/
    │       ├── PaymentGateway.java
    │       ├── StripePaymentGateway.java (mock)
    │       └── PaymentGatewayFactory.java
    ├── dto/
    │   ├── PaymentRequest.java
    │   ├── PaymentResponse.java
    │   ├── RefundRequest.java
    │   └── RefundResponse.java
    ├── event/
    │   ├── PaymentEventPublisher.java
    │   └── PaymentEvent.java
    ├── filter/
    │   └── TenantFilter.java
    └── util/
        └── TenantContext.java
20.2 Dependencies (pom.xml)
xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.supermarket</groupId>
        <artifactId>supermarket-pos-saas</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>payment-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.supermarket</groupId>
            <artifactId>common-library</artifactId>
            <version>1.0.0</version>
        </dependency>
        <!-- For Stripe simulation (optional) -->
        <dependency>
            <groupId>com.stripe</groupId>
            <artifactId>stripe-java</artifactId>
            <version>22.0.0</version>
        </dependency>
    </dependencies>
</project>
20.3 Main Application
PaymentApplication.java

java
package com.supermarket.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
20.4 Tenant Context and Filter (same as before)
20.5 Entities
entity/PaymentTransaction.java

java
package com.supermarket.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String orderId;       // reference to order (orderNumber or ID)

    @Column(nullable = false)
    private String transactionId; // from gateway

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String paymentMethod; // CARD, CASH, etc.

    @Column(nullable = false)
    private String status;        // SUCCESS, FAILED, PENDING, REFUNDED

    private String gatewayResponse;
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
entity/RefundTransaction.java

java
package com.supermarket.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_transactions")
@Data
@NoArgsConstructor
public class RefundTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private Long paymentId;        // reference to PaymentTransaction

    @Column(nullable = false)
    private String refundId;       // from gateway

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;         // SUCCESS, FAILED

    private String reason;

    private String gatewayResponse;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
20.6 Repositories
repository/PaymentRepository.java

java
package com.supermarket.payment.repository;

import com.supermarket.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTenantIdAndOrderId(String tenantId, String orderId);
}
repository/RefundRepository.java

java
package com.supermarket.payment.repository;

import com.supermarket.payment.entity.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RefundRepository extends JpaRepository<RefundTransaction, Long> {
    List<RefundTransaction> findByTenantIdAndPaymentId(String tenantId, Long paymentId);
}
20.7 DTOs
dto/PaymentRequest.java

java
package com.supermarket.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private String orderId;           // order identifier
    private BigDecimal amount;
    private String currency;           // e.g., "USD"
    private String paymentMethod;      // CARD, CASH
    private String cardToken;          // for card payments
    private String description;
    private String customerId;         // optional
}
dto/PaymentResponse.java

java
package com.supermarket.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private boolean success;
    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String message;
    private String errorCode;
}
dto/RefundRequest.java

java
package com.supermarket.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundRequest {
    private String transactionId;       // original payment transaction ID
    private BigDecimal amount;          // if partial refund
    private String reason;
}
dto/RefundResponse.java

java
package com.supermarket.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private boolean success;
    private String refundId;
    private String status;
    private String message;
}
20.8 Payment Gateway Abstraction
service/gateway/PaymentGateway.java

java
package com.supermarket.payment.service.gateway;

import com.supermarket.payment.dto.PaymentRequest;
import com.supermarket.payment.dto.PaymentResponse;
import com.supermarket.payment.dto.RefundRequest;
import com.supermarket.payment.dto.RefundResponse;

public interface PaymentGateway {
    PaymentResponse processPayment(PaymentRequest request);
    RefundResponse processRefund(RefundRequest request);
    PaymentResponse getPaymentStatus(String transactionId);
}
service/gateway/StripePaymentGateway.java (mock implementation for development)

java
package com.supermarket.payment.service.gateway;

import com.supermarket.payment.dto.PaymentRequest;
import com.supermarket.payment.dto.PaymentResponse;
import com.supermarket.payment.dto.RefundRequest;
import com.supermarket.payment.dto.RefundResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StripePaymentGateway implements PaymentGateway {

    @Value("${payment.gateway.stripe.api-key:sk_test_mock}")
    private String apiKey;

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        // Simulate payment processing
        boolean success = Math.random() > 0.1; // 90% success rate for mock
        if (success) {
            return new PaymentResponse(
                    true,
                    "ch_" + UUID.randomUUID().toString().replace("-", ""),
                    "SUCCESS",
                    request.getAmount(),
                    "Payment processed successfully",
                    null
            );
        } else {
            return new PaymentResponse(
                    false,
                    null,
                    "FAILED",
                    request.getAmount(),
                    "Payment failed",
                    "INSUFFICIENT_FUNDS"
            );
        }
    }

    @Override
    public RefundResponse processRefund(RefundRequest request) {
        // Simulate refund
        boolean success = true;
        if (success) {
            return new RefundResponse(
                    true,
                    "re_" + UUID.randomUUID().toString().replace("-", ""),
                    "SUCCESS",
                    "Refund processed"
            );
        } else {
            return new RefundResponse(
                    false,
                    null,
                    "FAILED",
                    "Refund failed"
            );
        }
    }

    @Override
    public PaymentResponse getPaymentStatus(String transactionId) {
        // Mock: assume always success if transactionId exists
        return new PaymentResponse(
                true,
                transactionId,
                "SUCCESS",
                null,
                "Payment found",
                null
        );
    }
}
service/gateway/PaymentGatewayFactory.java

java
package com.supermarket.payment.service.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentGatewayFactory {

    @Autowired
    private Map<String, PaymentGateway> gateways;

    public PaymentGateway getGateway(String method) {
        // method could be "stripe", "paypal", etc.
        String key = method.toLowerCase() + "PaymentGateway";
        PaymentGateway gateway = gateways.get(key);
        if (gateway == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + method);
        }
        return gateway;
    }
}
20.9 Event Publishing
event/PaymentEvent.java

java
package com.supermarket.payment.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String eventType; // PAYMENT_SUCCEEDED, PAYMENT_FAILED, REFUND_SUCCEEDED
    private String tenantId;
    private String orderId;
    private String transactionId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
event/PaymentEventPublisher.java

java
package com.supermarket.payment.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {
    private static final String TOPIC = "payment-events";

    @Autowired
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void publishPaymentEvent(PaymentEvent event) {
        kafkaTemplate.send(TOPIC, event.getTenantId(), event);
    }
}
20.10 Service Layer
service/PaymentService.java

java
package com.supermarket.payment.service;

import com.supermarket.payment.dto.PaymentRequest;
import com.supermarket.payment.dto.PaymentResponse;
import com.supermarket.payment.dto.RefundRequest;
import com.supermarket.payment.dto.RefundResponse;
import com.supermarket.payment.entity.PaymentTransaction;
import com.supermarket.payment.entity.RefundTransaction;
import com.supermarket.payment.event.PaymentEvent;
import com.supermarket.payment.event.PaymentEventPublisher;
import com.supermarket.payment.repository.PaymentRepository;
import com.supermarket.payment.repository.RefundRepository;
import com.supermarket.payment.service.gateway.PaymentGateway;
import com.supermarket.payment.service.gateway.PaymentGatewayFactory;
import com.supermarket.payment.util.TenantContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private PaymentGatewayFactory gatewayFactory;

    @Autowired
    private PaymentEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        String tenantId = TenantContext.getCurrentTenant();

        // Get appropriate gateway based on payment method
        PaymentGateway gateway = gatewayFactory.getGateway(request.getPaymentMethod());

        // Process via gateway
        PaymentResponse gatewayResponse = gateway.processPayment(request);

        // Save transaction
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTenantId(tenantId);
        transaction.setOrderId(request.getOrderId());
        transaction.setTransactionId(gatewayResponse.getTransactionId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setStatus(gatewayResponse.getStatus());
        transaction.setGatewayResponse(gatewayResponse.getMessage());
        transaction.setErrorMessage(gatewayResponse.getErrorCode());
        transaction.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(transaction);

        // Publish event
        PaymentEvent event = new PaymentEvent(
                gatewayResponse.isSuccess() ? "PAYMENT_SUCCEEDED" : "PAYMENT_FAILED",
                tenantId,
                request.getOrderId(),
                gatewayResponse.getTransactionId(),
                request.getAmount(),
                LocalDateTime.now()
        );
        eventPublisher.publishPaymentEvent(event);

        return gatewayResponse;
    }

    @Transactional
    public RefundResponse processRefund(RefundRequest request) {
        String tenantId = TenantContext.getCurrentTenant();

        // Find original payment
        PaymentTransaction payment = paymentRepository.findByTenantIdAndOrderId(tenantId, request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Get gateway
        PaymentGateway gateway = gatewayFactory.getGateway(payment.getPaymentMethod());

        // Process refund
        RefundResponse gatewayResponse = gateway.processRefund(request);

        // Save refund
        RefundTransaction refund = new RefundTransaction();
        refund.setTenantId(tenantId);
        refund.setPaymentId(payment.getId());
        refund.setRefundId(gatewayResponse.getRefundId());
        refund.setAmount(request.getAmount() != null ? request.getAmount() : payment.getAmount());
        refund.setStatus(gatewayResponse.getStatus());
        refund.setReason(request.getReason());
        refund.setGatewayResponse(gatewayResponse.getMessage());
        refund.setCreatedAt(LocalDateTime.now());
        refundRepository.save(refund);

        // Update payment status if fully refunded
        // (simplified: assume full refund for now)
        payment.setStatus("REFUNDED");
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Publish event
        PaymentEvent event = new PaymentEvent(
                "REFUND_SUCCEEDED",
                tenantId,
                payment.getOrderId(),
                payment.getTransactionId(),
                refund.getAmount(),
                LocalDateTime.now()
        );
        eventPublisher.publishPaymentEvent(event);

        return gatewayResponse;
    }

    public PaymentTransaction getPaymentByOrderId(String orderId) {
        String tenantId = TenantContext.getCurrentTenant();
        return paymentRepository.findByTenantIdAndOrderId(tenantId, orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }
}
20.11 REST Controller
controller/PaymentController.java

java
package com.supermarket.payment.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.payment.dto.PaymentRequest;
import com.supermarket.payment.dto.PaymentResponse;
import com.supermarket.payment.dto.RefundRequest;
import com.supermarket.payment.dto.RefundResponse;
import com.supermarket.payment.entity.PaymentTransaction;
import com.supermarket.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> processRefund(@RequestBody RefundRequest request) {
        RefundResponse response = paymentService.processRefund(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentTransaction>> getPaymentByOrder(@PathVariable String orderId) {
        PaymentTransaction payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }
}
20.12 Configuration
bootstrap.yml

yaml
spring:
  application:
    name: payment-service
  cloud:
    config:
      uri: http://config-service:8888
      fail-fast: true
Dockerfile

dockerfile
FROM openjdk:17-jdk-slim
COPY target/payment-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
20.13 Config Repository Update
Add payment-service.yml to ~/supermarket-config-repo/:

yaml
server:
  port: 8086

payment:
  gateway:
    stripe:
      api-key: sk_test_mock_key

spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
Also ensure Kafka is configured correctly in shared config.

20.14 Update Docker Compose
Add payment-service to docker-compose.yml:

yaml
payment-service:
  build: ./payment-service
  container_name: payment-service
  ports:
    - "8086:8086"
  depends_on:
    - postgres
    - service-discovery
    - config-service
    - kafka
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - SPRING_CONFIG_IMPORT=configserver:http://config-service:8888
  networks:
    - supermarket-network
Update API Gateway routes:

java
.route("payment-service", r -> r
    .path("/api/payments/**")
    .filters(f -> f.rewritePath("/api/payments/(?<segment>.*)", "/payments/${segment}")
            .filter(new AuthenticationFilter().apply(new AuthenticationFilter.Config())))
    .uri("lb://PAYMENT-SERVICE"))
20.15 Integration with Order Service
To automatically update order payment status, we need an Order Service Kafka consumer listening to payment-events. Let's add that to Order Service.

In Order Service (add consumer):

event/PaymentEventConsumer.java

java
package com.supermarket.order.event;

import com.supermarket.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventConsumer {

    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received payment event: {}", event);
        // For now, we just update payment status; in a real scenario, we'd have tenant context
        // We might need to set tenant context from event if needed.
        try {
            if ("PAYMENT_SUCCEEDED".equals(event.getEventType())) {
                orderService.confirmPayment(Long.valueOf(event.getOrderId()), "PAID");
            } else if ("PAYMENT_FAILED".equals(event.getEventType())) {
                // Handle failure (maybe mark order as failed)
            }
        } catch (Exception e) {
            log.error("Error processing payment event", e);
        }
    }
}
Add dependency in order-service pom for Kafka if not already.

Also add PaymentEvent class in order-service matching the one in payment-service (or share via common-library). For simplicity, we can copy the class.

Add to order-service bootstrap.yml the consumer config:

yaml
spring:
  kafka:
    consumer:
      group-id: order-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
20.16 Testing Payment Service
Rebuild:

bash
mvn clean package
docker-compose up --build
Get JWT token.

Process a payment:

text
POST http://localhost:8080/api/payments/process
Authorization: Bearer <token>
{
  "orderId": "ORD-12345678",
  "amount": 50.00,
  "currency": "USD",
  "paymentMethod": "stripe",
  "cardToken": "tok_visa"
}
Get payment by order ID:

text
GET http://localhost:8080/api/payments/order/ORD-12345678
Process refund (use transactionId from payment response):

text
POST http://localhost:8080/api/payments/refund
{
  "transactionId": "ORD-12345678",
  "amount": 50.00,
  "reason": "Customer request"
}
Check that Order Service received the event and updated order status.

21. Next Steps
Now we have:

Phase 11: Payment Service (complete)

Phase 8: Order Service (updated with consumer)

Phase 9: POS Service (complete)

Phase 5-6: Product & Inventory (complete)

Phase 3-4: Auth & Tenant (complete)

Infrastructure (complete)

Remaining:

Phase 7: Customer Service (optional but recommended)

Phase 10: E-commerce Service (frontend store)

Phase 12: Loyalty Service

Phase 13-14: Analytics & Reporting

Phase 15+ cross-cutting concerns, testing, deployment

Given that we have most core business services, we could now build E-commerce Service (Phase 10) which will provide a storefront API for customers to browse products, manage cart, and place orders (using Order and Payment services). That's a natural next step to complete the "Supermarket POS SaaS" with both in-store (POS) and online (e-commerce) channels.

Shall I proceed with E-commerce Service?