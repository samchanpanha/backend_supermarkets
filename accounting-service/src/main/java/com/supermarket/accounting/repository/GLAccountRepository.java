package com.supermarket.accounting.repository;

import com.supermarket.accounting.entity.GLAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GLAccountRepository extends JpaRepository<GLAccount, Long> {
    
    Optional<GLAccount> findByAccountCodeAndTenantId(String accountCode, String tenantId);
    
    List<GLAccount> findByTenantId(String tenantId);
    
    List<GLAccount> findByTenantIdAndAccountType(String tenantId, String accountType);
    
    List<GLAccount> findByTenantIdAndIsActiveTrue(String tenantId);
    
    boolean existsByAccountCodeAndTenantId(String accountCode, String tenantId);
}
