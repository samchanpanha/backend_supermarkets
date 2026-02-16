package com.supermarket.accounting.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "gl_accounts")
public class GLAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false, unique = true)
    private String accountCode;

    @Column(nullable = false)
    private String accountName;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String accountType;

    @Column(nullable = false)
    private String balanceType;

    @Column(nullable = false)
    private String parentAccountCode;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private boolean isCashFlow = false;

    @Column(nullable = false)
    private int level = 1;

    private BigDecimal openingBalance = BigDecimal.ZERO;

    private BigDecimal currentBalance = BigDecimal.ZERO;

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
