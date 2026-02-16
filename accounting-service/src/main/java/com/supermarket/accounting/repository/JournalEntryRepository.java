package com.supermarket.accounting.repository;

import com.supermarket.accounting.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    
    Optional<JournalEntry> findByEntryNumberAndTenantId(String entryNumber, String tenantId);
    
    Page<JournalEntry> findByTenantId(String tenantId, Pageable pageable);
    
    Page<JournalEntry> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
}
