We'll continue building the Supermarket POS SaaS system by implementing the **POS Service** (Phase 9). This service handles point-of-sale transactions, cashier shifts, receipts, and integrates with Order, Inventory, and Payment services. It will support offline mode (using local storage) and sync later, but we'll start with online mode.

---

## 22. POS Service

### 22.1 Module Structure
```
pos-service/
├── pom.xml
├── Dockerfile
└── src/main/java/com/supermarket/pos/
    ├── PosApplication.java
    ├── entity/
    │   ├── Sale.java
    │   ├── SaleItem.java
    │   ├── Shift.java
    │   └── CashDrawer.java
    ├── repository/
    │   ├── SaleRepository.java
    │   ├── ShiftRepository.java
    │   └── CashDrawerRepository.java
    ├── controller/
    │   └── PosController.java
    ├── service/
    │   ├── PosService.java
    │   └── ShiftService.java
    ├── client/
    │   ├── ProductServiceClient.java
    │   ├── InventoryServiceClient.java
    │   ├── OrderServiceClient.java
    │   └── PaymentServiceClient.java (will be added later)
    ├── filter/
    │   └── TenantFilter.java
    └── util/
        └── TenantContext.java
```

### 22.2 Dependencies (`pom.xml`)

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
    <artifactId>pos-service</artifactId>

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
        <!-- For receipt generation -->
        <dependency>
            <groupId>com.itextpdf</groupId>
            <artifactId>itext7-core</artifactId>
            <version>7.2.5</version>
            <type>pom</type>
        </dependency>
    </dependencies>
</project>
```

### 22.3 Main Application

**`PosApplication.java`**
```java
package com.supermarket.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class PosApplication {
    public static void main(String[] args) {
        SpringApplication.run(PosApplication.class, args);
    }
}
```

### 22.4 Tenant Context and Filter (same as before)

### 22.5 Entities

**`entity/Shift.java`** (cashier shift)
```java
package com.supermarket.pos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "shifts")
@Data
@NoArgsConstructor
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private Long cashierId;   // user ID from auth service

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(nullable = false)
    private Double openingCash;

    private Double closingCash;
    private Double expectedCash; // calculated from sales

    @Column(nullable = false)
    private String status; // OPEN, CLOSED

    private String notes;
}
```

**`entity/Sale.java`** (POS transaction)
```java
package com.supermarket.pos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private Long shiftId;

    @Column(nullable = false)
    private Long cashierId;

    @Column(nullable = false)
    private String saleNumber;   // unique receipt number

    @Column(nullable = false)
    private LocalDateTime saleTime;

    @Column(nullable = false)
    private String status; // COMPLETED, VOIDED, REFUNDED

    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal total;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SaleItem> items = new ArrayList<>();

    private String paymentMethod; // CASH, CARD, MIXED
    private BigDecimal cashTendered;
    private BigDecimal changeGiven;

    private String cardLastFour;
    private String cardTransactionId;

    private String customerId; // optional

    private String notes;
}
```

**`entity/SaleItem.java`**
```java
package com.supermarket.pos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_items")
@Data
@NoArgsConstructor
public class SaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    private String barcode;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    private BigDecimal discount;   // per item discount

    @Column(nullable = false)
    private BigDecimal totalPrice; // quantity * unitPrice - discount
}
```

**`entity/CashDrawer.java`** (optional, but useful for tracking)
```java
package com.supermarket.pos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_drawers")
@Data
@NoArgsConstructor
public class CashDrawer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private Long storeId;

    private Double currentBalance;

    private LocalDateTime lastOpened;
    private LocalDateTime lastClosed;

    private String status; // OPEN, CLOSED
}
```

### 22.6 Repositories

**`repository/ShiftRepository.java`**
```java
package com.supermarket.pos.repository;

import com.supermarket.pos.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Optional<Shift> findByTenantIdAndStoreIdAndCashierIdAndStatus(String tenantId, Long storeId, Long cashierId, String status);
}
```

**`repository/SaleRepository.java`**
```java
package com.supermarket.pos.repository;

import com.supermarket.pos.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    Page<Sale> findByTenantIdAndStoreId(String tenantId, Long storeId, Pageable pageable);
    Page<Sale> findByTenantIdAndCashierId(String tenantId, Long cashierId, Pageable pageable);
    Page<Sale> findByTenantIdAndShiftId(String tenantId, Long shiftId, Pageable pageable);
    List<Sale> findByTenantIdAndShiftIdAndSaleTimeBetween(String tenantId, Long shiftId, LocalDateTime start, LocalDateTime end);
}
```

**`repository/CashDrawerRepository.java`**
```java
package com.supermarket.pos.repository;

