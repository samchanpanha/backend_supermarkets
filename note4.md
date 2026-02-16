We'll continue by implementing the **Order Service** (Phase 8). This service will handle order creation, management, and fulfillment. It will integrate with **Inventory Service** to reserve stock and with **Kafka** to publish order events. Later, it will be used by the POS (for online orders) and the E-commerce service.

---

## 18. Order Service

### 18.1 Module Structure
```
order-service/
├── pom.xml
├── Dockerfile
└── src/main/java/com/supermarket/order/
    ├── OrderApplication.java
    ├── entity/
    │   ├── Order.java
    │   └── OrderItem.java
    ├── repository/
    │   ├── OrderRepository.java
    │   └── OrderItemRepository.java
    ├── controller/
    │   └── OrderController.java
    ├── service/
    │   └── OrderService.java
    ├── client/
    │   ├── ProductServiceClient.java
    │   └── InventoryServiceClient.java
    ├── event/
    │   ├── OrderEventPublisher.java
    │   └── OrderEvent.java
    ├── filter/
    │   └── TenantFilter.java
    └── util/
        └── TenantContext.java
```

### 18.2 Dependencies (`pom.xml`)

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
    <artifactId>order-service</artifactId>

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
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
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
    </dependencies>
</project>
```

### 18.3 Main Application

**`OrderApplication.java`**
```java
package com.supermarket.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

### 18.4 Tenant Context (same as before)

### 18.5 Entities

**`entity/Order.java`**
```java
package com.supermarket.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private Long storeId;

    private String customerId;   // if logged in

    @Column(nullable = false)
    private String orderNumber;   // unique

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private String status; // PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED

    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shippingCost;
    private BigDecimal discount;
    private BigDecimal total;

    private String paymentMethod; // CASH, CARD, ONLINE
    private String paymentStatus; // PENDING, PAID, FAILED, REFUNDED

    private String shippingAddress;
    private String billingAddress;
    private String contactPhone;
    private String contactEmail;

    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();
}
```

**`entity/OrderItem.java`**
```java
package com.supermarket.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;

    private String productName;
    private String barcode;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    private BigDecimal discount;

    @Column(nullable = false)
    private BigDecimal totalPrice;
}
```

### 18.6 Repositories

**`repository/OrderRepository.java`**
```java
package com.supermarket.order.repository;

import com.supermarket.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByTenantId(String tenantId, Pageable pageable);
    Page<Order> findByTenantIdAndCustomerId(String tenantId, String customerId, Pageable pageable);
    Page<Order> findByTenantIdAndStoreId(String tenantId, Long storeId, Pageable pageable);
    Optional<Order> findByTenantIdAndOrderNumber(String tenantId, String orderNumber);
}
```

