package com.supermarket.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities", indexes = {
    @Index(name = "idx_activity_user_id", columnList = "user_id"),
    @Index(name = "idx_activity_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_activity_session_id", columnList = "session_id"),
    @Index(name = "idx_activity_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(nullable = false)
    private String activityType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "location")
    private String location;

    @Column(name = "resource")
    private String resource;

    @Column(name = "action")
    private String action;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "bytes_in")
    private Long bytesIn;

    @Column(name = "bytes_out")
    private Long bytesOut;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Activity type constants
    public static final String TYPE_LOGIN = "LOGIN";
    public static final String TYPE_LOGOUT = "LOGOUT";
    public static final String TYPE_PAGE_VIEW = "PAGE_VIEW";
    public static final String TYPE_API_CALL = "API_CALL";
    public static final String TYPE_FILE_UPLOAD = "FILE_UPLOAD";
    public static final String TYPE_FILE_DOWNLOAD = "FILE_DOWNLOAD";
    public static final String TYPE_SEARCH = "SEARCH";
    public static final String TYPE_TRANSACTION = "TRANSACTION";
    public static final String TYPE_SESSION = "SESSION";
}
