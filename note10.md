We'll now implement **Tenant Service** (Phase 4), which is the dedicated service for managing tenants (supermarket chains), their stores, and subscription plans. This service provides the foundation for multi-tenancy across the entire system.

---

## 31. Tenant Service

### 31.1 Module Structure
```
tenant-service/
├── pom.xml
├── Dockerfile
└── src/main/java/com/supermarket/tenant/
    ├── TenantApplication.java
    ├── entity/
    │   ├── Tenant.java
    │   ├── Store.java
    │   └── Subscription.java
    ├── repository/
    │   ├── TenantRepository.java
    │   ├── StoreRepository.java
    │   └── SubscriptionRepository.java
    ├── controller/
    │   ├── TenantController.java
    │   └── StoreController.java
    ├── service/
    │   ├── TenantService.java
    │   └── SubscriptionService.java
    ├── dto/
    │   ├── TenantRequest.java
    │   ├── TenantResponse.java
    │   └── StoreRequest.java
    ├── filter/
    │   └── TenantFilter.java (optional, but for consistency)
    └── util/
        └── TenantContext.java (not needed here, but can be included)
```

### 31.2 Dependencies (`pom.xml`)

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
    <artifactId>tenant-service</artifactId>

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

### 31.3 Main Application

**`TenantApplication.java`**
```java
package com.supermarket.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class TenantApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenantApplication.class, args);
    }
}
```

### 31.4 Entities

**`entity/Tenant.java`** (the main tenant)
```java
package com.supermarket.tenant.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tenantId;           // Unique identifier (e.g., UUID or slug)

    @Column(nullable = false)
    private String name;

    private String subdomain;           // e.g., storename.supermarket.com
    private String contactEmail;
    private String contactPhone;

    @Column(nullable = false)
    private String plan;                 // BASIC, PREMIUM, ENTERPRISE

    private String status;               // ACTIVE, SUSPENDED, TRIAL

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL)
    private List<Store> stores = new ArrayList<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL)
    private List<Subscription> subscriptions = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**`entity/Store.java`** (a store/branch belonging to a tenant)
```java
package com.supermarket.tenant.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "stores")
@Data
@NoArgsConstructor
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String name;

    private String code;                // store code for internal use
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phone;
    private String email;

    private String managerName;
    private String managerPhone;

    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**`entity/Subscription.java`** (tracks tenant's subscription)
```java
package com.supermarket.tenant.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    private String planName;             // BASIC, PREMIUM, etc.
    private BigDecimal price;
    private String billingCycle;          // MONTHLY, YEARLY

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime nextBillingDate;

    private String status;                // ACTIVE, EXPIRED, CANCELLED

    private String paymentMethod;
    private String paymentDetails;        // encrypted

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 31.5 Repositories

**`repository/TenantRepository.java`**
```java
package com.supermarket.tenant.repository;

import com.supermarket.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByTenantId(String tenantId);
    Optional<Tenant> findBySubdomain(String subdomain);
}
```

**`repository/StoreRepository.java`**
```java
package com.supermarket.tenant.repository;

import com.supermarket.tenant.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByTenantId(Long tenantId);
    List<Store> findByTenantTenantId(String tenantId);
}
```

**`repository/SubscriptionRepository.java`**
```java
package com.supermarket.tenant.repository;

import com.supermarket.tenant.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findTopByTenantIdAndStatusOrderByEndDateDesc(Long tenantId, String status);
}
```

### 31.6 DTOs

**`dto/TenantRequest.java`**
```java
package com.supermarket.tenant.dto;

import lombok.Data;

@Data
public class TenantRequest {
    private String name;
    private String subdomain;
    private String contactEmail;
    private String contactPhone;
    private String plan;
}
```

**`dto/TenantResponse.java`**
```java
package com.supermarket.tenant.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TenantResponse {
    private Long id;
    private String tenantId;
    private String name;
    private String subdomain;
    private String contactEmail;
    private String contactPhone;
    private String plan;
    private String status;
    private LocalDateTime createdAt;
}
```

**`dto/StoreRequest.java`**
```java
package com.supermarket.tenant.dto;

import lombok.Data;

@Data
public class StoreRequest {
    private String name;
    private String code;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phone;
    private String email;
    private String managerName;
    private String managerPhone;
    private boolean active;
}
```

### 31.7 Service Layer

**`service/TenantService.java`**
```java
package com.supermarket.tenant.service;

