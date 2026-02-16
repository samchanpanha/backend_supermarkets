package com.supermarket.audit.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {

    private Long id;
    private String userId;
    private String userName;
    private String userEmail;
    private String tenantId;
    private String action;
    private String entityType;
    private String entityId;
    private String description;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String userAgent;
    private String requestMethod;
    private String requestPath;
    private String requestParams;
    private Integer responseStatus;
    private String errorMessage;
    private Boolean success;
    private Long executionTimeMs;
    private String serviceName;
    private LocalDateTime createdAt;
}
