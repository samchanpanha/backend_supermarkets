We continue building the Supermarket POS SaaS system by adding the **Inventory Service** (Phase 6). This service manages stock levels, stock movements, and publishes events when inventory changes. It will integrate with **Apache Kafka** for event-driven communication.

---

## 13. Inventory Service

### 13.1 Module Structure
```
inventory-service/
├── pom.xml
├── Dockerfile
└── src/main/java/com/supermarket/inventory/
    ├── InventoryApplication.java
    ├── entity/
    │   ├── Inventory.java
    │   └── StockMovement.java
    ├── repository/
    │   ├── InventoryRepository.java
    │   └── StockMovementRepository.java
    ├── controller/
    │   └── InventoryController.java
    ├── service/
    │   └── InventoryService.java
    ├── event/
    │   ├── InventoryEventPublisher.java
    │   └── InventoryChangedEvent.java
    ├── filter/
    │   └── TenantFilter.java
    └── util/
        └── TenantContext.java
```

### 13.2 Dependencies (`pom.xml`)

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
    <artifactId>inventory-service</artifactId>

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
    </dependencies>
</project>
```

### 13.3 Main Application Class

**`InventoryApplication.java`**
```java
package com.supermarket.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class InventoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
```

### 13.4 Tenant Context (same as product service)

**`util/TenantContext.java`**
```java
package com.supermarket.inventory.util;

public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
```

**`filter/TenantFilter.java`**
```java
package com.supermarket.inventory.filter;

import com.supermarket.inventory.util.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String tenantId = request.getHeader("X-Tenant-ID");
        if (tenantId == null) {
            tenantId = "default"; // or throw error
        }
        TenantContext.setCurrentTenant(tenantId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
```

### 13.5 Entities

**`entity/Inventory.java`**
```java
package com.supermarket.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenantId", "productId", "storeId"})
})
@Data
@NoArgsConstructor
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private Long productId;       // ID from product service (no FK)

    @Column(nullable = false)
    private Long storeId;         // store/branch identifier

    @Column(nullable = false)
    private Integer quantity;

    private Integer reorderLevel;
    private Integer reorderQuantity;

    @Version
    private Integer version;      // optimistic locking
}
```

**`entity/StockMovement.java`**
```java
package com.supermarket.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private Integer changeQuantity; // positive for increase, negative for decrease

    private Integer previousQuantity;
    private Integer newQuantity;

    @Column(nullable = false)
    private String movementType; // SALE, PURCHASE, ADJUSTMENT, TRANSFER

    private String referenceId;   // order ID, transfer ID, etc.

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
```

### 13.6 Repositories

**`repository/InventoryRepository.java`**
```java
package com.supermarket.inventory.repository;

import com.supermarket.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByTenantIdAndProductIdAndStoreId(String tenantId, Long productId, Long storeId);

    List<Inventory> findByTenantIdAndStoreId(String tenantId, Long storeId);

    List<Inventory> findByTenantIdAndQuantityLessThanEqual(String tenantId, Integer threshold);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT i FROM Inventory i WHERE i.tenantId = :tenantId AND i.productId = :productId AND i.storeId = :storeId")
    Optional<Inventory> findForUpdate(@Param("tenantId") String tenantId,
                                      @Param("productId") Long productId,
                                      @Param("storeId") Long storeId);
}
```

**`repository/StockMovementRepository.java`**
```java
package com.supermarket.inventory.repository;

import com.supermarket.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    Page<StockMovement> findByTenantIdAndProductId(String tenantId, Long productId, Pageable pageable);
    Page<StockMovement> findByTenantIdAndStoreId(String tenantId, Long storeId, Pageable pageable);
}
```

### 13.7 Event Publishing with Kafka

**`event/InventoryChangedEvent.java`**
```java
package com.supermarket.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryChangedEvent {
    private String tenantId;
    private Long productId;
    private Long storeId;
    private Integer previousQuantity;
    private Integer newQuantity;
    private String changeType; // SALE, PURCHASE, ADJUSTMENT, TRANSFER
    private String referenceId;
    private LocalDateTime timestamp;
}
```

**`event/InventoryEventPublisher.java`**
```java
package com.supermarket.inventory.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventPublisher {

    private static final String TOPIC = "inventory-changed";

    @Autowired
    private KafkaTemplate<String, InventoryChangedEvent> kafkaTemplate;

    public void publishInventoryChanged(InventoryChangedEvent event) {
        kafkaTemplate.send(TOPIC, event.getTenantId(), event);
    }
}
```

### 13.8 Service Layer

**`service/InventoryService.java`**
```java
package com.supermarket.inventory.service;

