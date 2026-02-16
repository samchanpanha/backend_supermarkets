package com.supermarket.order.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.order.dto.OrderRequest;
import com.supermarket.order.dto.OrderResponse;
import com.supermarket.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        OrderResponse response = orderService.createOrder(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Order created successfully", response, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        OrderResponse response = orderService.getOrderById(id, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Order retrieved successfully", response, null));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @PathVariable String orderNumber,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        OrderResponse response = orderService.getOrderByNumber(orderNumber, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Order retrieved successfully", response, null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<OrderResponse> orders = orderService.getOrdersByTenant(tenantId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Orders retrieved successfully", orders, null));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        String status = request.get("status");
        OrderResponse response = orderService.updateOrderStatus(id, status, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Order status updated", response, null));
    }

    @PatchMapping("/{id}/payment")
    public ResponseEntity<ApiResponse<OrderResponse>> updatePaymentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        String paymentStatus = request.get("paymentStatus");
        String transactionId = request.get("transactionId");
        
        OrderResponse response = orderService.updatePaymentStatus(id, paymentStatus, transactionId, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment status updated", response, null));
    }
}
