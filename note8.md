We are asked to "continue" after implementing the E-commerce Service. The next logical phase is Loyalty Service (Phase 12). Let's implement it.

We'll design a loyalty program with points earning and redemption, tiers (Bronze, Silver, Gold), and coupon generation. It will integrate with Order and Payment services via Kafka events.

29. Loyalty Service
29.1 Module Structure
text
loyalty-service/
├── pom.xml
├── Dockerfile
└── src/main/java/com/supermarket/loyalty/
    ├── LoyaltyApplication.java
    ├── entity/
    │   ├── LoyaltyAccount.java
    │   ├── PointsTransaction.java
    │   ├── Reward.java
    │   ├── Tier.java
    │   └── Coupon.java
    ├── repository/
    │   ├── LoyaltyAccountRepository.java
    │   ├── PointsTransactionRepository.java
    │   ├── RewardRepository.java
    │   ├── TierRepository.java
    │   └── CouponRepository.java
    ├── controller/
    │   ├── LoyaltyController.java
    │   └── CouponController.java
    ├── service/
    │   ├── LoyaltyService.java
    │   └── PointsCalculator.java
    ├── consumer/
    │   └── OrderEventConsumer.java
    ├── filter/
    │   └── TenantFilter.java
    └── util/
        └── TenantContext.java
29.2 Dependencies (pom.xml)
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
29.3 Main Application
LoyaltyApplication.java

java
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
29.4 Tenant Context and Filter (same as before)
29.5 Entities
entity/Tier.java

java
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
    private String name; // BRONZE, SILVER, GOLD

    private Integer minPoints; // minimum points to be in this tier
    private BigDecimal pointsMultiplier; // e.g., 1.0, 1.5, 2.0

    private String description;
}
entity/LoyaltyAccount.java

java
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
    private String customerId;

    private Integer pointsBalance;

    private Integer lifetimePoints;

    @ManyToOne
    @JoinColumn(name = "tier_id")
    private Tier tier;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LocalDateTime lastTransactionDate;
}
entity/PointsTransaction.java

java
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

    private Integer points; // positive for earn, negative for redeem

    private String transactionType; // EARN, REDEEM, ADJUST, EXPIRE

    private String referenceId; // order ID or payment ID

    private String description;

    private LocalDateTime transactionDate;

    private LocalDateTime expiryDate; // if points expire
}
entity/Reward.java

java
package com.supermarket.loyalty.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private String rewardType; // DISCOUNT, FREE_PRODUCT, SHIPPING

    private String rewardValue; // e.g., "10" for $10 off, or product ID

    private Boolean active;
}
entity/Coupon.java

java
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

    @Column(nullable = false, unique = true)
    private String code;

    private String description;

    private String discountType; // PERCENTAGE, FIXED
    private Double discountValue;

    private String applicableTo; // ALL, CATEGORY, PRODUCT
    private Long applicableId;   // category or product ID

    private Integer minOrderAmount;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    private Integer usageLimit;      // total uses
    private Integer usageCount;

    private String customerId;       // if assigned to specific customer

    private Boolean active;
}
29.6 Repositories
repository/TierRepository.java

java
package com.supermarket.loyalty.repository;

import com.supermarket.loyalty.entity.Tier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TierRepository extends JpaRepository<Tier, Long> {
    Optional<Tier> findByTenantIdAndName(String tenantId, String name);
}
repository/LoyaltyAccountRepository.java

java
package com.supermarket.loyalty.repository;

import com.supermarket.loyalty.entity.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {
    Optional<LoyaltyAccount> findByTenantIdAndCustomerId(String tenantId, String customerId);
}
repository/PointsTransactionRepository.java

java
package com.supermarket.loyalty.repository;

import com.supermarket.loyalty.entity.PointsTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {
    Page<PointsTransaction> findByTenantIdAndAccountId(String tenantId, Long accountId, Pageable pageable);
}
repository/RewardRepository.java

java
package com.supermarket.loyalty.repository;

import com.supermarket.loyalty.entity.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RewardRepository extends JpaRepository<Reward, Long> {
    List<Reward> findByTenantIdAndActiveTrue(String tenantId);
}
repository/CouponRepository.java

java
package com.supermarket.loyalty.repository;

import com.supermarket.loyalty.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByTenantIdAndCode(String tenantId, String code);
    List<Coupon> findByTenantIdAndCustomerIdAndValidFromBeforeAndValidUntilAfterAndActiveTrue(
            String tenantId, String customerId, LocalDateTime now1, LocalDateTime now2);
}
29.7 Kafka Consumer
consumer/OrderEventConsumer.java

