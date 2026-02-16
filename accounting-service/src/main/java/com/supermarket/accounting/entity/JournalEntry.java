package com.supermarket.accounting.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false, unique = true)
    private String entryNumber;

    @Column(nullable = false)
    private LocalDateTime entryDate;

    @Column(nullable = false)
    private String voucherType;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String referenceNumber;

    @Column(nullable = false)
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Long postedBy;

    private LocalDateTime postedAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<JournalEntryLine> lines;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        entryDate = LocalDateTime.now();
        if (status == null) {
            status = "DRAFT";
        }
    }
}
