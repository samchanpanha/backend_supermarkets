package com.supermarket.product.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.product.entity.Category;
import com.supermarket.product.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> createCategory(
            @RequestBody Category category,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        Category created = categoryService.createCategory(category, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Category created successfully", created, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable Long id,
            @RequestBody Category category,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        Category updated = categoryService.updateCategory(id, category, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Category updated successfully", updated, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getCategory(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        Category category = categoryService.getCategoryById(id, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Category retrieved successfully", category, null));
    }

    @GetMapping("/root")
    public ResponseEntity<ApiResponse<List<Category>>> getRootCategories(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<Category> categories = categoryService.getRootCategories(tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Root categories retrieved", categories, null));
    }

    @GetMapping("/children/{parentId}")
    public ResponseEntity<ApiResponse<List<Category>>> getChildCategories(
            @PathVariable Long parentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<Category> categories = categoryService.getChildCategories(tenantId, parentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Child categories retrieved", categories, null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<Category> categories = categoryService.getAllCategories(tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Categories retrieved successfully", categories, null));
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<ApiResponse<List<Category>>> getCategoriesByLevel(
            @PathVariable Integer level,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<Category> categories = categoryService.getCategoriesByLevel(tenantId, level);
        return ResponseEntity.ok(new ApiResponse<>(true, "Categories retrieved", categories, null));
    }
}
