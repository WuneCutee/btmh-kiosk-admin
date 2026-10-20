package vn.baotinmanhhai.kioskadmin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.baotinmanhhai.kioskadmin.entity.KioskAdminAuditLog;

public interface KioskAdminAuditLogRepository extends JpaRepository<KioskAdminAuditLog, Long> {

    Page<KioskAdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
