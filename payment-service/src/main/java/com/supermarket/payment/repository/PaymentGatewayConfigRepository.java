package com.supermarket.payment.repository;

import com.supermarket.payment.entity.PaymentGatewayConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentGatewayConfigRepository extends JpaRepository<PaymentGatewayConfig, Long> {
    
    Optional<PaymentGatewayConfig> findByGatewayCodeAndTenantId(String gatewayCode, String tenantId);
    
    List<PaymentGatewayConfig> findByTenantId(String tenantId);
    
    List<PaymentGatewayConfig> findByTenantIdAndEnabledTrue(String tenantId);
    
    boolean existsByGatewayCodeAndTenantId(String gatewayCode, String tenantId);
}
