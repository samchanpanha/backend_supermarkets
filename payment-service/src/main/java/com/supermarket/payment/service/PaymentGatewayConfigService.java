package com.supermarket.payment.service;

import com.supermarket.payment.entity.PaymentGatewayConfig;
import com.supermarket.payment.repository.PaymentGatewayConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PaymentGatewayConfigService {

    private final PaymentGatewayConfigRepository paymentGatewayConfigRepository;

    public PaymentGatewayConfigService(PaymentGatewayConfigRepository paymentGatewayConfigRepository) {
        this.paymentGatewayConfigRepository = paymentGatewayConfigRepository;
    }

    public PaymentGatewayConfig createGateway(PaymentGatewayConfig config, String tenantId) {
        if (paymentGatewayConfigRepository.existsByGatewayCodeAndTenantId(config.getGatewayCode(), tenantId)) {
            throw new RuntimeException("Gateway with code " + config.getGatewayCode() + " already exists");
        }

        config.setTenantId(tenantId);
        return paymentGatewayConfigRepository.save(config);
    }

    public PaymentGatewayConfig updateGateway(Long id, PaymentGatewayConfig config, String tenantId) {
        PaymentGatewayConfig existing = paymentGatewayConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment gateway not found"));

        if (!existing.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to gateway config");
        }

        existing.setGatewayName(config.getGatewayName());
        existing.setMerchantId(config.getMerchantId());
        existing.setMerchantKey(config.getMerchantKey());
        existing.setApiUrl(config.getApiUrl());
        existing.setCallbackUrl(config.getCallbackUrl());
        existing.setCurrency(config.getCurrency());
        existing.setTransactionFee(config.getTransactionFee());
        existing.setDescription(config.getDescription());
        existing.setEnabled(config.isEnabled());

        return paymentGatewayConfigRepository.save(existing);
    }

    public PaymentGatewayConfig enableGateway(String gatewayCode, String tenantId) {
        PaymentGatewayConfig config = paymentGatewayConfigRepository.findByGatewayCodeAndTenantId(gatewayCode, tenantId)
                .orElseThrow(() -> new RuntimeException("Payment gateway not found"));
        
        config.setEnabled(true);
        return paymentGatewayConfigRepository.save(config);
    }

    public PaymentGatewayConfig disableGateway(String gatewayCode, String tenantId) {
        PaymentGatewayConfig config = paymentGatewayConfigRepository.findByGatewayCodeAndTenantId(gatewayCode, tenantId)
                .orElseThrow(() -> new RuntimeException("Payment gateway not found"));
        
        config.setEnabled(false);
        return paymentGatewayConfigRepository.save(config);
    }

    @Transactional(readOnly = true)
    public PaymentGatewayConfig getGatewayByCode(String gatewayCode, String tenantId) {
        return paymentGatewayConfigRepository.findByGatewayCodeAndTenantId(gatewayCode, tenantId)
                .orElseThrow(() -> new RuntimeException("Payment gateway not found"));
    }

    @Transactional(readOnly = true)
    public List<PaymentGatewayConfig> getAllGateways(String tenantId) {
        return paymentGatewayConfigRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<PaymentGatewayConfig> getEnabledGateways(String tenantId) {
        return paymentGatewayConfigRepository.findByTenantIdAndEnabledTrue(tenantId);
    }

    public boolean isGatewayEnabled(String gatewayCode, String tenantId) {
        return paymentGatewayConfigRepository.findByGatewayCodeAndTenantId(gatewayCode, tenantId)
                .map(PaymentGatewayConfig::isEnabled)
                .orElse(false);
    }
}
