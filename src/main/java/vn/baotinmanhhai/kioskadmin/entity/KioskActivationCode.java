package vn.baotinmanhhai.kioskadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "kiosk_activation_code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskActivationCode {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "device_code", nullable = false, length = 50)
    private String deviceCode;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
