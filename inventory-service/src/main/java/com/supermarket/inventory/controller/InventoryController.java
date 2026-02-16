package com.supermarket.inventory.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.inventory.dto.InventoryRequest;
import com.supermarket.inventory.dto.InventoryResponse;
import com.supermarket.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> createInventory(
            @Valid @RequestBody InventoryRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        InventoryResponse response = inventoryService.createInventory(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Inventory created successfully", response, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody InventoryRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        InventoryResponse response = inventoryService.updateInventory(id, request, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Inventory updated successfully", response, null));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByProduct(
            @PathVariable Long productId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        InventoryResponse response = inventoryService.getInventoryByProductId(productId, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Inventory retrieved successfully", response, null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllInventory(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<InventoryResponse> inventory = inventoryService.getAllInventory(tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Inventory retrieved successfully", inventory, null));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStockInventory(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<InventoryResponse> inventory = inventoryService.getLowStockInventory(tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Low stock inventory retrieved", inventory, null));
    }

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkStock(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());
        
        boolean available = inventoryService.checkStockAvailability(productId, quantity, tenantId);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock check completed", Map.of("available", available), null));
    }

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<String>> reserveStock(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());
        
        inventoryService.reserveStock(productId, quantity, tenantId);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock reserved successfully", "Reserved", null));
    }

    @PostMapping("/release")
    public ResponseEntity<ApiResponse<String>> releaseStock(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());
        
        inventoryService.releaseStock(productId, quantity, tenantId);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock released successfully", "Released", null));
    }

    @PostMapping("/reduce")
    public ResponseEntity<ApiResponse<String>> reduceStock(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());
        
        inventoryService.reduceStock(productId, quantity, tenantId);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock reduced successfully", "Reduced", null));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<String>> addStock(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());
        
        inventoryService.addStock(productId, quantity, tenantId);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock added successfully", "Added", null));
    }
}
