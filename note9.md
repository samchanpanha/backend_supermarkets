We continue building the Supermarket POS SaaS system by implementing the **Loyalty Service** (Phase 12). This service manages customer loyalty accounts, points accumulation and redemption, tier levels, and rewards. It integrates with other services via Kafka to listen to order and payment events and update points automatically.

---

## 29. Loyalty Service

### 29.1 Module Structure
```
loyalty-service/
├── pom.xml
├── Dockerfile
└── src/main/java/com/supermarket/loyalty/
    ├── LoyaltyApplication.java
    ├── entity/
    │   ├── LoyaltyAccount.java
    │   ├── PointsTransaction.java
    │   ├── Tier.java
    │   ├── Reward.java
    │   └── Coupon.java
    ├── repository/
    │   ├── LoyaltyAccountRepository.java
    │   ├── PointsTransactionRepository.java
    │   ├── TierRepository.java
    │   ├── RewardRepository.java
    │   └── CouponRepository.java
    ├── controller/
    │   ├── LoyaltyController.java
    │   └── AdminLoyaltyController.java
    ├── service/
    │   ├── LoyaltyService.java
    │   ├── PointsCalculator.java
    │   └── RewardService.java
    ├── consumer/
    │   └── OrderEventConsumer.java
    ├── dto/
    │   ├── LoyaltyAccountDto.java
    │   ├── PointsTransactionDto.java
    │   ├── EarnPointsRequest.java
    │   ├── RedeemPointsRequest.java
    │   └── CouponDto.java
    ├── filter/
    │   └── TenantFilter.java
    └── util/
        └── TenantContext.java
```

### 29.2 Dependencies (`pom.xml`)

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
    <artifactId>loyalty-service</artifactId>

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

### 29.3 Main Application

**`LoyaltyApplication.java`**
```java
package com.supermarket.loyalty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LoyaltyApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoyaltyApplication.class, args);
    }
}
```

### 29.4 Tenant Context and Filter (same as before)

**`util/TenantContext.java`**
```java
package com.supermarket.loyalty.util;

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
package com.supermarket.loyalty.filter;

import com.supermarket.loyalty.util.TenantContext;
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

### 29.5 Entities

**`entity/Tier.java`** (defines tier levels and rules)
```java
package com.supermarket.loyalty.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "tiers")
@Data
@NoArgsConstructor
public class Tier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;               // Bronze, Silver, Gold, Platinum

    private Integer minPoints;          // minimum points to be in this tier
    private BigDecimal pointsMultiplier; // e.g., 1.0, 1.5, 2.0

    private String benefits;            // JSON or description

    private boolean active;
}
```

**`entity/LoyaltyAccount.java`**
```java
package com.supermarket.loyalty.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_accounts")
@Data
@NoArgsConstructor
public class LoyaltyAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false, unique = true)
    private String customerId;          // reference to customer service

    private String cardNumber;          // loyalty card number

    private Integer totalPoints;        // current points balance
    private Integer lifetimePoints;     // total points earned

    @ManyToOne
    @JoinColumn(name = "tier_id")
    private Tier tier;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private boolean active;
}
```

**`entity/PointsTransaction.java`**
```java
package com.supermarket.loyalty.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "points_transactions")
@Data
@NoArgsConstructor
public class PointsTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private Long accountId;

    private String orderId;              // reference to order that generated/spent points

    @Column(nullable = false)
    private Integer points;               // positive for earn, negative for redeem

    @Column(nullable = false)
    private String type;                  // EARN, REDEEM, ADJUST, EXPIRE

    private String description;

    private LocalDateTime createdAt;
}
```

**`entity/Reward.java`** (catalog of rewards)
```java
package com.supermarket.loyalty.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "rewards")
@Data
@NoArgsConstructor
public class Reward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    private String name;
    private String description;
    private Integer pointsRequired;

    private String rewardType;            // DISCOUNT, FREE_PRODUCT, SHIPPING, etc.
    private String rewardValue;           // e.g., "10%", "5.00", product_id

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    private Integer stock;                 // limited quantity, null for unlimited
    private boolean active;
}
```

**`entity/Coupon.java`** (generated when a reward is redeemed)
```java
package com.supermarket.loyalty.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String customerId;

    private String code;                   // unique coupon code

    private String description;

    private String discountType;            // PERCENTAGE, FIXED_AMOUNT
    private BigDecimal discountValue;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    private boolean used;
    private LocalDateTime usedAt;

    private String applicableProducts;      // JSON list of product IDs, or null for all
}
```

### 29.6 Repositories

**`repository/TierRepository.java`**
```java
package com.supermarket.loyalty.repository;

