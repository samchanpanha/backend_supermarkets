package com.supermarket.order.repository;

import com.supermarket.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumberAndTenantId(String orderNumber, String tenantId);
    
    Page<Order> findByTenantId(String tenantId, Pageable pageable);
    
    Page<Order> findByTenantIdAndCustomerId(String tenantId, Long customerId, Pageable pageable);
    
    Page<Order> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
}
