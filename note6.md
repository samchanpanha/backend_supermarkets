We'll implement the **Payment Service** (Phase 11) with Stripe integration. This service will handle payment processing, refunds, and webhooks, and publish events for other services to consume.

---

## 25. Payment Service

### 25.1 Module Structure
```
payment-service/
├── pom.xml
├── Dockerfile
└── src/main/java/com/supermarket/payment/
    ├── PaymentApplication.java
    ├── entity/
    │   ├── Payment.java
    │   └── Refund.java
    ├── repository/
    │   ├── PaymentRepository.java
    │   └── RefundRepository.java
    ├── controller/
    │   └── PaymentController.java
    ├── service/
    │   ├── PaymentService.java
    │   └── StripeService.java
    ├── event/
    │   ├── PaymentEventPublisher.java
    │   └── PaymentProcessedEvent.java
    ├── dto/
    │   ├── PaymentRequest.java
    │   ├── PaymentResponse.java
    │   └── RefundRequest.java
    ├── filter/
    │   └── TenantFilter.java
    └── util/
        └── TenantContext.java
```

### 25.2 Dependencies (`pom.xml`)

```xml
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
            <groupId>com.stripe</groupId>
            <artifactId>stripe-java</artifactId>
            <version>24.0.0</version>
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
    </dependencies>
</project>
```

### 25.3 Main Application

**`PaymentApplication.java`**
```java
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
```

### 25.4 Tenant Context and Filter (same as before)

### 25.5 Entities

**`entity/Payment.java`**
```java
package com.supermarket.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String orderId;       // reference to order service order ID

    @Column(nullable = false)
    private String paymentIntentId; // Stripe payment intent ID

    @Column(nullable = false)
    private BigDecimal amount;

    private String currency;       // default USD

    @Column(nullable = false)
    private String status;          // succeeded, pending, failed, refunded

    private String paymentMethod;   // card, cash, etc.

    private String customerId;      // Stripe customer ID (optional)

    private String receiptUrl;      // Stripe receipt URL

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String errorMessage;    // if failed
}
```

**`entity/Refund.java`**
```java
package com.supermarket.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Data
@NoArgsConstructor
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private Long paymentId;

    private String refundId;        // Stripe refund ID

    @Column(nullable = false)
    private BigDecimal amount;

    private String reason;

    private String status;          // succeeded, pending, failed

    private LocalDateTime createdAt;
}
```

### 25.6 Repositories

**`repository/PaymentRepository.java`**
```java
package com.supermarket.payment.repository;

import com.supermarket.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTenantIdAndPaymentIntentId(String tenantId, String paymentIntentId);
    List<Payment> findByTenantIdAndOrderId(String tenantId, String orderId);
}
```

**`repository/RefundRepository.java`**
```java
package com.supermarket.payment.repository;

import com.supermarket.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByTenantIdAndPaymentId(String tenantId, Long paymentId);
}
```

### 25.7 Event Publishing

**`event/PaymentProcessedEvent.java`**
```java
package com.supermarket.payment.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private String tenantId;
    private String orderId;
    private Long paymentId;
    private String paymentIntentId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime timestamp;
}
```

**`event/PaymentEventPublisher.java`**
```java
package com.supermarket.payment.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {
    private static final String TOPIC = "payment-events";

    @Autowired
    private KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate;

    public void publishPaymentEvent(PaymentProcessedEvent event) {
        kafkaTemplate.send(TOPIC, event.getTenantId(), event);
    }
}
```

### 25.8 DTOs

**`dto/PaymentRequest.java`**
```java
package com.supermarket.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private String orderId;
    private BigDecimal amount;
    private String currency = "usd";
    private String paymentMethod;      // card, cash (for POS)
    private String paymentMethodId;    // Stripe token or payment method ID (for card)
    private String customerId;         // Stripe customer ID (optional)
    private String description;
    private String storeId;             // for context
}
```

**`dto/PaymentResponse.java`**
```java
package com.supermarket.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private String paymentIntentId;
    private String clientSecret;       // for 3D Secure, if needed
    private String status;
    private BigDecimal amount;
    private String receiptUrl;
}
```

**`dto/RefundRequest.java`**
```java
package com.supermarket.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundRequest {
    private Long paymentId;
    private BigDecimal amount;
    private String reason;
}
```

### 25.9 Stripe Service

**`service/StripeService.java`**
```java
package com.supermarket.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public PaymentIntent createPaymentIntent(Long amount, String currency, String paymentMethodId,
                                             String customerId, String description) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount) // in cents
                .setCurrency(currency)
                .setPaymentMethod(paymentMethodId)
                .setCustomer(customerId)
                .setDescription(description)
                .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                .setConfirm(true)
                .build();
        return PaymentIntent.create(params);
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    public Refund createRefund(String paymentIntentId, Long amount) throws StripeException {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(amount)
                .build();
        return Refund.create(params);
    }
}
```

### 25.10 Payment Service