java
package com.supermarket.loyalty.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
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
            String customerId = (String) event.get("customerId");
            Number total = (Number) event.get("total");
            String orderId = (String) event.get("orderId");

            if ("COMPLETED".equals(eventType) || "DELIVERED".equals(eventType)) {
                if (customerId != null) {
                    loyaltyService.earnPoints(tenantId, customerId, total.doubleValue(), "ORDER", orderId);
                }
            } else if ("CANCELLED".equals(eventType) && customerId != null) {
                // Reverse points if needed
                loyaltyService.reversePoints(tenantId, customerId, total.doubleValue(), "ORDER_CANCELLED", orderId);
            }
        } catch (Exception e) {
            log.error("Error processing order event", e);
        }
    }
}
29.8 Service Layer
service/PointsCalculator.java

java
package com.supermarket.loyalty.service;

import com.supermarket.loyalty.entity.Tier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PointsCalculator {

    public int calculatePoints(BigDecimal amount, Tier tier) {
        // Base points: 1 point per dollar
        double basePoints = amount.doubleValue();
        if (tier != null && tier.getPointsMultiplier() != null) {
            basePoints *= tier.getPointsMultiplier().doubleValue();
        }
        return (int) Math.floor(basePoints);
    }
}
service/LoyaltyService.java

java
package com.supermarket.loyalty.service;