import com.supermarket.loyalty.entity.Tier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TierRepository extends JpaRepository<Tier, Long> {
    List<Tier> findByTenantIdAndActiveTrue(String tenantId);
    Optional<Tier> findByTenantIdAndMinPointsLessThanEqualOrderByMinPointsDesc(String tenantId, int points);
}
```

**`repository/LoyaltyAccountRepository.java`**
```java
package com.supermarket.loyalty.repository;

import com.supermarket.loyalty.entity.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {
    Optional<LoyaltyAccount> findByTenantIdAndCustomerId(String tenantId, String customerId);
}
```

**`repository/PointsTransactionRepository.java`**
```java
package com.supermarket.loyalty.repository;

import com.supermarket.loyalty.entity.PointsTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {
    Page<PointsTransaction> findByTenantIdAndAccountId(String tenantId, Long accountId, Pageable pageable);
}
```

**`repository/RewardRepository.java`**
```java
package com.supermarket.loyalty.repository;

import com.supermarket.loyalty.entity.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RewardRepository extends JpaRepository<Reward, Long> {
    List<Reward> findByTenantIdAndActiveTrue(String tenantId);
}
```

**`repository/CouponRepository.java`**
```java
package com.supermarket.loyalty.repository;

import com.supermarket.loyalty.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByTenantIdAndCode(String tenantId, String code);
    Optional<Coupon> findByTenantIdAndCustomerIdAndUsedFalse(String tenantId, String customerId);
}
```

### 29.7 DTOs

**`dto/LoyaltyAccountDto.java`**
```java
package com.supermarket.loyalty.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LoyaltyAccountDto {
    private Long id;
    private String customerId;
    private String cardNumber;
    private Integer totalPoints;
    private Integer lifetimePoints;
    private String tierName;
    private BigDecimal pointsMultiplier;
    private LocalDateTime createdAt;
}
```

**`dto/PointsTransactionDto.java`**
```java
package com.supermarket.loyalty.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PointsTransactionDto {
    private Long id;
    private Integer points;
    private String type;
    private String description;
    private String orderId;
    private LocalDateTime createdAt;
}
```

**`dto/EarnPointsRequest.java`**
```java
package com.supermarket.loyalty.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EarnPointsRequest {
    private String customerId;
    private String orderId;
    private BigDecimal orderAmount;
    private String description;
}
```

**`dto/RedeemPointsRequest.java`**
```java
package com.supermarket.loyalty.dto;

import lombok.Data;

@Data
public class RedeemPointsRequest {
    private String customerId;
    private Integer points;
    private String orderId;
    private String description;
}
```

**`dto/CouponDto.java`**
```java
package com.supermarket.loyalty.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponDto {
    private String code;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private LocalDateTime validUntil;
}
```

### 29.8 Service Layer

**`service/PointsCalculator.java`** (strategy for points calculation)
```java
package com.supermarket.loyalty.service;

import com.supermarket.loyalty.entity.Tier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PointsCalculator {

    public int calculatePoints(BigDecimal amount, Tier tier) {
        // Example: 1 point per dollar spent, multiplied by tier multiplier
        BigDecimal multiplier = tier != null ? tier.getPointsMultiplier() : BigDecimal.ONE;
        return amount.multiply(multiplier).intValue();
    }
}
```

**`service/LoyaltyService.java`**
```java
package com.supermarket.loyalty.service;

