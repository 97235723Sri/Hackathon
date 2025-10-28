package com.example.controlpanel.repository;

import com.example.controlpanel.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByButtonIdOrderByExecutedAtDesc(Long buttonId);
    List<AuditLog> findByExecutedByOrderByExecutedAtDesc(String executedBy);
}