**`service/PaymentService.java`**
```java
package com.supermarket.payment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.supermarket.payment.dto.PaymentRequest;
import com.supermarket.payment.dto.PaymentResponse;
import com.supermarket.payment.dto.RefundRequest;
import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.event.PaymentEventPublisher;
import com.supermarket.payment.event.PaymentProcessedEvent;
import com.supermarket.payment.repository.PaymentRepository;
import com.supermarket.payment.repository.RefundRepository;
import com.supermarket.payment.util.TenantContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private StripeService stripeService;

    @Autowired
    private PaymentEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) throws StripeException {
        String tenantId = TenantContext.getCurrentTenant();

        // For cash payments, we don't go to Stripe
        if ("cash".equalsIgnoreCase(request.getPaymentMethod())) {
            return processCashPayment(request, tenantId);
        }

        // Stripe payment
        long amountInCents = request.getAmount().multiply(new BigDecimal("100")).longValue();
        PaymentIntent intent = stripeService.createPaymentIntent(
                amountInCents,
                request.getCurrency(),
                request.getPaymentMethodId(),
                request.getCustomerId(),
                request.getDescription()
        );

        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setOrderId(request.getOrderId());
        payment.setPaymentIntentId(intent.getId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setStatus(intent.getStatus());
        payment.setPaymentMethod("card");
        payment.setCustomerId(request.getCustomerId());
        payment.setReceiptUrl(intent.getCharges().getData().isEmpty() ? null :
                intent.getCharges().getData().get(0).getReceiptUrl());
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Publish event
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                tenantId,
                request.getOrderId(),
                payment.getId(),
                intent.getId(),
                request.getAmount(),
                intent.getStatus(),
                LocalDateTime.now()
        );
        eventPublisher.publishPaymentEvent(event);

        return new PaymentResponse(
                payment.getId(),
                intent.getId(),
                intent.getClientSecret(),
                intent.getStatus(),
                request.getAmount(),
                payment.getReceiptUrl()
        );
    }

    private PaymentResponse processCashPayment(PaymentRequest request, String tenantId) {
        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setOrderId(request.getOrderId());
        payment.setPaymentIntentId("CASH-" + System.currentTimeMillis());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setStatus("succeeded");
        payment.setPaymentMethod("cash");
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Publish event
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                tenantId,
                request.getOrderId(),
                payment.getId(),
                payment.getPaymentIntentId(),
                request.getAmount(),
                "succeeded",
                LocalDateTime.now()
        );
        eventPublisher.publishPaymentEvent(event);

        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentIntentId(),
                null,
                "succeeded",
                request.getAmount(),
                null
        );
    }

    @Transactional
    public PaymentResponse refundPayment(RefundRequest request) throws StripeException {
        String tenantId = TenantContext.getCurrentTenant();
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }

        // For cash payments, just update status
        if ("cash".equals(payment.getPaymentMethod())) {
            payment.setStatus("refunded");
            paymentRepository.save(payment);

            com.supermarket.payment.entity.Refund refund = new com.supermarket.payment.entity.Refund();
            refund.setTenantId(tenantId);
            refund.setPaymentId(payment.getId());
            refund.setAmount(request.getAmount());
            refund.setReason(request.getReason());
            refund.setStatus("succeeded");
            refund.setCreatedAt(LocalDateTime.now());
            refundRepository.save(refund);

            // Publish event
            PaymentProcessedEvent event = new PaymentProcessedEvent(
                    tenantId,
                    payment.getOrderId(),
                    payment.getId(),
                    payment.getPaymentIntentId(),
                    request.getAmount().negate(),
                    "refunded",
                    LocalDateTime.now()
            );
            eventPublisher.publishPaymentEvent(event);

            return new PaymentResponse(
                    payment.getId(),
                    payment.getPaymentIntentId(),
                    null,
                    "refunded",
                    request.getAmount(),
                    null
            );
        }

        // Stripe refund
        long amountInCents = request.getAmount().multiply(new BigDecimal("100")).longValue();
        Refund stripeRefund = stripeService.createRefund(payment.getPaymentIntentId(), amountInCents);

        com.supermarket.payment.entity.Refund refund = new com.supermarket.payment.entity.Refund();
        refund.setTenantId(tenantId);
        refund.setPaymentId(payment.getId());
        refund.setRefundId(stripeRefund.getId());
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason());
        refund.setStatus(stripeRefund.getStatus());
        refund.setCreatedAt(LocalDateTime.now());
        refundRepository.save(refund);

        payment.setStatus("refunded");
        paymentRepository.save(payment);

        // Publish event
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                tenantId,
                payment.getOrderId(),
                payment.getId(),
                payment.getPaymentIntentId(),
                request.getAmount().negate(),
                "refunded",
                LocalDateTime.now()
        );
        eventPublisher.publishPaymentEvent(event);

        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentIntentId(),
                null,
                "refunded",
                request.getAmount(),
                null
        );
    }

    public Payment getPayment(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        if (!payment.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }
        return payment;
    }
}
```

### 25.11 REST Controller