import com.supermarket.inventory.entity.Inventory;
import com.supermarket.inventory.entity.StockMovement;
import com.supermarket.inventory.event.InventoryChangedEvent;
import com.supermarket.inventory.event.InventoryEventPublisher;
import com.supermarket.inventory.repository.InventoryRepository;
import com.supermarket.inventory.repository.StockMovementRepository;
import com.supermarket.inventory.util.TenantContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private InventoryEventPublisher eventPublisher;

    @Transactional
    public Inventory adjustStock(Long productId, Long storeId, int changeQuantity,
                                 String movementType, String referenceId) {
        String tenantId = TenantContext.getCurrentTenant();

        Inventory inventory = inventoryRepository
                .findForUpdate(tenantId, productId, storeId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        int previous = inventory.getQuantity();
        int newQuantity = previous + changeQuantity;
        if (newQuantity < 0) {
            throw new RuntimeException("Insufficient stock");
        }

        inventory.setQuantity(newQuantity);
        inventory = inventoryRepository.save(inventory);

        // Record movement
        StockMovement movement = new StockMovement();
        movement.setTenantId(tenantId);
        movement.setProductId(productId);
        movement.setStoreId(storeId);
        movement.setChangeQuantity(changeQuantity);
        movement.setPreviousQuantity(previous);
        movement.setNewQuantity(newQuantity);
        movement.setMovementType(movementType);
        movement.setReferenceId(referenceId);
        movement.setTimestamp(LocalDateTime.now());
        stockMovementRepository.save(movement);

        // Publish event
        InventoryChangedEvent event = new InventoryChangedEvent(
                tenantId, productId, storeId, previous, newQuantity,
                movementType, referenceId, LocalDateTime.now()
        );
        eventPublisher.publishInventoryChanged(event);

        return inventory;
    }

    @Transactional
    public Inventory initializeStock(Long productId, Long storeId, int initialQuantity) {
        String tenantId = TenantContext.getCurrentTenant();

        Inventory inventory = new Inventory();
        inventory.setTenantId(tenantId);
        inventory.setProductId(productId);
        inventory.setStoreId(storeId);
        inventory.setQuantity(initialQuantity);
        inventory.setReorderLevel(10);
        inventory.setReorderQuantity(50);
        inventory = inventoryRepository.save(inventory);

        // Record initial movement
        StockMovement movement = new StockMovement();
        movement.setTenantId(tenantId);
        movement.setProductId(productId);
        movement.setStoreId(storeId);
        movement.setChangeQuantity(initialQuantity);
        movement.setPreviousQuantity(0);
        movement.setNewQuantity(initialQuantity);
        movement.setMovementType("INITIAL");
        movement.setTimestamp(LocalDateTime.now());
        stockMovementRepository.save(movement);

        // Publish event
        InventoryChangedEvent event = new InventoryChangedEvent(
                tenantId, productId, storeId, 0, initialQuantity,
                "INITIAL", null, LocalDateTime.now()
        );
        eventPublisher.publishInventoryChanged(event);

        return inventory;
    }
}
```

### 13.9 REST Controller

**`controller/InventoryController.java`**
```java
package com.supermarket.inventory.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.inventory.entity.Inventory;
import com.supermarket.inventory.entity.StockMovement;
import com.supermarket.inventory.repository.InventoryRepository;
import com.supermarket.inventory.repository.StockMovementRepository;
import com.supermarket.inventory.service.InventoryService;
import com.supermarket.inventory.util.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/store/{storeId}")
    public ResponseEntity<ApiResponse<List<Inventory>>> getInventoryByStore(@PathVariable Long storeId) {
        String tenantId = TenantContext.getCurrentTenant();
        List<Inventory> inventories = inventoryRepository.findByTenantIdAndStoreId(tenantId, storeId);
        return ResponseEntity.ok(ApiResponse.success(inventories));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Inventory>> getInventoryByProduct(@PathVariable Long productId,
                                                                        @RequestParam Long storeId) {
        String tenantId = TenantContext.getCurrentTenant();
        Inventory inventory = inventoryRepository
                .findByTenantIdAndProductIdAndStoreId(tenantId, productId, storeId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<Inventory>>> getLowStock(@RequestParam(defaultValue = "10") int threshold) {
        String tenantId = TenantContext.getCurrentTenant();
        List<Inventory> lowStock = inventoryRepository.findByTenantIdAndQuantityLessThanEqual(tenantId, threshold);
        return ResponseEntity.ok(ApiResponse.success(lowStock));
    }

    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<Inventory>> initializeStock(@RequestParam Long productId,
                                                                  @RequestParam Long storeId,
                                                                  @RequestParam int quantity) {
        Inventory inventory = inventoryService.initializeStock(productId, storeId, quantity);
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<Inventory>> adjustStock(@RequestParam Long productId,
                                                              @RequestParam Long storeId,
                                                              @RequestParam int change,
                                                              @RequestParam String type,
                                                              @RequestParam(required = false) String referenceId) {
        Inventory inventory = inventoryService.adjustStock(productId, storeId, change, type, referenceId);
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<Page<StockMovement>>> getMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long storeId,
            Pageable pageable) {
        String tenantId = TenantContext.getCurrentTenant();
        Page<StockMovement> movements;
        if (productId != null) {
            movements = stockMovementRepository.findByTenantIdAndProductId(tenantId, productId, pageable);
        } else if (storeId != null) {
            movements = stockMovementRepository.findByTenantIdAndStoreId(tenantId, storeId, pageable);
        } else {
            throw new IllegalArgumentException("Either productId or storeId must be provided");
        }
        return ResponseEntity.ok(ApiResponse.success(movements));
    }
}
```

### 13.10 Configuration

**`bootstrap.yml`** (same as product service)
```yaml
spring:
  application:
    name: inventory-service
  cloud:
    config:
      uri: http://config-service:8888
      fail-fast: true
```

**`Dockerfile`**
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/inventory-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 13.11 Config Repository Update

Add `inventory-service.yml` to `~/supermarket-config-repo/`:

```yaml
server:
  port: 8083

spring:
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

Also update `application.yml` (shared) to include Kafka if not already.

---

## 14. Update Docker Compose

Add Kafka, Zookeeper, and inventory-service to `docker-compose.yml`:

```yaml
version: '3.8'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.3.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
    networks:
      - supermarket-network

  kafka:
    image: confluentinc/cp-kafka:7.3.0
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    networks:
      - supermarket-network

  postgres:
    # ... unchanged

  service-discovery:
    # ... unchanged

  config-service:
    # ... unchanged

  auth-service:
    # ... unchanged

  product-service:
    # ... unchanged

  inventory-service:
    build: ./inventory-service
    container_name: inventory-service
    ports:
      - "8083:8083"
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

  api-gateway:
    build: ./api-gateway
    container_name: api-gateway
    ports:
      - "8080:8080"
    depends_on:
      - service-discovery
      - config-service
      - auth-service
      - product-service
      - inventory-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_CONFIG_IMPORT=configserver:http://config-service:8888
    networks:
      - supermarket-network

networks:
  supermarket-network:
    driver: bridge

volumes:
  postgres-data:
```

---

## 15. Update API Gateway Routes

Add route for inventory service in `GatewayConfig.java`:

```java
.route("inventory-service", r -> r
    .path("/api/inventory/**")
    .filters(f -> f.rewritePath("/api/inventory/(?<segment>.*)", "/inventory/${segment}")
            .filter(new AuthenticationFilter().apply(new AuthenticationFilter.Config())))
    .uri("lb://INVENTORY-SERVICE"))
```

Also update the config repository's `api-gateway.yml` if needed (routes are in code, so fine).

---

## 16. Testing the New Service

1. Rebuild all services:
   ```bash
   mvn clean package
   ```

2. Start the stack:
   ```bash
   docker-compose up --build
   ```

3. Create a product first (via product service) to get a product ID.

4. Initialize inventory for that product:
   ```
   POST http://localhost:8080/api/inventory/initialize?productId=1&storeId=1&quantity=100
   Header: Authorization: Bearer <jwt-token>
   ```

5. Adjust stock:
   ```
   POST http://localhost:8080/api/inventory/adjust?productId=1&storeId=1&change=-10&type=SALE&referenceId=order123
   Header: Authorization: Bearer <jwt-token>
   ```

6. Check low stock:
   ```
   GET http://localhost:8080/api/inventory/low-stock?threshold=20
   ```

7. View movements:
   ```
   GET http://localhost:8080/api/inventory/movements?productId=1
   ```

---

## 17. What's Next?

The inventory service is now complete with:
- Tenant-isolated stock management
- Stock movement tracking
- Kafka event publishing
- Optimistic locking for concurrency
- REST API

Next steps from the original guide:
- **Order Service** (Phase 8) – handles orders, integrates with inventory via Kafka consumers to reserve/release stock.
- **POS Service** (Phase 9) – uses inventory and product services.
- **E-commerce Service** (Phase 10) – uses cart, checkout, inventory.

We can continue building those services similarly. Would you like me to proceed with **Order Service** next, or any other specific component?