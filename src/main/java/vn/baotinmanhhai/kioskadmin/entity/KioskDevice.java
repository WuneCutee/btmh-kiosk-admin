package vn.baotinmanhhai.kioskadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "kiosk_device")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskDevice {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REVOKED = "REVOKED";

    @Id
    @Column(name = "device_code", length = 50)
    private String deviceCode;

    @Column(name = "note")
    private String note;

    @Column(name = "public_key")
    private String publicKey;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "force_key_rotation")
    private boolean forceKeyRotation;

    @Column(name = "key_id", length = 50)
    private String keyId;

    @Column(name = "last_rotated_at")
    private Instant lastRotatedAt;

    @Column(name = "os_version", length = 20)
    private String osVersion;

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean isRevoked() {
        return STATUS_REVOKED.equals(status);
    }
}
