package com.supermarket.product.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.product.dto.ProductRequest;
import com.supermarket.product.dto.ProductResponse;
import com.supermarket.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        ProductResponse response = productService.createProduct(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Product created successfully", response, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        ProductResponse response = productService.updateProduct(id, request, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product updated successfully", response, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        ProductResponse response = productService.getProductById(id, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product retrieved successfully", response, null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ProductResponse> products = productService.getAllProducts(tenantId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Products retrieved successfully", products, null));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductsByCategory(
            @PathVariable String category,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ProductResponse> products = productService.getProductsByCategory(tenantId, category, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Products retrieved successfully", products, null));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByIds(
            @RequestBody List<Long> ids,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<ProductResponse> products = productService.getProductsByIds(ids, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Products retrieved successfully", products, null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        productService.deleteProduct(id, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product deleted successfully", "Product deleted", null));
    }
}
