package com.supermarket.audit.controller;

import com.supermarket.audit.dto.AuditLogDTO;
import com.supermarket.audit.service.AuditLogService;
import com.supermarket.common.dto.CreateAuditLogRequest;
import com.supermarket.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<ApiResponse<AuditLogDTO>> createAuditLog(
            @Valid @RequestBody CreateAuditLogRequest request) {
        AuditLogDTO created = auditLogService.createAuditLog(request);
        return ResponseEntity.ok(ApiResponse.success(created, "Audit log created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<AuditLogDTO>>> getAuditLogs(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AuditLogDTO> logs = auditLogService.getAuditLogs(
                tenantId, userId, action, entityType, startDate, endDate, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<AuditLogDTO>>> getAuditLogsByTenant(
            @PathVariable String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogDTO> logs = auditLogService.getAuditLogsByTenant(tenantId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<AuditLogDTO>>> getAuditLogsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogDTO> logs = auditLogService.getAuditLogsByUser(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/action/{action}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<AuditLogDTO>>> getAuditLogsByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogDTO> logs = auditLogService.getAuditLogsByAction(action, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/entity/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getAuditLogsByEntity(
            @PathVariable String entityId,
            @RequestParam String entityType) {
        
        List<AuditLogDTO> logs = auditLogService.getAuditLogsByEntity(entityId, entityType);
        
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getActionSummary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate) {
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(7);
        }
        
        Map<String, Long> summary = auditLogService.getActionSummary(tenantId, startDate);
        
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/active-users")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMostActiveUsers(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(defaultValue = "10") int limit) {
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(7);
        }
        
        List<Map<String, Object>> users = auditLogService.getMostActiveUsers(tenantId, startDate, limit);
        
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getFailedActivities(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate) {
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(1);
        }
        
        List<AuditLogDTO> failedLogs = auditLogService.getFailedActivities(tenantId, startDate);
        
        return ResponseEntity.ok(ApiResponse.success(failedLogs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<AuditLogDTO>> getAuditLogById(@PathVariable Long id) {
        AuditLogDTO log = auditLogService.getAuditLogById(id);
        
        if (log == null) {
            return ResponseEntity.ok(ApiResponse.error("Audit log not found"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(log));
    }
}
