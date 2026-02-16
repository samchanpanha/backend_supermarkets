We'll now implement **Customer Service** (Phase 7). This service manages customer profiles, addresses, and segmentation. It will be used by Order, Loyalty, E-commerce, and POS services to store and retrieve customer information.

---

## 33. Customer Service

### 33.1 Module Structure
```
customer-service/
├── pom.xml
├── Dockerfile
└── src/main/java/com/supermarket/customer/
    ├── CustomerApplication.java
    ├── entity/
    │   ├── Customer.java
    │   └── Address.java
    ├── repository/
    │   ├── CustomerRepository.java
    │   └── AddressRepository.java
    ├── controller/
    │   └── CustomerController.java
    ├── service/
    │   └── CustomerService.java
    ├── dto/
    │   ├── CustomerRequest.java
    │   ├── CustomerResponse.java
    │   └── AddressRequest.java
    ├── filter/
    │   └── TenantFilter.java
    └── util/
        └── TenantContext.java
```

### 33.2 Dependencies (`pom.xml`)

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
    <artifactId>customer-service</artifactId>

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

### 33.3 Main Application

**`CustomerApplication.java`**
```java
package com.supermarket.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CustomerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerApplication.class, args);
    }
}
```

### 33.4 Tenant Context and Filter

**`util/TenantContext.java`**
```java
package com.supermarket.customer.util;

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
package com.supermarket.customer.filter;

import com.supermarket.customer.util.TenantContext;
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
            tenantId = "default";
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

### 33.5 Entities

**`entity/Customer.java`**
```java
package com.supermarket.customer.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(unique = true)
    private String email;               // can be used as login

    private String phone;

    @Column(nullable = false)
    private String firstName;

    private String lastName;

    private LocalDate dateOfBirth;

    private String gender;

    // Marketing preferences
    private boolean emailOptIn;
    private boolean smsOptIn;

    private String customerType;         // REGULAR, VIP, WHOLESALE

    private String notes;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Address> addresses = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**`entity/Address.java`**
```java
package com.supermarket.customer.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    private boolean isDefault;           // default shipping address

    private String type;                  // HOME, WORK, OTHER
}
```

### 33.6 Repositories

**`repository/CustomerRepository.java`**
```java
package com.supermarket.customer.repository;

import com.supermarket.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByTenantIdAndEmail(String tenantId, String email);
    Page<Customer> findByTenantId(String tenantId, Pageable pageable);
}
```

**`repository/AddressRepository.java`**
```java
package com.supermarket.customer.repository;

import com.supermarket.customer.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByCustomerId(Long customerId);
}
```

### 33.7 DTOs

**`dto/AddressRequest.java`**
```java
package com.supermarket.customer.dto;

import lombok.Data;

@Data
public class AddressRequest {
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private boolean isDefault;
    private String type;
}
```

**`dto/CustomerRequest.java`** (for creating/updating customer)
```java
package com.supermarket.customer.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CustomerRequest {
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private boolean emailOptIn;
    private boolean smsOptIn;
    private String customerType;
    private String notes;
    private List<AddressRequest> addresses;
}
```

**`dto/CustomerResponse.java`** (for API responses)
```java
package com.supermarket.customer.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerResponse {
    private Long id;
    private String tenantId;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private boolean emailOptIn;
    private boolean smsOptIn;
    private String customerType;
    private String notes;
    private List<AddressDto> addresses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class AddressDto {
        private Long id;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private boolean isDefault;
        private String type;
    }
}
```

### 33.8 Service Layer

