package com.supermarket.product.service;

import com.supermarket.product.entity.ProductUnit;
import com.supermarket.product.repository.ProductUnitRepository;
import com.supermarket.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductUnitService {

    private final ProductUnitRepository productUnitRepository;
    private final ProductRepository productRepository;

    public ProductUnitService(ProductUnitRepository productUnitRepository, ProductRepository productRepository) {
        this.productUnitRepository = productUnitRepository;
        this.productRepository = productRepository;
    }

    public ProductUnit createProductUnit(ProductUnit productUnit, String tenantId) {
        productUnit.setTenantId(tenantId);
        
        if (productUnit.isBaseUnit()) {
            productUnitRepository.findIsBaseUnitTrueByProductIdAnd(productUnit.getProduct().getId())
                    .ifPresent(existing -> {
                        existing.setBaseUnit(false);
                        productUnitRepository.save(existing);
                    });
        }

        return productUnitRepository.save(productUnit);
    }

    public List<ProductUnit> getProductUnits(Long productId, String tenantId) {
        return productUnitRepository.findByProductIdAndTenantId(productId, tenantId);
    }

    public BigDecimal convertQuantity(Long productId, String fromUnit, String toUnit, BigDecimal quantity) {
        ProductUnit from = productUnitRepository.findByProductIdAndUnitCode(productId, fromUnit)
                .orElseThrow(() -> new RuntimeException("From unit not found"));
        
        ProductUnit to = productUnitRepository.findByProductIdAndUnitCode(productId, toUnit)
                .orElseThrow(() -> new RuntimeException("To unit not found"));
        
        BigDecimal baseQuantity = quantity.multiply(from.getConversionRate());
        return baseQuantity.divide(to.getConversionRate(), 2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal calculatePrice(Long productId, String unitCode, BigDecimal quantity) {
        ProductUnit unit = productUnitRepository.findByProductIdAndUnitCode(productId, unitCode)
                .orElseThrow(() -> new RuntimeException("Unit not found"));
        
        return unit.getSellingPrice().multiply(quantity);
    }
}
