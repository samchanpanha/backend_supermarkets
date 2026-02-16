package com.supermarket.payment.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.payment.dto.PaymentRequest;
import com.supermarket.payment.dto.PaymentResponse;
import com.supermarket.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        PaymentResponse response = paymentService.processPayment(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Payment processed", response, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        PaymentResponse response = paymentService.getPaymentById(id, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment retrieved", response, null));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment retrieved", response, null));
    }
}