import com.supermarket.tenant.dto.TenantRequest;
import com.supermarket.tenant.dto.TenantResponse;
import com.supermarket.tenant.entity.Tenant;
import com.supermarket.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional
    public TenantResponse createTenant(TenantRequest request) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(generateTenantId());
        tenant.setName(request.getName());
        tenant.setSubdomain(request.getSubdomain());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setPlan(request.getPlan());
        tenant.setStatus("ACTIVE"); // or TRIAL
        tenant.setCreatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);

        return mapToResponse(tenant);
    }

    @Transactional
    public TenantResponse updateTenant(String tenantId, TenantRequest request) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        tenant.setName(request.getName());
        tenant.setSubdomain(request.getSubdomain());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setPlan(request.getPlan());
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);
        return mapToResponse(tenant);
    }

    public TenantResponse getTenant(String tenantId) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        return mapToResponse(tenant);
    }

    @Transactional
    public void suspendTenant(String tenantId) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        tenant.setStatus("SUSPENDED");
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);
    }

    @Transactional
    public void activateTenant(String tenantId) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        tenant.setStatus("ACTIVE");
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);
    }

    private String generateTenantId() {
        return "tenant-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setTenantId(tenant.getTenantId());
        response.setName(tenant.getName());
        response.setSubdomain(tenant.getSubdomain());
        response.setContactEmail(tenant.getContactEmail());
        response.setContactPhone(tenant.getContactPhone());
        response.setPlan(tenant.getPlan());
        response.setStatus(tenant.getStatus());
        response.setCreatedAt(tenant.getCreatedAt());
        return response;
    }
}
```

**`service/StoreService.java`**
```java
package com.supermarket.tenant.service;

