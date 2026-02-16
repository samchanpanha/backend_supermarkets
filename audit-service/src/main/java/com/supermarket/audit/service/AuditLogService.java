package com.supermarket.audit.service;

import com.supermarket.audit.dto.AuditLogDTO;
import com.supermarket.audit.entity.AuditLog;
import com.supermarket.audit.repository.AuditLogRepository;
import com.supermarket.common.dto.CreateAuditLogRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final KafkaTemplate<String, CreateAuditLogRequest> kafkaTemplate;

    private static final String AUDIT_TOPIC = "audit-logs";

    @Transactional
    public AuditLogDTO createAuditLog(CreateAuditLogRequest request) {
        log.debug("Creating audit log: action={}, userId={}, tenantId={}", 
                  request.getAction(), request.getUserId(), request.getTenantId());

        AuditLog auditLog = AuditLog.builder()
                .userId(request.getUserId())
                .userName(request.getUserName())
                .userEmail(request.getUserEmail())
                .tenantId(request.getTenantId())
                .action(request.getAction())
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .description(request.getDescription())
                .oldValue(request.getOldValue())
                .newValue(request.getNewValue())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .requestMethod(request.getRequestMethod())
                .requestPath(request.getRequestPath())
                .requestParams(request.getRequestParams())
                .responseStatus(request.getResponseStatus())
                .errorMessage(request.getErrorMessage())
                .success(request.getSuccess() != null ? request.getSuccess() : true)
                .executionTimeMs(request.getExecutionTimeMs())
                .serviceName(request.getServiceName())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit log created: id={}, action={}", saved.getId(), saved.getAction());

        return toDTO(saved);
    }

    public void createAuditLogAsync(CreateAuditLogRequest request) {
        kafkaTemplate.send(AUDIT_TOPIC, request);
    }

    @KafkaListener(topics = AUDIT_TOPIC, groupId = "audit-service")
    public void handleAuditLog(CreateAuditLogRequest request) {
        try {
            createAuditLog(request);
        } catch (Exception e) {
            log.error("Failed to process audit log: {}", e.getMessage(), e);
        }
    }

    public Page<AuditLogDTO> getAuditLogs(String tenantId, String userId, String action, 
                                          String entityType, LocalDateTime startDate, 
                                          LocalDateTime endDate, Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.searchAuditLogs(
                tenantId, userId, action, entityType, startDate, endDate, pageable);
        return logs.map(this::toDTO);
    }

    public Page<AuditLogDTO> getAuditLogsByTenant(String tenantId, Pageable pageable) {
        return auditLogRepository.findByTenantId(tenantId, pageable).map(this::toDTO);
    }

    public Page<AuditLogDTO> getAuditLogsByUser(String userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable).map(this::toDTO);
    }

    public Page<AuditLogDTO> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable).map(this::toDTO);
    }

    public List<AuditLogDTO> getAuditLogsByEntity(String entityId, String entityType) {
        return auditLogRepository.findByEntityIdAndEntityType(entityId, entityType)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<AuditLogDTO> getAuditLogsByDateRange(String tenantId, LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTenantIdAndCreatedAtBetween(tenantId, start, end)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public Map<String, Long> getActionSummary(String tenantId, LocalDateTime startDate) {
        List<Object[]> results = auditLogRepository.countByAction(tenantId, startDate);
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (String) result[0],
                        result -> (Long) result[1]
                ));
    }

    public List<Map<String, Object>> getMostActiveUsers(String tenantId, LocalDateTime startDate, int limit) {
        List<Object[]> results = auditLogRepository.findMostActiveUsers(
                tenantId, startDate, Pageable.ofSize(limit));
        return results.stream()
                .map(result -> Map.of(
                        "userId", (String) result[0],
                        "count", (Long) result[1]
                ))
                .collect(Collectors.toList());
    }

    public Long getFailedActionCount(String tenantId, String action, LocalDateTime startDate) {
        return auditLogRepository.countByActionAndDate(tenantId, action, startDate);
    }

    public List<AuditLogDTO> getFailedActivities(String tenantId, LocalDateTime startDate) {
        return auditLogRepository.findFailedActivities(tenantId, startDate)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public AuditLogDTO getAuditLogById(Long id) {
        return auditLogRepository.findById(id).map(this::toDTO).orElse(null);
    }

    private AuditLogDTO toDTO(AuditLog auditLog) {
        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .userName(auditLog.getUserName())
                .userEmail(auditLog.getUserEmail())
                .tenantId(auditLog.getTenantId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .description(auditLog.getDescription())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .requestMethod(auditLog.getRequestMethod())
                .requestPath(auditLog.getRequestPath())
                .requestParams(auditLog.getRequestParams())
                .responseStatus(auditLog.getResponseStatus())
                .errorMessage(auditLog.getErrorMessage())
                .success(auditLog.getSuccess())
                .executionTimeMs(auditLog.getExecutionTimeMs())
                .serviceName(auditLog.getServiceName())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
