package com.example.demo.repository;

import com.example.demo.model.IngestionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionAuditLogRepository extends JpaRepository<IngestionAuditLog, Long> {
}
