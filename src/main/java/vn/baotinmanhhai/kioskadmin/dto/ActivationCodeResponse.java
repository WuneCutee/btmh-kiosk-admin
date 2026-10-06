package vn.baotinmanhhai.kioskadmin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ActivationCodeResponse {
    private String deviceCode;
    private String activationCode;
    private Instant expiresAt;
    private String qrPayload;
}