import com.supermarket.pos.entity.CashDrawer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CashDrawerRepository extends JpaRepository<CashDrawer, Long> {
    Optional<CashDrawer> findByTenantIdAndStoreId(String tenantId, Long storeId);
}
```

### 22.7 Feign Clients

**`client/ProductServiceClient.java`**
```java
package com.supermarket.pos.client;

import com.supermarket.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "product-service")
public interface ProductServiceClient {
    @GetMapping("/products/barcode/{barcode}")
    ApiResponse<?> getProductByBarcode(@PathVariable("barcode") String barcode,
                                       @RequestHeader("X-Tenant-ID") String tenantId);
}
```

**`client/InventoryServiceClient.java`**
```java
package com.supermarket.pos.client;

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

### 22.8 Service Layer

**`service/ShiftService.java`**
```java
package com.supermarket.pos.service;

import com.supermarket.pos.entity.Shift;
import com.supermarket.pos.repository.ShiftRepository;
import com.supermarket.pos.util.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ShiftService {

    @Autowired
    private ShiftRepository shiftRepository;

    @Transactional
    public Shift openShift(Long storeId, Long cashierId, Double openingCash) {
        String tenantId = TenantContext.getCurrentTenant();

        // Check if there's already an open shift for this cashier
        shiftRepository.findByTenantIdAndStoreIdAndCashierIdAndStatus(tenantId, storeId, cashierId, "OPEN")
                .ifPresent(shift -> {
                    throw new RuntimeException("Cashier already has an open shift");
                });

        Shift shift = new Shift();
        shift.setTenantId(tenantId);
        shift.setStoreId(storeId);
        shift.setCashierId(cashierId);
        shift.setStartTime(LocalDateTime.now());
        shift.setOpeningCash(openingCash);
        shift.setStatus("OPEN");
        return shiftRepository.save(shift);
    }

    @Transactional
    public Shift closeShift(Long shiftId, Double closingCash) {
        String tenantId = TenantContext.getCurrentTenant();
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found"));
        if (!shift.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }
        if (!"OPEN".equals(shift.getStatus())) {
            throw new RuntimeException("Shift is not open");
        }
        shift.setEndTime(LocalDateTime.now());
        shift.setClosingCash(closingCash);
        // expectedCash would be calculated from sales during shift
        shift.setStatus("CLOSED");
        return shiftRepository.save(shift);
    }

    public Shift getCurrentShift(Long storeId, Long cashierId) {
        String tenantId = TenantContext.getCurrentTenant();
        return shiftRepository.findByTenantIdAndStoreIdAndCashierIdAndStatus(tenantId, storeId, cashierId, "OPEN")
                .orElseThrow(() -> new RuntimeException("No open shift found"));
    }
}
```

