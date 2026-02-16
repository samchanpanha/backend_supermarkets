package com.supermarket.order.service;

import com.supermarket.order.dto.OrderRequest;
import com.supermarket.order.dto.OrderResponse;
import com.supermarket.order.entity.Order;
import com.supermarket.order.entity.OrderItem;
import com.supermarket.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResponse createOrder(OrderRequest request, String tenantId) {
        Order order = new Order();
        order.setTenantId(tenantId);
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerId(request.getCustomerId());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setStatus("PENDING");
        order.setSubtotal(request.getSubtotal());
        order.setTaxAmount(request.getTaxAmount());
        order.setDiscountAmount(request.getDiscountAmount());
        order.setTotalAmount(request.getTotalAmount());
        order.setShippingAddress(request.getShippingAddress());
        order.setBillingAddress(request.getBillingAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus("PENDING");

        Order saved = orderRepository.save(order);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, String tenantId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to order");
        }
        
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber, String tenantId) {
        Order order = orderRepository.findByOrderNumberAndTenantId(orderNumber, tenantId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByTenant(String tenantId, Pageable pageable) {
        return orderRepository.findByTenantId(tenantId, pageable)
                .map(this::mapToResponse);
    }

    public OrderResponse updateOrderStatus(Long id, String status, String tenantId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to order");
        }

        order.setStatus(status);
        Order saved = orderRepository.save(order);
        return mapToResponse(saved);
    }

    public OrderResponse updatePaymentStatus(Long id, String paymentStatus, String transactionId, String tenantId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to order");
        }

        order.setPaymentStatus(paymentStatus);
        order.setPaymentTransactionId(transactionId);
        
        if ("PAID".equals(paymentStatus)) {
            order.setStatus("CONFIRMED");
        }
        
        Order saved = orderRepository.save(order);
        return mapToResponse(saved);
    }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setTenantId(order.getTenantId());
        response.setOrderNumber(order.getOrderNumber());
        response.setCustomerId(order.getCustomerId());
        response.setCustomerName(order.getCustomerName());
        response.setCustomerEmail(order.getCustomerEmail());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setSubtotal(order.getSubtotal());
        response.setTaxAmount(order.getTaxAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setShippingAddress(order.getShippingAddress());
        response.setBillingAddress(order.getBillingAddress());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentTransactionId(order.getPaymentTransactionId());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }
}
