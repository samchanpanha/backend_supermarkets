package com.supermarket.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payment_gateway_configs")
public class PaymentGatewayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String gatewayName;

    @Column(nullable = false, unique = true)
    private String gatewayCode;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(length = 1000)
    private String merchantId;

    @Column(length = 1000)
    private String merchantKey;

    @Column(length = 1000)
    private String apiUrl;

    @Column(length = 1000)
    private String callbackUrl;

    @Column(nullable = false)
    private String currency = "USD";

    @Column(nullable = false)
    private double transactionFee = 0.0;

    @Column(length = 1000)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