**`service/PosService.java`**
```java
package com.supermarket.pos.service;

import com.supermarket.pos.client.InventoryServiceClient;
import com.supermarket.pos.client.ProductServiceClient;
import com.supermarket.pos.dto.SaleRequest;
import com.supermarket.pos.entity.Sale;
import com.supermarket.pos.entity.SaleItem;
import com.supermarket.pos.entity.Shift;
import com.supermarket.pos.repository.SaleRepository;
import com.supermarket.pos.repository.ShiftRepository;
import com.supermarket.pos.util.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PosService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private InventoryServiceClient inventoryServiceClient;

    @Transactional
    public Sale createSale(SaleRequest request) {
        String tenantId = TenantContext.getCurrentTenant();

        // Validate shift is open
        Shift shift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() -> new RuntimeException("Shift not found"));
        if (!shift.getTenantId().equals(tenantId) || !"OPEN".equals(shift.getStatus())) {
            throw new RuntimeException("Invalid or closed shift");
        }

        Sale sale = new Sale();
        sale.setTenantId(tenantId);
        sale.setStoreId(shift.getStoreId());
        sale.setShiftId(request.getShiftId());
        sale.setCashierId(shift.getCashierId());
        sale.setSaleNumber(generateSaleNumber());
        sale.setSaleTime(LocalDateTime.now());
        sale.setStatus("COMPLETED");
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setCashTendered(request.getCashTendered());
        sale.setCustomerId(request.getCustomerId());
        sale.setNotes(request.getNotes());

        // Calculate totals and create items
        BigDecimal subtotal = BigDecimal.ZERO;
        List<SaleItem> items = request.getItems().stream().map(itemReq -> {
            // In real scenario, fetch product details from product service
            // For now, assume product exists
            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProductId(itemReq.getProductId());
            item.setProductName(itemReq.getProductName()); // should come from product service
            item.setBarcode(itemReq.getBarcode());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            item.setDiscount(itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO);
            BigDecimal total = itemReq.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()))
                    .subtract(item.getDiscount());
            item.setTotalPrice(total);
            subtotal = subtotal.add(total);
            return item;
        }).collect(Collectors.toList());

        sale.setItems(items);
        sale.setSubtotal(subtotal);
        sale.setTax(calculateTax(subtotal));
        sale.setDiscount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO);
        BigDecimal total = subtotal.add(sale.getTax()).subtract(sale.getDiscount());
        sale.setTotal(total);

        // Calculate change if cash payment
        if ("CASH".equals(request.getPaymentMethod()) && request.getCashTendered() != null) {
            BigDecimal change = request.getCashTendered().subtract(total);
            sale.setChangeGiven(change);
        }

        sale = saleRepository.save(sale);

        // Update inventory for each item sold
        for (SaleItem item : items) {
            inventoryServiceClient.adjustStock(
                    item.getProductId(),
                    sale.getStoreId(),
                    -item.getQuantity(),
                    "SALE",
                    "SALE_" + sale.getId(),
                    tenantId
            );
        }

        return sale;
    }

    @Transactional
    public Sale voidSale(Long saleId, String reason) {
        String tenantId = TenantContext.getCurrentTenant();
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found"));
        if (!sale.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }
        if (!"COMPLETED".equals(sale.getStatus())) {
            throw new RuntimeException("Sale cannot be voided");
        }

        // Reverse inventory
        for (SaleItem item : sale.getItems()) {
            inventoryServiceClient.adjustStock(
                    item.getProductId(),
                    sale.getStoreId(),
                    item.getQuantity(),
                    "VOID",
                    "VOID_" + sale.getId(),
                    tenantId
            );
        }

        sale.setStatus("VOIDED");
        sale.setNotes(sale.getNotes() + " | VOIDED: " + reason);
        return saleRepository.save(sale);
    }

    public Sale getSale(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));
        if (!sale.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }
        return sale;
    }

    private String generateSaleNumber() {
        return "SALE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal calculateTax(BigDecimal subtotal) {
        // Simplified tax
        return subtotal.multiply(new BigDecimal("0.10"));
    }
}
```

### 22.9 DTOs

**`dto/SaleRequest.java`**
```java
package com.supermarket.pos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SaleRequest {
    private Long shiftId;
    private String paymentMethod; // CASH, CARD
    private BigDecimal cashTendered; // for cash payments
    private BigDecimal discount;
    private String customerId; // optional
    private String notes;
    private List<SaleItemRequest> items;

    @Data
    public static class SaleItemRequest {
        private Long productId;
        private String productName;
        private String barcode;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
    }
}
```

**`dto/ShiftOpenRequest.java`**
```java
package com.supermarket.pos.dto;

import lombok.Data;

@Data
public class ShiftOpenRequest {
    private Long storeId;
    private Long cashierId;
    private Double openingCash;
}
```

**`dto/ShiftCloseRequest.java`**
```java
package com.supermarket.pos.dto;

import lombok.Data;

@Data
public class ShiftCloseRequest {
    private Double closingCash;
}
```

### 22.10 REST Controller