import com.supermarket.loyalty.dto.*;
import com.supermarket.loyalty.entity.*;
import com.supermarket.loyalty.repository.*;
import com.supermarket.loyalty.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyAccountRepository accountRepository;
    private final PointsTransactionRepository transactionRepository;
    private final TierRepository tierRepository;
    private final RewardRepository rewardRepository;
    private final CouponRepository couponRepository;
    private final PointsCalculator pointsCalculator;

    @Transactional
    public LoyaltyAccountDto earnPoints(EarnPointsRequest request) {
        String tenantId = TenantContext.getCurrentTenant();
        LoyaltyAccount account = getOrCreateAccount(request.getCustomerId(), tenantId);

        Tier tier = account.getTier();
        int pointsEarned = pointsCalculator.calculatePoints(request.getOrderAmount(), tier);

        // Update account
        account.setTotalPoints(account.getTotalPoints() + pointsEarned);
        account.setLifetimePoints(account.getLifetimePoints() + pointsEarned);
        account.setUpdatedAt(LocalDateTime.now());

        // Check if tier needs to be upgraded based on lifetime points
        updateTier(account);

        accountRepository.save(account);

        // Record transaction
        PointsTransaction transaction = new PointsTransaction();
        transaction.setTenantId(tenantId);
        transaction.setAccountId(account.getId());
        transaction.setOrderId(request.getOrderId());
        transaction.setPoints(pointsEarned);
        transaction.setType("EARN");
        transaction.setDescription(request.getDescription());
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        return mapToDto(account);
    }

    @Transactional
    public CouponDto redeemPoints(RedeemPointsRequest request) {
        String tenantId = TenantContext.getCurrentTenant();
        LoyaltyAccount account = accountRepository.findByTenantIdAndCustomerId(tenantId, request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Loyalty account not found"));

        if (account.getTotalPoints() < request.getPoints()) {
            throw new RuntimeException("Insufficient points");
        }

        // Determine reward based on points (simplified: $1 off per 100 points)
        int discountDollars = request.getPoints() / 100;
        if (discountDollars == 0) {
            throw new RuntimeException("Points must be at least 100 to redeem");
        }

        // Deduct points
        account.setTotalPoints(account.getTotalPoints() - request.getPoints());
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        // Record transaction
        PointsTransaction transaction = new PointsTransaction();
        transaction.setTenantId(tenantId);
        transaction.setAccountId(account.getId());
        transaction.setOrderId(request.getOrderId());
        transaction.setPoints(-request.getPoints());
        transaction.setType("REDEEM");
        transaction.setDescription(request.getDescription());
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Generate coupon
        Coupon coupon = new Coupon();
        coupon.setTenantId(tenantId);
        coupon.setCustomerId(request.getCustomerId());
        coupon.setCode(generateCouponCode());
        coupon.setDescription("Redeemed " + request.getPoints() + " points");
        coupon.setDiscountType("FIXED_AMOUNT");
        coupon.setDiscountValue(BigDecimal.valueOf(discountDollars));
        coupon.setValidFrom(LocalDateTime.now());
        coupon.setValidUntil(LocalDateTime.now().plusMonths(3));
        coupon.setUsed(false);
        couponRepository.save(coupon);

        CouponDto dto = new CouponDto();
        dto.setCode(coupon.getCode());
        dto.setDescription(coupon.getDescription());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setValidUntil(coupon.getValidUntil());
        return dto;
    }

    @Transactional
    public void useCoupon(String code) {
        String tenantId = TenantContext.getCurrentTenant();
        Coupon coupon = couponRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        if (coupon.isUsed()) {
            throw new RuntimeException("Coupon already used");
        }
        if (coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Coupon expired");
        }
        coupon.setUsed(true);
        coupon.setUsedAt(LocalDateTime.now());
        couponRepository.save(coupon);
    }

    public LoyaltyAccountDto getAccount(String customerId) {
        String tenantId = TenantContext.getCurrentTenant();
        LoyaltyAccount account = accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseGet(() -> createAccount(tenantId, customerId));
        return mapToDto(account);
    }

    public List<PointsTransactionDto> getTransactionHistory(String customerId) {
        String tenantId = TenantContext.getCurrentTenant();
        LoyaltyAccount account = accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return transactionRepository.findByTenantIdAndAccountId(tenantId, account.getId(), Pageable.unpaged())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<Reward> getAvailableRewards() {
        String tenantId = TenantContext.getCurrentTenant();
        return rewardRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    private LoyaltyAccount getOrCreateAccount(String customerId, String tenantId) {
        return accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseGet(() -> createAccount(tenantId, customerId));
    }

    private LoyaltyAccount createAccount(String tenantId, String customerId) {
        LoyaltyAccount account = new LoyaltyAccount();
        account.setTenantId(tenantId);
        account.setCustomerId(customerId);
        account.setCardNumber(generateCardNumber());
        account.setTotalPoints(0);
        account.setLifetimePoints(0);
        account.setCreatedAt(LocalDateTime.now());
        account.setActive(true);
        // Assign default tier (Bronze)
        Tier defaultTier = tierRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .filter(t -> "Bronze".equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElse(null);
        account.setTier(defaultTier);
        return accountRepository.save(account);
    }

    private void updateTier(LoyaltyAccount account) {
        String tenantId = account.getTenantId();
        Tier newTier = tierRepository.findByTenantIdAndMinPointsLessThanEqualOrderByMinPointsDesc(
                tenantId, account.getLifetimePoints()).orElse(null);
        if (newTier != null && !newTier.equals(account.getTier())) {
            account.setTier(newTier);
        }
    }

    private String generateCardNumber() {
        return "L" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String generateCouponCode() {
        return "COUPON-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private LoyaltyAccountDto mapToDto(LoyaltyAccount account) {
        LoyaltyAccountDto dto = new LoyaltyAccountDto();
        dto.setId(account.getId());
        dto.setCustomerId(account.getCustomerId());
        dto.setCardNumber(account.getCardNumber());
        dto.setTotalPoints(account.getTotalPoints());
        dto.setLifetimePoints(account.getLifetimePoints());
        if (account.getTier() != null) {
            dto.setTierName(account.getTier().getName());
            dto.setPointsMultiplier(account.getTier().getPointsMultiplier());
        }
        dto.setCreatedAt(account.getCreatedAt());
        return dto;
    }

    private PointsTransactionDto mapToDto(PointsTransaction txn) {
        PointsTransactionDto dto = new PointsTransactionDto();
        dto.setId(txn.getId());
        dto.setPoints(txn.getPoints());
        dto.setType(txn.getType());
        dto.setDescription(txn.getDescription());
        dto.setOrderId(txn.getOrderId());
        dto.setCreatedAt(txn.getCreatedAt());
        return dto;
    }
}
```

**`service/RewardService.java`** (admin functions for managing rewards)
```java
package com.supermarket.loyalty.service;

import com.supermarket.loyalty.entity.Reward;
import com.supermarket.loyalty.repository.RewardRepository;
import com.supermarket.loyalty.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;

    public Reward createReward(Reward reward) {
        String tenantId = TenantContext.getCurrentTenant();
        reward.setTenantId(tenantId);
        return rewardRepository.save(reward);
    }

    public Reward updateReward(Long id, Reward rewardDetails) {
        String tenantId = TenantContext.getCurrentTenant();
        Reward reward = rewardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reward not found"));
        if (!reward.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }
        reward.setName(rewardDetails.getName());
        reward.setDescription(rewardDetails.getDescription());
        reward.setPointsRequired(rewardDetails.getPointsRequired());
        reward.setRewardType(rewardDetails.getRewardType());
        reward.setRewardValue(rewardDetails.getRewardValue());
        reward.setValidFrom(rewardDetails.getValidFrom());
        reward.setValidUntil(rewardDetails.getValidUntil());
        reward.setStock(rewardDetails.getStock());
        reward.setActive(rewardDetails.isActive());
        return rewardRepository.save(reward);
    }

    public void deleteReward(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        Reward reward = rewardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reward not found"));
        if (!reward.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }
        rewardRepository.delete(reward);
    }

    public List<Reward> getAllRewards() {
        String tenantId = TenantContext.getCurrentTenant();
        return rewardRepository.findByTenantIdAndActiveTrue(tenantId);
    }
}
```

### 29.9 Kafka Consumer for Order Events

**`consumer/OrderEventConsumer.java`**
```java
package com.supermarket.loyalty.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.loyalty.dto.EarnPointsRequest;
import com.supermarket.loyalty.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final LoyaltyService loyaltyService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "loyalty-service")
    public void consumeOrderEvent(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = (String) event.get("eventType");
            String tenantId = (String) event.get("tenantId");
            String orderId = event.get("orderId").toString();
            String customerId = (String) event.get("customerId");
            Number total = (Number) event.get("total");

            if ("CREATED".equals(eventType) && customerId != null) {
                // Set tenant context for the duration of processing
                TenantContext.setCurrentTenant(tenantId);
                try {
                    EarnPointsRequest request = new EarnPointsRequest();
                    request.setCustomerId(customerId);
                    request.setOrderId(orderId);
                    request.setOrderAmount(new java.math.BigDecimal(total.toString()));
                    request.setDescription("Order completed");
                    loyaltyService.earnPoints(request);
                    log.info("Earned points for order: {}", orderId);
                } finally {
                    TenantContext.clear();
                }
            }
        } catch (Exception e) {
            log.error("Error processing order event", e);
        }
    }
}
```

### 29.10 REST Controllers

**`controller/LoyaltyController.java`** (public endpoints for customers)
```java
package com.supermarket.loyalty.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.loyalty.dto.*;
import com.supermarket.loyalty.entity.Reward;
import com.supermarket.loyalty.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/account")
    public ResponseEntity<ApiResponse<LoyaltyAccountDto>> getAccount(@RequestParam String customerId) {
        LoyaltyAccountDto account = loyaltyService.getAccount(customerId);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<PointsTransactionDto>>> getTransactions(@RequestParam String customerId) {
        List<PointsTransactionDto> transactions = loyaltyService.getTransactionHistory(customerId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @PostMapping("/earn")
    public ResponseEntity<ApiResponse<LoyaltyAccountDto>> earnPoints(@RequestBody EarnPointsRequest request) {
        LoyaltyAccountDto account = loyaltyService.earnPoints(request);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse<CouponDto>> redeemPoints(@RequestBody RedeemPointsRequest request) {
        CouponDto coupon = loyaltyService.redeemPoints(request);
        return ResponseEntity.ok(ApiResponse.success(coupon));
    }

    @PostMapping("/coupons/{code}/use")
    public ResponseEntity<ApiResponse<Void>> useCoupon(@PathVariable String code) {
        loyaltyService.useCoupon(code);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/rewards")
    public ResponseEntity<ApiResponse<List<Reward>>> getRewards() {
        List<Reward> rewards = loyaltyService.getAvailableRewards();
        return ResponseEntity.ok(ApiResponse.success(rewards));
    }
}
```

**`controller/AdminLoyaltyController.java`** (admin endpoints for managing tiers and rewards)
```java
package com.supermarket.loyalty.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.loyalty.entity.Reward;
import com.supermarket.loyalty.entity.Tier;
import com.supermarket.loyalty.repository.TierRepository;
import com.supermarket.loyalty.service.RewardService;
import com.supermarket.loyalty.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/loyalty")
@RequiredArgsConstructor
public class AdminLoyaltyController {

    private final TierRepository tierRepository;
    private final RewardService rewardService;

    // Tier management
    @GetMapping("/tiers")
    public ResponseEntity<ApiResponse<List<Tier>>> getTiers() {
        String tenantId = TenantContext.getCurrentTenant();
        List<Tier> tiers = tierRepository.findByTenantIdAndActiveTrue(tenantId);
        return ResponseEntity.ok(ApiResponse.success(tiers));
    }

    @PostMapping("/tiers")
    public ResponseEntity<ApiResponse<Tier>> createTier(@RequestBody Tier tier) {
        String tenantId = TenantContext.getCurrentTenant();
        tier.setTenantId(tenantId);
        Tier saved = tierRepository.save(tier);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PutMapping("/tiers/{id}")
    public ResponseEntity<ApiResponse<Tier>> updateTier(@PathVariable Long id, @RequestBody Tier tierDetails) {
        String tenantId = TenantContext.getCurrentTenant();
        Tier tier = tierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tier not found"));
        if (!tier.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }
        tier.setName(tierDetails.getName());
        tier.setMinPoints(tierDetails.getMinPoints());
        tier.setPointsMultiplier(tierDetails.getPointsMultiplier());
        tier.setBenefits(tierDetails.getBenefits());
        tier.setActive(tierDetails.isActive());
        tierRepository.save(tier);
        return ResponseEntity.ok(ApiResponse.success(tier));
    }

    // Reward management
    @GetMapping("/rewards")
    public ResponseEntity<ApiResponse<List<Reward>>> getAllRewards() {
        List<Reward> rewards = rewardService.getAllRewards();
        return ResponseEntity.ok(ApiResponse.success(rewards));
    }

    @PostMapping("/rewards")
    public ResponseEntity<ApiResponse<Reward>> createReward(@RequestBody Reward reward) {
        Reward saved = rewardService.createReward(reward);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PutMapping("/rewards/{id}")
    public ResponseEntity<ApiResponse<Reward>> updateReward(@PathVariable Long id, @RequestBody Reward reward) {
        Reward updated = rewardService.updateReward(id, reward);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/rewards/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReward(@PathVariable Long id) {
        rewardService.deleteReward(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

### 29.11 Configuration

**`bootstrap.yml`**
```yaml
spring:
  application:
    name: loyalty-service
  cloud:
    config:
      uri: http://config-service:8888
      fail-fast: true
```

**Config repo (`loyalty-service.yml`)**
```yaml
server:
  port: 8088

spring:
  kafka:
    consumer:
      bootstrap-servers: kafka:9092
      group-id: loyalty-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

**`Dockerfile`**
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/loyalty-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 29.12 Update Docker Compose

Add loyalty-service to `docker-compose.yml`:

```yaml
loyalty-service:
  build: ./loyalty-service
  container_name: loyalty-service
  ports:
    - "8088:8088"
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
```

### 29.13 Update API Gateway Routes

Add routes for loyalty service in `GatewayConfig.java`:

```java
.route("loyalty", r -> r
    .path("/api/loyalty/**")
    .filters(f -> f.rewritePath("/api/loyalty/(?<segment>.*)", "/loyalty/${segment}")
            .filter(new AuthenticationFilter().apply(new AuthenticationFilter.Config())))
    .uri("lb://LOYALTY-SERVICE"))
.route("admin-loyalty", r -> r
    .path("/api/admin/loyalty/**")
    .filters(f -> f.rewritePath("/api/admin/loyalty/(?<segment>.*)", "/admin/loyalty/${segment}")
            .filter(new AuthenticationFilter().apply(new AuthenticationFilter.Config())))
    .uri("lb://LOYALTY-SERVICE"))
```

### 29.14 Initial Data Setup

We should create default tiers (Bronze, Silver, Gold) when a tenant is created. Since we don't have a Tenant Service yet, we can add a `@PostConstruct` in the main class to insert default tiers if they don't exist.

**In `LoyaltyApplication.java` add:**
```java
@Bean
public CommandLineRunner initTiers(TierRepository tierRepository) {
    return args -> {
        String tenantId = "default"; // or get from config
        if (tierRepository.findByTenantIdAndActiveTrue(tenantId).isEmpty()) {
            Tier bronze = new Tier();
            bronze.setTenantId(tenantId);
            bronze.setName("Bronze");
            bronze.setMinPoints(0);
            bronze.setPointsMultiplier(BigDecimal.ONE);
            bronze.setBenefits("Standard points earning");
            bronze.setActive(true);
            tierRepository.save(bronze);

            Tier silver = new Tier();
            silver.setTenantId(tenantId);
            silver.setName("Silver");
            silver.setMinPoints(1000);
            silver.setPointsMultiplier(new BigDecimal("1.2"));
            silver.setBenefits("20% bonus points");
            silver.setActive(true);
            tierRepository.save(silver);

            Tier gold = new Tier();
            gold.setTenantId(tenantId);
            gold.setName("Gold");
            gold.setMinPoints(5000);
            gold.setPointsMultiplier(new BigDecimal("1.5"));
            gold.setBenefits("50% bonus points, free shipping");
            gold.setActive(true);
            tierRepository.save(gold);
        }
    };
}
```

### 29.15 Testing

1. Rebuild all:
   ```bash
   mvn clean package
   docker-compose up --build
   ```

2. Get JWT token.

3. Create a loyalty account implicitly by getting account:
   ```
   GET http://localhost:8080/api/loyalty/account?customerId=cust123
   ```

4. Earn points manually:
   ```
   POST http://localhost:8080/api/loyalty/earn
   {
     "customerId": "cust123",
     "orderId": "ORD-123",
     "orderAmount": 50.00,
     "description": "Test purchase"
   }
   ```

5. Check account:
   ```
   GET http://localhost:8080/api/loyalty/account?customerId=cust123
   ```

6. Redeem points:
   ```
   POST http://localhost:8080/api/loyalty/redeem
   {
     "customerId": "cust123",
     "points": 100,
     "orderId": "ORD-124",
     "description": "Discount on next order"
   }
   ```

7. Use coupon:
   ```
   POST http://localhost:8080/api/loyalty/coupons/COUPON-XXXXXX/use
   ```

8. Admin: create reward:
   ```
   POST http://localhost:8080/api/admin/loyalty/rewards
   {
     "name": "10% Discount",
     "description": "Get 10% off your next purchase",
     "pointsRequired": 500,
     "rewardType": "DISCOUNT",
     "rewardValue": "10",
     "validFrom": "2025-01-01T00:00:00",
     "validUntil": "2025-12-31T23:59:59",
     "active": true
   }
   ```

9. Get available rewards:
   ```
   GET http://localhost:8080/api/loyalty/rewards
   ```

---

## 30. Next Steps

We have now completed **Phase 12: Loyalty Service**.

Remaining major phases:
- **Phase 4: Tenant Service** (dedicated service for tenant management, subscription, stores)
- **Phase 7: Customer Service** (customer profiles, addresses, segmentation)
- **Phase 13-14: Analytics & Reporting** (could be combined)
- **Phase 15: Inter-service communication enhancements** (event-driven patterns, sagas)
- **Phase 16+: Cross-cutting concerns** (logging, monitoring, security hardening, testing, deployment)

Given the user's request to "continue", we can proceed with any of these. **Tenant Service** (Phase 4) is foundational and would be a good choice, as it would allow proper multi-tenant isolation and store management, which many services currently hardcode as "default" tenant. Alternatively, **Customer Service** (Phase 7) would complement loyalty and ecommerce. Which would you like next?