import com.supermarket.loyalty.entity.LoyaltyAccount;
import com.supermarket.loyalty.entity.PointsTransaction;
import com.supermarket.loyalty.entity.Tier;
import com.supermarket.loyalty.repository.LoyaltyAccountRepository;
import com.supermarket.loyalty.repository.PointsTransactionRepository;
import com.supermarket.loyalty.repository.TierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyAccountRepository accountRepository;
    private final PointsTransactionRepository transactionRepository;
    private final TierRepository tierRepository;
    private final PointsCalculator pointsCalculator;

    @Transactional
    public LoyaltyAccount getOrCreateAccount(String tenantId, String customerId) {
        return accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseGet(() -> {
                    LoyaltyAccount account = new LoyaltyAccount();
                    account.setTenantId(tenantId);
                    account.setCustomerId(customerId);
                    account.setPointsBalance(0);
                    account.setLifetimePoints(0);
                    account.setCreatedAt(LocalDateTime.now());
                    account.setUpdatedAt(LocalDateTime.now());

                    // Assign default tier (Bronze)
                    Tier defaultTier = tierRepository.findByTenantIdAndName(tenantId, "BRONZE")
                            .orElseGet(() -> createDefaultTiers(tenantId).stream()
                                    .filter(t -> "BRONZE".equals(t.getName()))
                                    .findFirst().orElse(null));
                    account.setTier(defaultTier);

                    return accountRepository.save(account);
                });
    }

    @Transactional
    public void earnPoints(String tenantId, String customerId, double amount, String referenceType, String referenceId) {
        LoyaltyAccount account = getOrCreateAccount(tenantId, customerId);
        Tier tier = account.getTier();

        int points = pointsCalculator.calculatePoints(BigDecimal.valueOf(amount), tier);

        PointsTransaction transaction = new PointsTransaction();
        transaction.setTenantId(tenantId);
        transaction.setAccountId(account.getId());
        transaction.setPoints(points);
        transaction.setTransactionType("EARN");
        transaction.setReferenceId(referenceId);
        transaction.setDescription(referenceType + " purchase");
        transaction.setTransactionDate(LocalDateTime.now());
        // Set expiry if needed (e.g., 1 year)
        transaction.setExpiryDate(LocalDateTime.now().plusYears(1));
        transactionRepository.save(transaction);

        account.setPointsBalance(account.getPointsBalance() + points);
        account.setLifetimePoints(account.getLifetimePoints() + points);
        account.setLastTransactionDate(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        // Check for tier upgrade
        checkAndUpdateTier(account);

        accountRepository.save(account);
    }

    @Transactional
    public void reversePoints(String tenantId, String customerId, double amount, String reason, String referenceId) {
        LoyaltyAccount account = getOrCreateAccount(tenantId, customerId);
        Tier tier = account.getTier();
        int points = pointsCalculator.calculatePoints(BigDecimal.valueOf(amount), tier);

        PointsTransaction transaction = new PointsTransaction();
        transaction.setTenantId(tenantId);
        transaction.setAccountId(account.getId());
        transaction.setPoints(-points);
        transaction.setTransactionType("ADJUST");
        transaction.setReferenceId(referenceId);
        transaction.setDescription(reason);
        transaction.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(transaction);

        account.setPointsBalance(Math.max(0, account.getPointsBalance() - points));
        account.setLastTransactionDate(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        accountRepository.save(account);
    }

    @Transactional
    public boolean redeemPoints(String tenantId, String customerId, int pointsToRedeem, String rewardId, String description) {
        LoyaltyAccount account = getOrCreateAccount(tenantId, customerId);
        if (account.getPointsBalance() < pointsToRedeem) {
            return false;
        }

        PointsTransaction transaction = new PointsTransaction();
        transaction.setTenantId(tenantId);
        transaction.setAccountId(account.getId());
        transaction.setPoints(-pointsToRedeem);
        transaction.setTransactionType("REDEEM");
        transaction.setReferenceId(rewardId);
        transaction.setDescription(description);
        transaction.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(transaction);

        account.setPointsBalance(account.getPointsBalance() - pointsToRedeem);
        account.setLastTransactionDate(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        return true;
    }

    public LoyaltyAccount getAccount(String tenantId, String customerId) {
        return getOrCreateAccount(tenantId, customerId);
    }

    private void checkAndUpdateTier(LoyaltyAccount account) {
        List<Tier> tiers = tierRepository.findByTenantIdAndActiveTrue(account.getTenantId());
        // Sort by minPoints descending to find highest eligible
        tiers.sort((a, b) -> b.getMinPoints().compareTo(a.getMinPoints()));

        for (Tier tier : tiers) {
            if (account.getLifetimePoints() >= tier.getMinPoints()) {
                if (!tier.equals(account.getTier())) {
                    account.setTier(tier);
                }
                break;
            }
        }
    }

    private List<Tier> createDefaultTiers(String tenantId) {
        Tier bronze = new Tier();
        bronze.setTenantId(tenantId);
        bronze.setName("BRONZE");
        bronze.setMinPoints(0);
        bronze.setPointsMultiplier(BigDecimal.valueOf(1.0));
        bronze.setDescription("Bronze tier");
        tierRepository.save(bronze);

        Tier silver = new Tier();
        silver.setTenantId(tenantId);
        silver.setName("SILVER");
        silver.setMinPoints(1000);
        silver.setPointsMultiplier(BigDecimal.valueOf(1.5));
        silver.setDescription("Silver tier");
        tierRepository.save(silver);

        Tier gold = new Tier();
        gold.setTenantId(tenantId);
        gold.setName("GOLD");
        gold.setMinPoints(5000);
        gold.setPointsMultiplier(BigDecimal.valueOf(2.0));
        gold.setDescription("Gold tier");
        tierRepository.save(gold);

        return List.of(bronze, silver, gold);
    }
}
service/CouponService.java

java
package com.supermarket.loyalty.service;

import com.supermarket.loyalty.entity.Coupon;
import com.supermarket.loyalty.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public Coupon generateCoupon(String tenantId, String customerId, String description,
                                 String discountType, Double discountValue,
                                 LocalDateTime validUntil) {
        Coupon coupon = new Coupon();
        coupon.setTenantId(tenantId);
        coupon.setCode(generateCouponCode());
        coupon.setDescription(description);
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(discountValue);
        coupon.setApplicableTo("ALL");
        coupon.setValidFrom(LocalDateTime.now());
        coupon.setValidUntil(validUntil);
        coupon.setUsageLimit(1);
        coupon.setUsageCount(0);
        coupon.setCustomerId(customerId);
        coupon.setActive(true);
        return couponRepository.save(coupon);
    }

    public boolean validateCoupon(String tenantId, String code, String customerId, Double orderAmount) {
        Coupon coupon = couponRepository.findByTenantIdAndCode(tenantId, code)
                .orElse(null);
        if (coupon == null || !coupon.getActive()) {
            return false;
        }
        if (coupon.getCustomerId() != null && !coupon.getCustomerId().equals(customerId)) {
            return false;
        }
        if (coupon.getValidFrom().isAfter(LocalDateTime.now()) ||
                coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            return false;
        }
        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            return false;
        }
        if (coupon.getMinOrderAmount() != null && orderAmount < coupon.getMinOrderAmount()) {
            return false;
        }
        return true;
    }

    @Transactional
    public Coupon useCoupon(String tenantId, String code) {
        Coupon coupon = couponRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        coupon.setUsageCount(coupon.getUsageCount() + 1);
        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            coupon.setActive(false);
        }
        return couponRepository.save(coupon);
    }

    public List<Coupon> getCustomerCoupons(String tenantId, String customerId) {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findByTenantIdAndCustomerIdAndValidFromBeforeAndValidUntilAfterAndActiveTrue(
                tenantId, customerId, now, now);
    }

    private String generateCouponCode() {
        return "SAVE" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
29.9 Controllers
controller/LoyaltyController.java

java
package com.supermarket.loyalty.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.loyalty.entity.LoyaltyAccount;
import com.supermarket.loyalty.entity.PointsTransaction;
import com.supermarket.loyalty.entity.Reward;
import com.supermarket.loyalty.repository.PointsTransactionRepository;
import com.supermarket.loyalty.repository.RewardRepository;
import com.supermarket.loyalty.service.LoyaltyService;
import com.supermarket.loyalty.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final RewardRepository rewardRepository;
    private final PointsTransactionRepository transactionRepository;

    @GetMapping("/account")
    public ResponseEntity<ApiResponse<LoyaltyAccount>> getAccount(@RequestParam String customerId) {
        String tenantId = TenantContext.getCurrentTenant();
        LoyaltyAccount account = loyaltyService.getAccount(tenantId, customerId);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @GetMapping("/account/{accountId}/transactions")
    public ResponseEntity<ApiResponse<Page<PointsTransaction>>> getTransactions(
            @PathVariable Long accountId, Pageable pageable) {
        String tenantId = TenantContext.getCurrentTenant();
        Page<PointsTransaction> transactions = transactionRepository.findByTenantIdAndAccountId(tenantId, accountId, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse<String>> redeemPoints(
            @RequestParam String customerId,
            @RequestParam int points,
            @RequestParam Long rewardId) {
        String tenantId = TenantContext.getCurrentTenant();
        boolean success = loyaltyService.redeemPoints(tenantId, customerId, points, rewardId.toString(), "Redeemed reward");
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Points redeemed successfully"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Insufficient points"));
        }
    }

    @GetMapping("/rewards")
    public ResponseEntity<ApiResponse<List<Reward>>> getRewards() {
        String tenantId = TenantContext.getCurrentTenant();
        List<Reward> rewards = rewardRepository.findByTenantIdAndActiveTrue(tenantId);
        return ResponseEntity.ok(ApiResponse.success(rewards));
    }
}
controller/CouponController.java

