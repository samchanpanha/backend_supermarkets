package com.supermarket.product.repository;

import com.supermarket.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findBySkuAndTenantId(String sku, String tenantId);
    
    List<Product> findByTenantId(String tenantId);
    
    Page<Product> findByTenantId(String tenantId, Pageable pageable);
    
    Page<Product> findByTenantIdAndCategory(String tenantId, String category, Pageable pageable);
    
    Page<Product> findByTenantIdAndActive(String tenantId, boolean active, Pageable pageable);
    
    boolean existsBySkuAndTenantId(String sku, String tenantId);
    
    List<Product> findByIdInAndTenantId(List<Long> ids, String tenantId);
}
