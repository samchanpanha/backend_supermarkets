package com.supermarket.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Data transfer object for creating audit logs.
 * This can be used across all services to log user activities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAuditLogRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    private String userName;

    private String userEmail;

    private String tenantId;

    @NotBlank(message = "Action is required")
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

    // Action constants
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_VIEW = "VIEW";
    public static final String ACTION_EXPORT = "EXPORT";
    public static final String ACTION_IMPORT = "IMPORT";
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_PAYMENT = "PAYMENT";
    public static final String ACTION_REFUND = "REFUND";
    public static final String ACTION_PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String ACTION_ACCESS_DENIED = "ACCESS_DENIED";
}
