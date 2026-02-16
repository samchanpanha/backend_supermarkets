package com.supermarket.accounting.repository;

import com.supermarket.accounting.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByAccountNumberAndTenantId(String accountNumber, String tenantId);
    
    List<Account> findByTenantId(String tenantId);
    
    List<Account> findByTenantIdAndIsActiveTrue(String tenantId);
    
    List<Account> findByTenantIdAndAccountCategory(String tenantId, String category);
}
