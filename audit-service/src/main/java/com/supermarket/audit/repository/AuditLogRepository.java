package com.supermarket.audit.repository;

import com.supermarket.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByTenantId(String tenantId, Pageable pageable);

    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndUserId(String tenantId, String userId, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    List<AuditLog> findByEntityIdAndEntityType(String entityId, String entityType);

    List<AuditLog> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate)")
    Page<AuditLog> searchAuditLogs(
            @Param("tenantId") String tenantId,
            @Param("userId") String userId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.createdAt >= :startDate GROUP BY a.action")
    List<Object[]> countByAction(@Param("tenantId") String tenantId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT a.userId, COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.createdAt >= :startDate GROUP BY a.userId ORDER BY COUNT(a) DESC")
    List<Object[]> findMostActiveUsers(@Param("tenantId") String tenantId, @Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId AND a.action = :action " +
           "AND a.createdAt >= :startDate")
    Long countByActionAndDate(@Param("tenantId") String tenantId, @Param("action") String action, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT a FROM AuditLog a WHERE a.success = false AND a.tenantId = :tenantId " +
           "AND a.createdAt >= :startDate ORDER BY a.createdAt DESC")
    List<AuditLog> findFailedActivities(@Param("tenantId") String tenantId, @Param("startDate") LocalDateTime startDate);
}
