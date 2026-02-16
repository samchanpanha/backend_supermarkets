package com.supermarket.inventory.service;

import com.supermarket.inventory.dto.InventoryRequest;
import com.supermarket.inventory.dto.InventoryResponse;
import com.supermarket.inventory.entity.Inventory;
import com.supermarket.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryResponse createInventory(InventoryRequest request, String tenantId) {
        if (inventoryRepository.findByProductIdAndTenantId(request.getProductId(), tenantId).isPresent()) {
            throw new RuntimeException("Inventory already exists for this product");
        }

        Inventory inventory = new Inventory();
        inventory.setTenantId(tenantId);
        inventory.setProductId(request.getProductId());
        inventory.setQuantity(request.getQuantity());
        inventory.setReservedQuantity(0);
        inventory.setReorderLevel(request.getReorderLevel());
        inventory.setReorderQuantity(request.getReorderQuantity());
        inventory.setLocation(request.getLocation());
        inventory.setBatchNumber(request.getBatchNumber());
        inventory.setExpiryDate(request.getExpiryDate());
        inventory.setLastStockIn(LocalDateTime.now());

        Inventory saved = inventoryRepository.save(inventory);
        return mapToResponse(saved);
    }

    public InventoryResponse updateInventory(Long id, InventoryRequest request, String tenantId) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        if (!inventory.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to inventory");
        }

        inventory.setQuantity(request.getQuantity());
        inventory.setReorderLevel(request.getReorderLevel());
        inventory.setReorderQuantity(request.getReorderQuantity());
        inventory.setLocation(request.getLocation());
        inventory.setBatchNumber(request.getBatchNumber());
        inventory.setExpiryDate(request.getExpiryDate());

        Inventory saved = inventoryRepository.save(inventory);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId, String tenantId) {
        Inventory inventory = inventoryRepository.findByProductIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
        return mapToResponse(inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventory(String tenantId) {
        return inventoryRepository.findByTenantId(tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryByProductIds(List<Long> productIds, String tenantId) {
        return inventoryRepository.findByProductIdInAndTenantId(productIds, tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockInventory(String tenantId) {
        return inventoryRepository.findByQuantityLessThanEqualAndTenantId(10, tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public boolean checkStockAvailability(Long productId, Integer quantity, String tenantId) {
        Inventory inventory = inventoryRepository.findByProductIdAndTenantId(productId, tenantId)
                .orElse(null);
        
        if (inventory == null) {
            return false;
        }
        
        return inventory.getAvailableQuantity() >= quantity;
    }

    public void reserveStock(Long productId, Integer quantity, String tenantId) {
        Inventory inventory = inventoryRepository.findByProductIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock available");
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);
    }

    public void releaseStock(Long productId, Integer quantity, String tenantId) {
        Inventory inventory = inventoryRepository.findByProductIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - quantity));
        inventoryRepository.save(inventory);
    }

    public void reduceStock(Long productId, Integer quantity, String tenantId) {
        Inventory inventory = inventoryRepository.findByProductIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        if (inventory.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - quantity));
        inventory.setLastStockOut(LocalDateTime.now());
        inventoryRepository.save(inventory);
    }

    public void addStock(Long productId, Integer quantity, String tenantId) {
        Inventory inventory = inventoryRepository.findByProductIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventory.setLastStockIn(LocalDateTime.now());
        inventoryRepository.save(inventory);
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        InventoryResponse response = new InventoryResponse();
        response.setId(inventory.getId());
        response.setTenantId(inventory.getTenantId());
        response.setProductId(inventory.getProductId());
        response.setQuantity(inventory.getQuantity());
        response.setReservedQuantity(inventory.getReservedQuantity());
        response.setAvailableQuantity(inventory.getAvailableQuantity());
        response.setReorderLevel(inventory.getReorderLevel());
        response.setReorderQuantity(inventory.getReorderQuantity());
        response.setLocation(inventory.getLocation());
        response.setBatchNumber(inventory.getBatchNumber());
        response.setExpiryDate(inventory.getExpiryDate());
        response.setLastStockIn(inventory.getLastStockIn());
        response.setLastStockOut(inventory.getLastStockOut());
        response.setCreatedAt(inventory.getCreatedAt());
        response.setUpdatedAt(inventory.getUpdatedAt());
        return response;
    }
}
