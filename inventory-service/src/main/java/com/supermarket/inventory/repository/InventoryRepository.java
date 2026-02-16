package com.supermarket.inventory.repository;

import com.supermarket.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    Optional<Inventory> findByProductIdAndTenantId(Long productId, String tenantId);
    
    List<Inventory> findByTenantId(String tenantId);
    
    List<Inventory> findByProductIdInAndTenantId(List<Long> productIds, String tenantId);
    
    List<Inventory> findByQuantityLessThanEqualAndTenantId(Integer reorderLevel, String tenantId);
}
