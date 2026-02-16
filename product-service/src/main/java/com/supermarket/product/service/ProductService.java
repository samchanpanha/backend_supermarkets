package com.supermarket.product.service;

import com.supermarket.product.dto.ProductRequest;
import com.supermarket.product.dto.ProductResponse;
import com.supermarket.product.entity.Product;
import com.supermarket.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(ProductRequest request, String tenantId) {
        if (productRepository.existsBySkuAndTenantId(request.getSku(), tenantId)) {
            throw new RuntimeException("Product with SKU " + request.getSku() + " already exists");
        }

        Product product = new Product();
        product.setTenantId(tenantId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setBrand(request.getBrand());
        product.setUnit(request.getUnit());
        product.setWeight(request.getWeight());
        product.setActive(request.isActive());

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request, String tenantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to product");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setBrand(request.getBrand());
        product.setUnit(request.getUnit());
        product.setWeight(request.getWeight());
        product.setActive(request.isActive());

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id, String tenantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to product");
        }

        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(String tenantId, Pageable pageable) {
        return productRepository.findByTenantId(tenantId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(String tenantId, String category, Pageable pageable) {
        return productRepository.findByTenantIdAndCategory(tenantId, category, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByIds(List<Long> ids, String tenantId) {
        return productRepository.findByIdInAndTenantId(ids, tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void deleteProduct(Long id, String tenantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to product");
        }

        productRepository.delete(product);
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setTenantId(product.getTenantId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setSku(product.getSku());
        response.setPrice(product.getPrice());
        response.setCategory(product.getCategory());
        response.setImageUrl(product.getImageUrl());
        response.setActive(product.isActive());
        response.setBrand(product.getBrand());
        response.setUnit(product.getUnit());
        response.setWeight(product.getWeight());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        return response;
    }
}
