package com.supermarket.payment.service;

import com.supermarket.payment.dto.PaymentRequest;
import com.supermarket.payment.dto.PaymentResponse;
import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse processPayment(PaymentRequest request, String tenantId) {
        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setOrderId(request.getOrderId());
        payment.setOrderNumber(request.getOrderNumber());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus("PROCESSING");
        
        // Simulate payment processing
        boolean success = simulatePaymentProcessing(request);
        
        if (success) {
            payment.setStatus("SUCCESS");
            payment.setGatewayResponse("Payment processed successfully");
        } else {
            payment.setStatus("FAILED");
            payment.setGatewayResponse("Payment failed");
        }
        
        payment.setProcessedAt(LocalDateTime.now());
        
        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id, String tenantId) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (!payment.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to payment");
        }
        
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId, String tenantId) {
        Payment payment = paymentRepository.findByOrderIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return mapToResponse(payment);
    }

    private boolean simulatePaymentProcessing(PaymentRequest request) {
        // Simulate payment gateway processing
        return true;
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setTenantId(payment.getTenantId());
        response.setTransactionId(payment.getTransactionId());
        response.setOrderId(payment.getOrderId());
        response.setOrderNumber(payment.getOrderNumber());
        response.setAmount(payment.getAmount());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setStatus(payment.getStatus());
        response.setGatewayResponse(payment.getGatewayResponse());
        response.setProcessedAt(payment.getProcessedAt());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }
}
