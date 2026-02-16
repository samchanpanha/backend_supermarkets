package com.supermarket.payment.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.payment.entity.PaymentGatewayConfig;
import com.supermarket.payment.service.PaymentGatewayConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/gateways")
public class PaymentGatewayConfigController {

    private final PaymentGatewayConfigService paymentGatewayConfigService;

    public PaymentGatewayConfigController(PaymentGatewayConfigService paymentGatewayConfigService) {
        this.paymentGatewayConfigService = paymentGatewayConfigService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentGatewayConfig>> createGateway(
            @RequestBody PaymentGatewayConfig config,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        PaymentGatewayConfig created = paymentGatewayConfigService.createGateway(config, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Payment gateway created", created, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentGatewayConfig>> updateGateway(
            @PathVariable Long id,
            @RequestBody PaymentGatewayConfig config,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        PaymentGatewayConfig updated = paymentGatewayConfigService.updateGateway(id, config, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment gateway updated", updated, null));
    }

    @PostMapping("/enable/{gatewayCode}")
    public ResponseEntity<ApiResponse<PaymentGatewayConfig>> enableGateway(
            @PathVariable String gatewayCode,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        PaymentGatewayConfig updated = paymentGatewayConfigService.enableGateway(gatewayCode, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment gateway enabled", updated, null));
    }

    @PostMapping("/disable/{gatewayCode}")
    public ResponseEntity<ApiResponse<PaymentGatewayConfig>> disableGateway(
            @PathVariable String gatewayCode,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        PaymentGatewayConfig updated = paymentGatewayConfigService.disableGateway(gatewayCode, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment gateway disabled", updated, null));
    }

    @GetMapping("/{gatewayCode}")
    public ResponseEntity<ApiResponse<PaymentGatewayConfig>> getGateway(
            @PathVariable String gatewayCode,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        PaymentGatewayConfig config = paymentGatewayConfigService.getGatewayByCode(gatewayCode, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Gateway retrieved", config, null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentGatewayConfig>>> getAllGateways(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<PaymentGatewayConfig> gateways = paymentGatewayConfigService.getAllGateways(tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Gateways retrieved", gateways, null));
    }

    @GetMapping("/enabled")
    public ResponseEntity<ApiResponse<List<PaymentGatewayConfig>>> getEnabledGateways(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<PaymentGatewayConfig> gateways = paymentGatewayConfigService.getEnabledGateways(tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Enabled gateways retrieved", gateways, null));
    }

    @GetMapping("/check/{gatewayCode}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkGatewayEnabled(
            @PathVariable String gatewayCode,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        boolean enabled = paymentGatewayConfigService.isGatewayEnabled(gatewayCode, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Gateway status", Map.of("enabled", enabled), null));
    }
}