**`controller/PosController.java`**
```java
package com.supermarket.pos.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.pos.dto.SaleRequest;
import com.supermarket.pos.dto.ShiftCloseRequest;
import com.supermarket.pos.dto.ShiftOpenRequest;
import com.supermarket.pos.entity.Sale;
import com.supermarket.pos.entity.Shift;
import com.supermarket.pos.repository.SaleRepository;
import com.supermarket.pos.service.PosService;
import com.supermarket.pos.service.ShiftService;
import com.supermarket.pos.util.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pos")
public class PosController {

    @Autowired
    private PosService posService;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private SaleRepository saleRepository;

    // Shift endpoints
    @PostMapping("/shifts/open")
    public ResponseEntity<ApiResponse<Shift>> openShift(@RequestBody ShiftOpenRequest request) {
        Shift shift = shiftService.openShift(request.getStoreId(), request.getCashierId(), request.getOpeningCash());
        return ResponseEntity.ok(ApiResponse.success(shift));
    }

    @PutMapping("/shifts/{shiftId}/close")
    public ResponseEntity<ApiResponse<Shift>> closeShift(@PathVariable Long shiftId,
                                                         @RequestBody ShiftCloseRequest request) {
        Shift shift = shiftService.closeShift(shiftId, request.getClosingCash());
        return ResponseEntity.ok(ApiResponse.success(shift));
    }

    @GetMapping("/shifts/current")
    public ResponseEntity<ApiResponse<Shift>> getCurrentShift(@RequestParam Long storeId,
                                                              @RequestParam Long cashierId) {
        Shift shift = shiftService.getCurrentShift(storeId, cashierId);
        return ResponseEntity.ok(ApiResponse.success(shift));
    }

    // Sale endpoints
    @PostMapping("/sales")
    public ResponseEntity<ApiResponse<Sale>> createSale(@RequestBody SaleRequest request) {
        Sale sale = posService.createSale(request);
        return ResponseEntity.ok(ApiResponse.success(sale));
    }

    @GetMapping("/sales/{id}")
    public ResponseEntity<ApiResponse<Sale>> getSale(@PathVariable Long id) {
        Sale sale = posService.getSale(id);
        return ResponseEntity.ok(ApiResponse.success(sale));
    }

    @PutMapping("/sales/{id}/void")
    public ResponseEntity<ApiResponse<Sale>> voidSale(@PathVariable Long id,
                                                       @RequestParam String reason) {
        Sale sale = posService.voidSale(id, reason);
        return ResponseEntity.ok(ApiResponse.success(sale));
    }

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<Page<Sale>>> getSales(@RequestParam(required = false) Long storeId,
                                                            @RequestParam(required = false) Long cashierId,
                                                            @RequestParam(required = false) Long shiftId,
                                                            Pageable pageable) {
        String tenantId = TenantContext.getCurrentTenant();
        Page<Sale> sales;
        if (storeId != null) {
            sales = saleRepository.findByTenantIdAndStoreId(tenantId, storeId, pageable);
        } else if (cashierId != null) {
            sales = saleRepository.findByTenantIdAndCashierId(tenantId, cashierId, pageable);
        } else if (shiftId != null) {
            sales = saleRepository.findByTenantIdAndShiftId(tenantId, shiftId, pageable);
        } else {
            throw new IllegalArgumentException("At least one filter required");
        }
        return ResponseEntity.ok(ApiResponse.success(sales));
    }
}
```

### 22.11 Configuration

**`bootstrap.yml`**
```yaml
spring:
  application:
    name: pos-service
  cloud:
    config:
      uri: http://config-service:8888
      fail-fast: true
```

**`Dockerfile`**
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/pos-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 22.12 Config Repository Update

Add `pos-service.yml` to `~/supermarket-config-repo/`:

```yaml
server:
  port: 8085
```

### 22.13 Update Docker Compose

Add pos-service to `docker-compose.yml`:

```yaml
pos-service:
  build: ./pos-service
  container_name: pos-service
  ports:
    - "8085:8085"
  depends_on:
    - postgres
    - service-discovery
    - config-service
    - product-service
    - inventory-service
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - SPRING_CONFIG_IMPORT=configserver:http://config-service:8888
  networks:
    - supermarket-network
```

Update API Gateway routes:

```java
.route("pos-service", r -> r
    .path("/api/pos/**")
    .filters(f -> f.rewritePath("/api/pos/(?<segment>.*)", "/pos/${segment}")
            .filter(new AuthenticationFilter().apply(new AuthenticationFilter.Config())))
    .uri("lb://POS-SERVICE"))
```

---

## 23. Testing POS Service

1. Rebuild:
   ```bash
   mvn clean package
   docker-compose up --build
   ```

2. Get JWT token.

3. Open a shift:
   ```
   POST http://localhost:8080/api/pos/shifts/open
   Authorization: Bearer <token>
   {
     "storeId": 1,
     "cashierId": 1,
     "openingCash": 100.00
   }
   ```

4. Create a sale (ensure product exists and has inventory):
   ```
   POST http://localhost:8080/api/pos/sales
   {
     "shiftId": 1,
     "paymentMethod": "CASH",
     "cashTendered": 50.00,
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

5. Get sale details.

6. Void sale (if needed).

7. Close shift:
   ```
   PUT http://localhost:8080/api/pos/shifts/1/close
   {
     "closingCash": 150.00
   }
   ```

---

## 24. Next Steps

We have now implemented:
- **Phase 1-2**: Infrastructure (Eureka, Config, Gateway)
- **Phase 3**: Auth Service
- **Phase 5**: Product Service
- **Phase 6**: Inventory Service
- **Phase 8**: Order Service
- **Phase 9**: POS Service

The remaining major components:
- **Phase 7**: Customer Service (already partially in order service but needs its own service)
- **Phase 10**: E-commerce Service
- **Phase 11**: Payment Service
- **Phase 12**: Loyalty Service
- **Phase 13-14**: Analytics & Reporting
- **Phase 15**: Event-Driven Enhancements
- **Phase 16+**: Cross-cutting concerns, testing, deployment

Given the request to "continue", I can proceed with any of these. Which one would you like next? Common choices are **Payment Service** or **E-commerce Service**. Let me know, and I'll implement it.