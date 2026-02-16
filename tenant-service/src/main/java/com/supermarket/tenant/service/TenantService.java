package com.supermarket.tenant.service;

import com.supermarket.tenant.entity.Tenant;
import com.supermarket.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant createTenant(Tenant tenant) {
        tenant.setTenantId(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tenant.setStatus("ACTIVE");
        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant getTenantById(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }

    @Transactional(readOnly = true)
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    public Tenant updateTenant(String tenantId, Tenant tenant) {
        Tenant existing = getTenantById(tenantId);
        existing.setName(tenant.getName());
        existing.setDescription(tenant.getDescription());
        existing.setContactEmail(tenant.getContactEmail());
        existing.setContactPhone(tenant.getContactPhone());
        existing.setAddress(tenant.getAddress());
        return tenantRepository.save(existing);
    }
}