import com.supermarket.tenant.dto.StoreRequest;
import com.supermarket.tenant.entity.Store;
import com.supermarket.tenant.entity.Tenant;
import com.supermarket.tenant.repository.StoreRepository;
import com.supermarket.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public Store createStore(String tenantId, StoreRequest request) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Store store = new Store();
        store.setTenant(tenant);
        store.setName(request.getName());
        store.setCode(request.getCode());
        store.setAddress(request.getAddress());
        store.setCity(request.getCity());
        store.setState(request.getState());
        store.setZipCode(request.getZipCode());
        store.setPhone(request.getPhone());
        store.setEmail(request.getEmail());
        store.setManagerName(request.getManagerName());
        store.setManagerPhone(request.getManagerPhone());
        store.setActive(request.isActive());
        store.setCreatedAt(LocalDateTime.now());
        return storeRepository.save(store);
    }

    public List<Store> getStoresByTenant(String tenantId) {
        return storeRepository.findByTenantTenantId(tenantId);
    }

    public Store getStore(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Store not found"));
    }

    @Transactional
    public Store updateStore(Long id, StoreRequest request) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        store.setName(request.getName());
        store.setCode(request.getCode());
        store.setAddress(request.getAddress());
        store.setCity(request.getCity());
        store.setState(request.getState());
        store.setZipCode(request.getZipCode());
        store.setPhone(request.getPhone());
        store.setEmail(request.getEmail());
        store.setManagerName(request.getManagerName());
        store.setManagerPhone(request.getManagerPhone());
        store.setActive(request.isActive());
        store.setUpdatedAt(LocalDateTime.now());
        return storeRepository.save(store);
    }

    @Transactional
    public void deleteStore(Long id) {
        storeRepository.deleteById(id);
    }
}
```

### 31.8 Controllers

**`controller/TenantController.java`** (admin only – should be secured with role ADMIN)
```java
package com.supermarket.tenant.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.tenant.dto.TenantRequest;
import com.supermarket.tenant.dto.TenantResponse;
import com.supermarket.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(@RequestBody TenantRequest request) {
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable String tenantId) {
        TenantResponse response = tenantService.getTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(@PathVariable String tenantId,
                                                                     @RequestBody TenantRequest request) {
        TenantResponse response = tenantService.updateTenant(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{tenantId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendTenant(@PathVariable String tenantId) {
        tenantService.suspendTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{tenantId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateTenant(@PathVariable String tenantId) {
        tenantService.activateTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

**`controller/StoreController.java`** (within a tenant context)
```java
package com.supermarket.tenant.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.tenant.dto.StoreRequest;
import com.supermarket.tenant.entity.Store;
import com.supermarket.tenant.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants/{tenantId}/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    public ResponseEntity<ApiResponse<Store>> createStore(@PathVariable String tenantId,
                                                          @RequestBody StoreRequest request) {
        Store store = storeService.createStore(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success(store));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Store>>> getStores(@PathVariable String tenantId) {
        List<Store> stores = storeService.getStoresByTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Store>> getStore(@PathVariable Long id) {
        Store store = storeService.getStore(id);
        return ResponseEntity.ok(ApiResponse.success(store));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Store>> updateStore(@PathVariable Long id,
                                                          @RequestBody StoreRequest request) {
        Store store = storeService.updateStore(id, request);
        return ResponseEntity.ok(ApiResponse.success(store));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStore(@PathVariable Long id) {
        storeService.deleteStore(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

### 31.9 Configuration

**`bootstrap.yml`**
```yaml
spring:
  application:
    name: tenant-service
  cloud:
    config:
      uri: http://config-service:8888
      fail-fast: true
```

**Config repo (`tenant-service.yml`)**
```yaml
server:
  port: 8089
```

**`Dockerfile`**
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/tenant-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 31.10 Update Docker Compose

Add tenant-service to `docker-compose.yml`:

```yaml
tenant-service:
  build: ./tenant-service
  container_name: tenant-service
  ports:
    - "8089:8089"
  depends_on:
    - postgres
    - service-discovery
    - config-service
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - SPRING_CONFIG_IMPORT=configserver:http://config-service:8888
  networks:
    - supermarket-network
```

### 31.11 Update API Gateway Routes

Add routes for tenant service in `GatewayConfig.java` (these should be accessible only to super-admin, so we may add role checks later):

```java
.route("tenant-service", r -> r
    .path("/api/tenants/**")
    .filters(f -> f.rewritePath("/api/tenants/(?<segment>.*)", "/tenants/${segment}")
            .filter(new AuthenticationFilter().apply(new AuthenticationFilter.Config())))
    .uri("lb://TENANT-SERVICE"))
```

### 31.12 Integration with Other Services

Now that we have a dedicated Tenant Service, other services can validate that a tenant exists (optional) and retrieve store information. However, for simplicity, we can keep the current approach where services only use the tenant ID from the header/JWT and don't validate it. The tenant service is primarily for management and onboarding.

### 31.13 Testing

1. Rebuild:
   ```bash
   mvn clean package
   docker-compose up --build
   ```

2. Create a tenant (need admin JWT):
   ```
   POST http://localhost:8080/api/tenants
   {
     "name": "Test Supermarket",
     "subdomain": "test",
     "contactEmail": "admin@test.com",
     "contactPhone": "1234567890",
     "plan": "PREMIUM"
   }
   ```

3. Get tenant by tenantId (returns generated tenantId like "tenant-abc123"):
   ```
   GET http://localhost:8080/api/tenants/tenant-abc123
   ```

4. Add a store:
   ```
   POST http://localhost:8080/api/tenants/tenant-abc123/stores
   {
     "name": "Main Street Store",
     "code": "STORE001",
     "address": "123 Main St",
     "city": "Anytown",
     "state": "CA",
     "zipCode": "90210",
     "phone": "555-1234",
     "email": "store@test.com",
     "managerName": "John Doe",
     "managerPhone": "555-5678",
     "active": true
   }
   ```

5. List stores:
   ```
   GET http://localhost:8080/api/tenants/tenant-abc123/stores
   ```

---

## 32. Next Steps

We have now implemented **Tenant Service** (Phase 4) as a dedicated service. The system now has:

- **Phase 1-2**: Infrastructure (Eureka, Config, Gateway)
- **Phase 3**: Auth Service
- **Phase 4**: Tenant Service (new)
- **Phase 5**: Product Service
- **Phase 6**: Inventory Service
- **Phase 8**: Order Service
- **Phase 9**: POS Service
- **Phase 10**: E-commerce Service
- **Phase 11**: Payment Service
- **Phase 12**: Loyalty Service

Remaining phases:
- **Phase 7: Customer Service** (customer profiles, addresses, segmentation)
- **Phase 13-14: Analytics & Reporting**
- **Phase 15: Inter-service communication enhancements** (sagas, event-driven patterns)
- **Phase 16+: Cross-cutting concerns** (logging, monitoring, security hardening, testing, deployment)

Given the user request to "continue", we can proceed with **Customer Service** (Phase 7) as it is the next logical service that many others (Loyalty, E-commerce, POS) rely on for consistent customer data.

Shall I proceed with **Customer Service**?