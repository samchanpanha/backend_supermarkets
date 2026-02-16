package com.supermarket.product.repository;

import com.supermarket.product.entity.ProductUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductUnitRepository extends JpaRepository<ProductUnit, Long> {
    
    List<ProductUnit> findByProductIdAndTenantId(Long productId, String tenantId);
    
    Optional<ProductUnit> findByProductIdAndIsBaseUnitTrue(Long productId);
    
    Optional<ProductUnit> findByProductIdAndUnitCode(Long productId, String unitCode);
}
