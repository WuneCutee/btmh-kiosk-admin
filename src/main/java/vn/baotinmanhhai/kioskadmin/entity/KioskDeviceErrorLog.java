package vn.baotinmanhhai.kioskadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Shadow mapping cua bang "kiosk_device_error_log" - bang nay duoc webview ghi vao (may kiosk bao
 * loi len webview), kiosk-admin CHI DOC de hien thi cho nguoi quan tri xem. Ca 2 ben (webview va
 * kiosk-admin) deu tu tao bang nay bang JDBC pre-flight (CREATE TABLE IF NOT EXISTS) de khong
 * phu thuoc thu tu deploy - xem KioskAdminApplication.main().
 */
@Entity
@Table(name = "kiosk_device_error_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskDeviceErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_code", length = 100)
    private String deviceCode;

    @Column(name = "error_id", length = 36)
    private String errorId;

    @Column(name = "error_type", length = 30)
    private String errorType;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "context")
    private String context;

    @Column(name = "received_at")
    private Instant receivedAt;
}
