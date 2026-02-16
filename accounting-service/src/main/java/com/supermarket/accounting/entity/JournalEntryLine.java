package com.supermarket.accounting.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "journal_entry_lines")
public class JournalEntryLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Column(nullable = false)
    private String accountCode;

    private String accountName;

    @Column(nullable = false)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(length = 1000)
    private String description;

    private String costCenter;

    private String projectCode;

    private Long relatedReferenceId;
}
