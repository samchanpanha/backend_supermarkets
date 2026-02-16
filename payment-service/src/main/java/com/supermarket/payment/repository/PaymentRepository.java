package com.supermarket.payment.repository;

import com.supermarket.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByTransactionIdAndTenantId(String transactionId, String tenantId);
    
    Optional<Payment> findByOrderIdAndTenantId(Long orderId, String tenantId);
}
