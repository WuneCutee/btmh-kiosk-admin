package vn.baotinmanhhai.kioskadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Bang MOI, so huu HOAN TOAN boi kiosk-admin (khong co service nao khac dong vao) - ghi lai
 * moi hanh dong thay doi du lieu tu UI/API cua web nay de truy vet sau nay. Tao bang qua JDBC
 * pre-flight trong KioskAdminApplication.main(), cung co che voi kiosk_admin_user.
 */
@Entity
@Table(name = "kiosk_admin_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskAdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(name = "detail", length = 1000)
    private String detail;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
