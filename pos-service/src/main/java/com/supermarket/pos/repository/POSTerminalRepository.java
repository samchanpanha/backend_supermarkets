package com.supermarket.pos.repository;

import com.supermarket.pos.entity.POSTerminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface POSTerminalRepository extends JpaRepository<POSTerminal, Long> {
    
    Optional<POSTerminal> findByTerminalCodeAndTenantId(String terminalCode, String tenantId);
}