**`repository/OrderItemRepository.java`** (optional, can use order's list)

### 18.7 Feign Clients

**`client/ProductServiceClient.java`** (to fetch product details if needed)
```java
package com.supermarket.order.client;

import com.supermarket.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "product-service")
public interface ProductServiceClient {
    @GetMapping("/products/{id}")
    ApiResponse<?> getProductById(@PathVariable("id") Long id,
                                  @RequestHeader("X-Tenant-ID") String tenantId);
}
```

**`client/InventoryServiceClient.java`** (to reserve stock)
```java
package com.supermarket.order.client;

import com.supermarket.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "inventory-service")
public interface InventoryServiceClient {
    @PostMapping("/inventory/adjust")
    ApiResponse<?> adjustStock(@RequestParam("productId") Long productId,
                               @RequestParam("storeId") Long storeId,
                               @RequestParam("change") int change,
                               @RequestParam("type") String type,
                               @RequestParam("referenceId") String referenceId,
                               @RequestHeader("X-Tenant-ID") String tenantId);
}
```

### 18.8 Event Publishing

**`event/OrderEvent.java`**
```java
package com.supermarket.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String eventType; // CREATED, CONFIRMED, CANCELLED, SHIPPED, DELIVERED
    private String tenantId;
    private Long orderId;
    private String orderNumber;
    private String customerId;
    private BigDecimal total;
    private LocalDateTime timestamp;
}
```

**`event/OrderEventPublisher.java`**
```java
package com.supermarket.order.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
    private static final String TOPIC = "order-events";

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publishOrderEvent(OrderEvent event) {
        kafkaTemplate.send(TOPIC, event.getTenantId(), event);
    }
}
```

### 18.9 Service Layer

**`service/OrderService.java`**
```java
package com.supermarket.order.service;

import com.supermarket.order.client.InventoryServiceClient;
import com.supermarket.order.dto.OrderRequest;
import com.supermarket.order.entity.Order;
import com.supermarket.order.entity.OrderItem;
import com.supermarket.order.event.OrderEvent;
import com.supermarket.order.event.OrderEventPublisher;
import com.supermarket.order.repository.OrderRepository;
import com.supermarket.order.util.TenantContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryServiceClient inventoryServiceClient;

    @Autowired
    private OrderEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(OrderRequest request) {
        String tenantId = TenantContext.getCurrentTenant();

        Order order = new Order();
        order.setTenantId(tenantId);
        order.setStoreId(request.getStoreId());
        order.setCustomerId(request.getCustomerId());
        order.setOrderNumber(generateOrderNumber());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus("PENDING");
        order.setShippingAddress(request.getShippingAddress());
        order.setBillingAddress(request.getBillingAddress());
        order.setContactPhone(request.getContactPhone());
        order.setContactEmail(request.getContactEmail());
        order.setNotes(request.getNotes());

        // Calculate totals and create items
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(itemReq.getProductId());
            item.setProductName(itemReq.getProductName());
            item.setBarcode(itemReq.getBarcode());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            item.setDiscount(itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO);
            BigDecimal total = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .subtract(item.getDiscount());
            item.setTotalPrice(total);
            subtotal = subtotal.add(total);
            order.getItems().add(item);
        }

        order.setSubtotal(subtotal);
        order.setTax(calculateTax(subtotal));
        order.setShippingCost(request.getShippingCost() != null ? request.getShippingCost() : BigDecimal.ZERO);
        order.setDiscount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO);
        BigDecimal total = subtotal.add(order.getTax()).add(order.getShippingCost()).subtract(order.getDiscount());
        order.setTotal(total);

        order = orderRepository.save(order);

        // Reserve inventory (reduce stock) – we'll use a "RESERVE" type
        for (OrderItem item : order.getItems()) {
            inventoryServiceClient.adjustStock(
                    item.getProductId(),
                    order.getStoreId(),
                    -item.getQuantity(),
                    "RESERVE",
                    "ORDER_" + order.getId(),
                    tenantId
            );
        }

        // Publish event
        OrderEvent event = new OrderEvent(
                "CREATED",
                tenantId,
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getTotal(),
                LocalDateTime.now()
        );
        eventPublisher.publishOrderEvent(event);

        return order;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus) {
        String tenantId = TenantContext.getCurrentTenant();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }

        String oldStatus = order.getStatus();
        order.setStatus(newStatus);

        // If cancelling, release inventory
        if ("CANCELLED".equals(newStatus) && !"CANCELLED".equals(oldStatus)) {
            for (OrderItem item : order.getItems()) {
                inventoryServiceClient.adjustStock(
                        item.getProductId(),
                        order.getStoreId(),
                        item.getQuantity(), // add back
                        "RELEASE",
                        "CANCEL_" + order.getId(),
                        tenantId
                );
            }
        }

        order = orderRepository.save(order);

        // Publish event
        OrderEvent event = new OrderEvent(
                newStatus,
                tenantId,
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getTotal(),
                LocalDateTime.now()
        );
        eventPublisher.publishOrderEvent(event);

        return order;
    }

    @Transactional
    public Order confirmPayment(Long orderId, String paymentStatus) {
        String tenantId = TenantContext.getCurrentTenant();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }

        order.setPaymentStatus(paymentStatus);
        if ("PAID".equals(paymentStatus) && "PENDING".equals(order.getStatus())) {
            order.setStatus("CONFIRMED");
        }
        order = orderRepository.save(order);

        // Publish event
        OrderEvent event = new OrderEvent(
                "PAYMENT_" + paymentStatus,
                tenantId,
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getTotal(),
                LocalDateTime.now()
        );
        eventPublisher.publishOrderEvent(event);

        return order;
    }

    public Order getOrder(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }
        return order;
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal calculateTax(BigDecimal subtotal) {
        // Simplified tax – 10%
        return subtotal.multiply(new BigDecimal("0.10"));
    }
}
```

### 18.10 DTOs

**`dto/OrderRequest.java`**
```java
package com.supermarket.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderRequest {
    private Long storeId;
    private String customerId;
    private String paymentMethod;
    private String shippingAddress;
    private String billingAddress;
    private String contactPhone;
    private String contactEmail;
    private BigDecimal shippingCost;
    private BigDecimal discount;
    private String notes;
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        private Long productId;
        private String productName;
        private String barcode;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
    }
}
```

**`dto/OrderStatusUpdateRequest.java`**
```java
package com.supermarket.order.dto;

import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    private String status;
}
```

**`dto/PaymentConfirmationRequest.java`**
```java
package com.supermarket.order.dto;

import lombok.Data;

@Data
public class PaymentConfirmationRequest {
    private String paymentStatus;
    private String transactionId;
}
```

### 18.11 REST Controller

**`controller/OrderController.java`**
```java
package com.supermarket.order.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.order.dto.OrderRequest;
import com.supermarket.order.dto.OrderStatusUpdateRequest;
import com.supermarket.order.dto.PaymentConfirmationRequest;
import com.supermarket.order.entity.Order;
import com.supermarket.order.repository.OrderRepository;
import com.supermarket.order.service.OrderService;
import com.supermarket.order.util.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(@RequestBody OrderRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrder(@PathVariable Long id) {
        Order order = orderService.getOrder(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Order>> updateStatus(@PathVariable Long id,
                                                           @RequestBody OrderStatusUpdateRequest request) {
        Order order = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<ApiResponse<Order>> confirmPayment(@PathVariable Long id,
                                                             @RequestBody PaymentConfirmationRequest request) {
        Order order = orderService.confirmPayment(id, request.getPaymentStatus());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Order>>> getOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) Long storeId,
            Pageable pageable) {
        String tenantId = TenantContext.getCurrentTenant();
        Page<Order> orders;
        if (customerId != null) {
            orders = orderRepository.findByTenantIdAndCustomerId(tenantId, customerId, pageable);
        } else if (storeId != null) {
            orders = orderRepository.findByTenantIdAndStoreId(tenantId, storeId, pageable);
        } else {
            orders = orderRepository.findByTenantId(tenantId, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
```

### 18.12 Configuration

**`bootstrap.yml`**
```yaml
spring:
  application:
    name: order-service
  cloud:
    config:
      uri: http://config-service:8888
      fail-fast: true
```

**`Dockerfile`**
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/order-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 18.13 Config Repository Update

Add `order-service.yml` to `~/supermarket-config-repo/`:

```yaml
server:
  port: 8084

spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

### 18.14 Update Docker Compose

Add order-service to `docker-compose.yml`:

```yaml
order-service:
  build: ./order-service
  container_name: order-service
  ports:
    - "8084:8084"
  depends_on:
    - postgres
    - service-discovery
    - config-service
    - product-service
    - inventory-service
    - kafka
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - SPRING_CONFIG_IMPORT=configserver:http://config-service:8888
  networks:
    - supermarket-network
```

Update API Gateway routes:

```java
.route("order-service", r -> r
    .path("/api/orders/**")
    .filters(f -> f.rewritePath("/api/orders/(?<segment>.*)", "/orders/${segment}")
            .filter(new AuthenticationFilter().apply(new AuthenticationFilter.Config())))
    .uri("lb://ORDER-SERVICE"))
```

### 18.15 Testing Order Service

1. Rebuild:
   ```bash
   mvn clean package
   docker-compose up --build
   ```

2. Get JWT token.

3. Create an order:
   ```
   POST http://localhost:8080/api/orders
   Authorization: Bearer <token>
   {
     "storeId": 1,
     "customerId": "cust123",
     "paymentMethod": "CARD",
     "shippingAddress": "123 Main St",
     "contactPhone": "555-1234",
     "items": [
       {
         "productId": 1,
         "productName": "Milk",
         "quantity": 2,
         "unitPrice": 2.50
       }
     ]
   }
   ```

4. Get order by ID.

5. Update order status (e.g., to "CONFIRMED").

6. Confirm payment (simulate from payment service later).

7. Check inventory adjustments (should have decreased stock).

---

## 19. Next Steps

Now we have:
- **Phase 8: Order Service** (complete)
- **Phase 9: POS Service** (complete)
- **Phase 5-6: Product & Inventory** (complete)
- **Phase 3-4: Auth & Tenant** (complete)
- **Infrastructure** (complete)

Remaining major services:
- **Phase 7: Customer Service** (but we already have customerId fields, could be separate)
- **Phase 10: E-commerce Service**
- **Phase 11: Payment Service**
- **Phase 12: Loyalty Service**
- **Phase 13-14: Analytics & Reporting**

Given that both Order and POS need payment processing, I suggest implementing **Payment Service** next, as it's a core dependency. After that, E-commerce can use both Order and Payment.

Shall I proceed with **Payment Service**?