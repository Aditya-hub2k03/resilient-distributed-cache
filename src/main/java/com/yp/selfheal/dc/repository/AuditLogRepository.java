package com.yp.selfheal.dc.repository;

import com.yp.selfheal.dc.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