**`controller/PaymentController.java`**
```java
package com.supermarket.payment.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.supermarket.common.dto.ApiResponse;
import com.supermarket.payment.dto.PaymentRequest;
import com.supermarket.payment.dto.PaymentResponse;
import com.supermarket.payment.dto.RefundRequest;
import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.repository.PaymentRepository;
import com.supermarket.payment.service.PaymentService;
import com.supermarket.payment.util.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@RequestBody PaymentRequest request) throws StripeException {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(@RequestBody RefundRequest request) throws StripeException {
        PaymentResponse response = paymentService.refundPayment(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Payment>> getPayment(@PathVariable Long id) {
        Payment payment = paymentService.getPayment(id);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<Payment>>> getPaymentsByOrder(@PathVariable String orderId) {
        String tenantId = TenantContext.getCurrentTenant();
        List<Payment> payments = paymentRepository.findByTenantIdAndOrderId(tenantId, orderId);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    // Webhook endpoint for Stripe events
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @RequestHeader("Stripe-Signature") String sigHeader) {
        // In production, verify signature with your webhook secret
        // For simplicity, we just parse the event
        try {
            Event event = Event.GSON.fromJson(payload, Event.class);
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject().orElse(null);
                    if (intent != null) {
                        // Update payment status
                        paymentRepository.findByTenantIdAndPaymentIntentId("default", intent.getId())
                                .ifPresent(payment -> {
                                    payment.setStatus("succeeded");
                                    paymentRepository.save(payment);
                                });
                    }
                    break;
                // handle other events
                default:
                    // ignore
            }
            return ResponseEntity.ok("Received");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error");
        }
    }
}
```

### 25.12 Configuration

**`bootstrap.yml`**
```yaml
spring:
  application:
    name: payment-service
  cloud:
    config:
      uri: http://config-service:8888
      fail-fast: true
```

**`application.yml` in config repo (`payment-service.yml`)**:
```yaml
server:
  port: 8086

stripe:
  api:
    key: ${STRIPE_API_KEY:sk_test_your_test_key_here}   # Set via environment variable

spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

**`Dockerfile`**
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/payment-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 25.13 Update Docker Compose

Add payment-service to `docker-compose.yml`:

```yaml
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
    - STRIPE_API_KEY=${STRIPE_API_KEY}  # set in .env or shell
  networks:
    - supermarket-network
```

### 25.14 Update API Gateway Routes

Add route for payment service in `GatewayConfig.java`:

```java
.route("payment-service", r -> r
    .path("/api/payments/**")
    .filters(f -> f.rewritePath("/api/payments/(?<segment>.*)", "/payments/${segment}")
            .filter(new AuthenticationFilter().apply(new AuthenticationFilter.Config())))
    .uri("lb://PAYMENT-SERVICE"))
```

Note: The webhook endpoint should be public, so we might need to exclude it from authentication filter. We can either add a separate route without filter or modify the AuthenticationFilter to skip certain paths. For simplicity, we'll add a separate route without filter before the authenticated route.

In GatewayConfig, add:

```java
.route("payment-webhook", r -> r
    .path("/api/payments/webhook")
    .filters(f -> f.rewritePath("/api/payments/webhook", "/payments/webhook"))
    .uri("lb://PAYMENT-SERVICE"))
```

And keep the authenticated route for other paths (but ensure webhook is matched first because order matters). We'll put the webhook route before the general payment route.

### 25.15 Testing Payment Service

1. Set Stripe test key as environment variable:
   ```bash
   export STRIPE_API_KEY=sk_test_your_key
   ```

2. Rebuild and run:
   ```bash
   mvn clean package
   docker-compose up --build
   ```

3. Get JWT token.

4. Process a card payment (using Stripe test token: `pm_card_visa`):
   ```
   POST http://localhost:8080/api/payments/process
   Authorization: Bearer <token>
   {
     "orderId": "ORD-123",
     "amount": 25.50,
     "paymentMethod": "card",
     "paymentMethodId": "pm_card_visa",
     "description": "Order payment"
   }
   ```

5. Process a cash payment:
   ```
   POST http://localhost:8080/api/payments/process
   {
     "orderId": "ORD-124",
     "amount": 15.00,
     "paymentMethod": "cash"
   }
   ```

6. Refund:
   ```
   POST http://localhost:8080/api/payments/refund
   {
     "paymentId": 1,
     "amount": 10.00,
     "reason": "Customer returned item"
   }
   ```

7. Check payment by order:
   ```
   GET http://localhost:8080/api/payments/order/ORD-123
   ```

---

## 26. Next Steps

Now we have:
- **Payment Service** (Phase 11) complete.

Remaining major services:
- **Phase 10: E-commerce Service** (cart, checkout, product catalog for web)
- **Phase 12: Loyalty Service** (points, rewards)
- **Phase 13-14: Analytics & Reporting** (could be combined)
- **Phase 7: Customer Service** (if needed separately)

Given that E-commerce will heavily use Payment, Order, Product, and Inventory, we should implement it next. It will provide a REST API for frontend (React/Next.js) to handle shopping cart, wishlist, and checkout.

Shall I proceed with **E-commerce Service**?