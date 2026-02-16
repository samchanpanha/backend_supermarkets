package com.supermarket.product.repository;

import com.supermarket.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByCodeAndTenantId(String code, String tenantId);
    
    List<Category> findByTenantId(String tenantId);
    
    List<Category> findByTenantIdAndParentIsNull(String tenantId);
    
    List<Category> findByTenantIdAndParentId(String tenantId, Long parentId);
    
    List<Category> findByTenantIdAndLevel(String tenantId, Integer level);
    
    boolean existsByCodeAndTenantId(String code, String tenantId);
}