**`service/CustomerService.java`**
```java
package com.supermarket.customer.service;

import com.supermarket.customer.dto.AddressRequest;
import com.supermarket.customer.dto.CustomerRequest;
import com.supermarket.customer.dto.CustomerResponse;
import com.supermarket.customer.entity.Address;
import com.supermarket.customer.entity.Customer;
import com.supermarket.customer.repository.AddressRepository;
import com.supermarket.customer.repository.CustomerRepository;
import com.supermarket.customer.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        String tenantId = TenantContext.getCurrentTenant();

        if (request.getEmail() != null) {
            customerRepository.findByTenantIdAndEmail(tenantId, request.getEmail())
                    .ifPresent(c -> {
                        throw new RuntimeException("Customer with this email already exists");
                    });
        }

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setGender(request.getGender());
        customer.setEmailOptIn(request.isEmailOptIn());
        customer.setSmsOptIn(request.isSmsOptIn());
        customer.setCustomerType(request.getCustomerType() != null ? request.getCustomerType() : "REGULAR");
        customer.setNotes(request.getNotes());
        customer.setCreatedAt(LocalDateTime.now());

        customer = customerRepository.save(customer);

        // Save addresses
        if (request.getAddresses() != null) {
            for (AddressRequest addrReq : request.getAddresses()) {
                Address address = new Address();
                address.setCustomer(customer);
                address.setAddressLine1(addrReq.getAddressLine1());
                address.setAddressLine2(addrReq.getAddressLine2());
                address.setCity(addrReq.getCity());
                address.setState(addrReq.getState());
                address.setZipCode(addrReq.getZipCode());
                address.setCountry(addrReq.getCountry());
                address.setDefault(addrReq.isDefault());
                address.setType(addrReq.getType());
                addressRepository.save(address);
            }
        }

        return mapToResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        String tenantId = TenantContext.getCurrentTenant();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (!customer.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }

        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setGender(request.getGender());
        customer.setEmailOptIn(request.isEmailOptIn());
        customer.setSmsOptIn(request.isSmsOptIn());
        customer.setCustomerType(request.getCustomerType());
        customer.setNotes(request.getNotes());
        customer.setUpdatedAt(LocalDateTime.now());

        customerRepository.save(customer);

        // For simplicity, we are not updating addresses here; could implement separate endpoints

        return mapToResponse(customer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (!customer.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }
        customerRepository.delete(customer);
    }

    public CustomerResponse getCustomer(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (!customer.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }
        return mapToResponse(customer);
    }

    public CustomerResponse getCustomerByEmail(String email) {
        String tenantId = TenantContext.getCurrentTenant();
        Customer customer = customerRepository.findByTenantIdAndEmail(tenantId, email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return mapToResponse(customer);
    }

    public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
        String tenantId = TenantContext.getCurrentTenant();
        return customerRepository.findByTenantId(tenantId, pageable)
                .map(this::mapToResponse);
    }

    private CustomerResponse mapToResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setTenantId(customer.getTenantId());
        response.setEmail(customer.getEmail());
        response.setPhone(customer.getPhone());
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setDateOfBirth(customer.getDateOfBirth());
        response.setGender(customer.getGender());
        response.setEmailOptIn(customer.isEmailOptIn());
        response.setSmsOptIn(customer.isSmsOptIn());
        response.setCustomerType(customer.getCustomerType());
        response.setNotes(customer.getNotes());
        response.setCreatedAt(customer.getCreatedAt());
        response.setUpdatedAt(customer.getUpdatedAt());

        if (customer.getAddresses() != null) {
            response.setAddresses(customer.getAddresses().stream()
                    .map(this::mapAddressToDto)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private CustomerResponse.AddressDto mapAddressToDto(Address address) {
        CustomerResponse.AddressDto dto = new CustomerResponse.AddressDto();
        dto.setId(address.getId());
        dto.setAddressLine1(address.getAddressLine1());
        dto.setAddressLine2(address.getAddressLine2());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setZipCode(address.getZipCode());
        dto.setCountry(address.getCountry());
        dto.setDefault(address.isDefault());
        dto.setType(address.getType());
        return dto;
    }
}
```

### 33.9 REST Controller

**`controller/CustomerController.java`**
```java
package com.supermarket.customer.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.customer.dto.CustomerRequest;
import com.supermarket.customer.dto.CustomerResponse;
import com.supermarket.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(@PathVariable Long id) {
        CustomerResponse response = customerService.getCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByEmail(@PathVariable String email) {
        CustomerResponse response = customerService.getCustomerByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(@PathVariable Long id,
                                                                        @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getAllCustomers(Pageable pageable) {
        Page<CustomerResponse> customers = customerService.getAllCustomers(pageable);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }
}
```

### 33.10 Configuration

**`bootstrap.yml`**
```yaml
spring:
  application:
    name: customer-service
  cloud:
    config:
      uri: http://config-service:8888
      fail-fast: true
```

**Config repo (`customer-service.yml`)**
```yaml
server:
  port: 8090
```

**`Dockerfile`**
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/customer-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 33.11 Update Docker Compose

Add customer-service to `docker-compose.yml`:

```yaml
customer-service:
  build: ./customer-service
  container_name: customer-service
  ports:
    - "8090:8090"
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

### 33.12 Update API Gateway Routes

Add route for customer service:

```java
.route("customer-service", r -> r
    .path("/api/customers/**")
    .filters(f -> f.rewritePath("/api/customers/(?<segment>.*)", "/customers/${segment}")
            .filter(new AuthenticationFilter().apply(new AuthenticationFilter.Config())))
    .uri("lb://CUSTOMER-SERVICE"))
```

### 33.13 Testing

1. Rebuild:
   ```bash
   mvn clean package
   docker-compose up --build
   ```

2. Create a customer:
   ```
   POST http://localhost:8080/api/customers
   Authorization: Bearer <token>
   {
     "email": "john@example.com",
     "firstName": "John",
     "lastName": "Doe",
     "phone": "555-1234",
     "emailOptIn": true,
     "customerType": "REGULAR",
     "addresses": [
       {
         "addressLine1": "123 Main St",
         "city": "Anytown",
         "state": "CA",
         "zipCode": "90210",
         "country": "USA",
         "default": true,
         "type": "HOME"
       }
     ]
   }
   ```

3. Get customer by ID:
   ```
   GET http://localhost:8080/api/customers/1
   ```

4. Get all customers (paginated):
   ```
   GET http://localhost:8080/api/customers?page=0&size=10
   ```

---

## 34. Next Steps

We have now implemented **Customer Service** (Phase 7). The system is nearly feature-complete.

Remaining phases:
- **Phase 13-14: Analytics & Reporting** – could be combined into one service.
- **Phase 15: Inter-service communication enhancements** – implementing sagas for distributed transactions, idempotency, etc.
- **Phase 16+: Cross-cutting concerns** – centralized logging (ELK), monitoring (Prometheus/Grafana), security hardening, API documentation, testing, CI/CD, deployment.

Given the request to "continue", we can either implement **Analytics & Reporting** or focus on infrastructure improvements like **centralized logging** or **monitoring**.

Which would you prefer? Or would you like me to implement a specific service/feature?