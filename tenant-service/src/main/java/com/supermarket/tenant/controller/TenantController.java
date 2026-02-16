package com.supermarket.tenant.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.tenant.entity.Tenant;
import com.supermarket.tenant.service.TenantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Tenant>> createTenant(@RequestBody Tenant tenant) {
        Tenant created = tenantService.createTenant(tenant);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Tenant created", created, null));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<Tenant>> getTenant(@PathVariable String tenantId) {
        Tenant tenant = tenantService.getTenantById(tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tenant retrieved", tenant, null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Tenant>>> getAllTenants() {
        List<Tenant> tenants = tenantService.getAllTenants();
        return ResponseEntity.ok(new ApiResponse<>(true, "Tenants retrieved", tenants, null));
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<Tenant>> updateTenant(
            @PathVariable String tenantId, 
            @RequestBody Tenant tenant) {
        Tenant updated = tenantService.updateTenant(tenantId, tenant);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tenant updated", updated, null));
    }
}
