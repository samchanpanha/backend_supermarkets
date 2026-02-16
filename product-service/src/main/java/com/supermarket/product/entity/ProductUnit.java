package com.supermarket.product.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "product_units")
public class ProductUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String unitName;

    @Column(nullable = false)
    private String unitCode;

    @Column(nullable = false)
    private BigDecimal conversionRate;

    @Column(nullable = false)
    private BigDecimal sellingPrice;

    private BigDecimal costPrice;

    private BigDecimal minStockLevel;

    private BigDecimal maxStockLevel;

    private BigDecimal reorderLevel;

    @Column(nullable = false)
    private boolean isBaseUnit = false;

    @Column(nullable = false)
    private boolean isActive = true;

    private Integer barcode;

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
