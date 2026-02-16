package com.supermarket.common.util;

import com.supermarket.common.dto.CreateAuditLogRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Utility class for creating audit logs across all services.
 * Can be used synchronously or asynchronously via Kafka.
 */
@Component
@Slf4j
public class AuditLogUtil {

    private final KafkaTemplate<String, CreateAuditLogRequest> kafkaTemplate;
    private final String serviceName;

    public AuditLogUtil(
            KafkaTemplate<String, CreateAuditLogRequest> kafkaTemplate,
            @Value("${spring.application.name:unknown-service}") String serviceName) {
        this.kafkaTemplate = kafkaTemplate;
        this.serviceName = serviceName;
    }

    /**
     * Log an action synchronously (creates audit log directly)
     */
    public CreateAuditLogRequest buildAuditLog(
            String userId,
            String userName,
            String userEmail,
            String tenantId,
            String action,
            String entityType,
            String entityId,
            String description,
            String ipAddress,
            String userAgent,
            String requestMethod,
            String requestPath,
            Integer responseStatus,
            Boolean success,
            Long executionTimeMs) {

        return CreateAuditLogRequest.builder()
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .tenantId(tenantId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .requestMethod(requestMethod)
                .requestPath(requestPath)
                .responseStatus(responseStatus)
                .success(success != null ? success : true)
                .executionTimeMs(executionTimeMs)
                .serviceName(serviceName)
                .build();
    }

    /**
     * Log login action
     */
    public CreateAuditLogRequest buildLoginLog(
            String userId,
            String userName,
            String userEmail,
            String tenantId,
            String ipAddress,
            String userAgent,
            Boolean success,
            String errorMessage) {

        return CreateAuditLogRequest.builder()
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .tenantId(tenantId)
                .action(success ? CreateAuditLogRequest.ACTION_LOGIN : CreateAuditLogRequest.ACTION_LOGIN_FAILED)
                .description(success ? "User logged in successfully" : "Login failed: " + errorMessage)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(success)
                .errorMessage(errorMessage)
                .serviceName(serviceName)
                .build();
    }

    /**
     * Log CRUD action
     */
    public CreateAuditLogRequest buildCrudLog(
            String userId,
            String userName,
            String tenantId,
            String action, // CREATE, UPDATE, DELETE, VIEW
            String entityType,
            String entityId,
            String description,
            String oldValue,
            String newValue,
            String ipAddress,
            Integer responseStatus,
            Long executionTimeMs) {

        return CreateAuditLogRequest.builder()
                .userId(userId)
                .userName(userName)
                .tenantId(tenantId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .responseStatus(responseStatus)
                .success(responseStatus != null && responseStatus < 400)
                .executionTimeMs(executionTimeMs)
                .serviceName(serviceName)
                .build();
    }

    /**
     * Log payment action
     */
    public CreateAuditLogRequest buildPaymentLog(
            String userId,
            String userName,
            String tenantId,
            String orderId,
            Double amount,
            String paymentMethod,
            String status,
            String ipAddress,
            Integer responseStatus) {

        return CreateAuditLogRequest.builder()
                .userId(userId)
                .userName(userName)
                .tenantId(tenantId)
                .action(CreateAuditLogRequest.ACTION_PAYMENT)
                .entityType("ORDER")
                .entityId(orderId)
                .description(String.format("Payment of %.2f via %s - Status: %s", amount, paymentMethod, status))
                .ipAddress(ipAddress)
                .responseStatus(responseStatus)
                .success(responseStatus != null && responseStatus < 400)
                .serviceName(serviceName)
                .build();
    }

    /**
     * Log permission denied action
     */
    public CreateAuditLogRequest buildPermissionDeniedLog(
            String userId,
            String userName,
            String tenantId,
            String resource,
            String action,
            String ipAddress) {

        return CreateAuditLogRequest.builder()
                .userId(userId)
                .userName(userName)
                .tenantId(tenantId)
                .action(CreateAuditLogRequest.ACTION_PERMISSION_DENIED)
                .description(String.format("Access denied to %s for action: %s", resource, action))
                .ipAddress(ipAddress)
                .success(false)
                .serviceName(serviceName)
                .build();
    }

    /**
     * Send audit log asynchronously via Kafka
     */
    public void sendAsyncAuditLog(CreateAuditLogRequest auditLogRequest) {
        try {
            kafkaTemplate.send("audit-logs", auditLogRequest);
            log.debug("Sent audit log to Kafka: action={}, userId={}", 
                    auditLogRequest.getAction(), auditLogRequest.getUserId());
        } catch (Exception e) {
            log.error("Failed to send audit log to Kafka: {}", e.getMessage(), e);
        }
    }

    /**
     * Convenience method to log and send asynchronously
     */
    public void logAndSend(
            String userId,
            String userName,
            String tenantId,
            String action,
            String entityType,
            String entityId,
            String description,
            String ipAddress,
            Integer responseStatus) {

        CreateAuditLogRequest request = buildCrudLog(
                userId, userName, tenantId, action, entityType, entityId,
                description, null, null, ipAddress, responseStatus, null);
        sendAsyncAuditLog(request);
    }
}
