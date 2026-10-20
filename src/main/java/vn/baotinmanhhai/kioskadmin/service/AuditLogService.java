package vn.baotinmanhhai.kioskadmin.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.baotinmanhhai.kioskadmin.entity.KioskAdminAuditLog;
import vn.baotinmanhhai.kioskadmin.repository.KioskAdminAuditLogRepository;

import java.time.Instant;

/**
 * Ghi log hanh dong cua admin (bang kiosk_admin_audit_log, so huu rieng boi app nay).
 * QUAN TRONG: khong de loi ghi audit lam vo request goc dang xu ly - luon bat Exception va chi
 * log.error, khong throw lai.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final int MAX_PAGE_SIZE = 200;

    private final KioskAdminAuditLogRepository kioskAdminAuditLogRepository;

    public void record(String username, String action, String targetType, String targetId, String detail,
                        HttpServletRequest request) {
        try {
            KioskAdminAuditLog logEntry = KioskAdminAuditLog.builder()
                    .username(username)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .detail(detail)
                    .ipAddress(extractClientIp(request))
                    .createdAt(Instant.now())
                    .build();
            kioskAdminAuditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Khong ghi duoc audit log (action={}, username={}) - bo qua, khong lam vo request goc",
                    action, username, e);
        }
    }

    public Page<KioskAdminAuditLog> list(int page, int size) {
        int s = size <= 0 ? 50 : Math.min(size, MAX_PAGE_SIZE);
        return kioskAdminAuditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), s));
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For co the chua danh sach "client, proxy1, proxy2" - lay IP dau tien.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