java
package com.supermarket.loyalty.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.loyalty.entity.Coupon;
import com.supermarket.loyalty.service.CouponService;
import com.supermarket.loyalty.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Coupon>>> getMyCoupons(@RequestParam String customerId) {
        String tenantId = TenantContext.getCurrentTenant();
        List<Coupon> coupons = couponService.getCustomerCoupons(tenantId, customerId);
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateCoupon(
            @RequestParam String code,
            @RequestParam String customerId,
            @RequestParam Double orderAmount) {
        String tenantId = TenantContext.getCurrentTenant();
        boolean valid = couponService.validateCoupon(tenantId, code, customerId, orderAmount);
        return ResponseEntity.ok(ApiResponse.success(valid));
    }

    @PostMapping("/{code}/use")
    public ResponseEntity<ApiResponse<Coupon>> useCoupon(@PathVariable String code) {
        String tenantId = TenantContext.getCurrentTenant();
        Coupon coupon = couponService.useCoupon(tenantId, code);
        return ResponseEntity.ok(ApiResponse.success(coupon));
    }
}
29.10 Configuration
bootstrap.yml

yaml
spring:
  application:
    name: loyalty-service
  cloud:
    config:
      uri: http://config-service:8888
      fail-fast: true
Config repo (loyalty-service.yml)

yaml
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
Dockerfile

dockerfile
FROM openjdk:17-jdk-slim
COPY target/loyalty-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
29.11 Update Docker Compose
Add loyalty-service to docker-compose.yml:

yaml
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
29.12 Update API Gateway Routes
Add routes for loyalty service in GatewayConfig.java:

java
.route("loyalty", r -> r
    .path("/api/loyalty/**")
    .filters(f -> f.rewritePath("/api/loyalty/(?<segment>.*)", "/loyalty/${segment}")
            .filter(new AuthenticationFilter().apply(new AuthenticationFilter.Config())))
    .uri("lb://LOYALTY-SERVICE"))
.route("coupons", r -> r
    .path("/api/coupons/**")
    .filters(f -> f.rewritePath("/api/coupons/(?<segment>.*)", "/coupons/${segment}")
            .filter(new AuthenticationFilter().apply(new AuthenticationFilter.Config())))
    .uri("lb://LOYALTY-SERVICE"))
29.13 Testing
Rebuild all:

bash
mvn clean package
docker-compose up --build
Get JWT token.

Create an order (with customerId) to trigger points earning via Kafka.

Check loyalty account:

text
GET http://localhost:8080/api/loyalty/account?customerId=cust123
Authorization: Bearer <token>
Get available rewards:

text
GET http://localhost:8080/api/loyalty/rewards
Redeem points:

text
POST http://localhost:8080/api/loyalty/redeem?customerId=cust123&points=50&rewardId=1
Get customer coupons:

text
GET http://localhost:8080/api/coupons?customerId=cust123
30. Next Steps
We have now implemented:

Phase 12: Loyalty Service (complete)

Remaining services from the original plan:

Phase 13-14: Analytics & Reporting (could be combined into one service)

Phase 7: Customer Service (if needed as separate service, but customer data is currently handled in auth and order services)

Given the scope, Analytics & Reporting is the last major service. It will consume events from Kafka (orders, payments, inventory) and provide aggregated data and reports.

Shall I proceed with Analytics & Reporting Service?