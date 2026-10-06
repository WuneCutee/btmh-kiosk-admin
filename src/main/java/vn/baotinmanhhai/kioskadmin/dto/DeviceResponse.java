package vn.baotinmanhhai.kioskadmin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class DeviceResponse {
    private String deviceCode;
    private String publicKey;
    private String keyId;
    private Long storeId;
    private String storeName;
    private String status;
    private String note;
    private String appVersion;
    private String osVersion;
    private String deviceModel;
    private Instant createdAt;
    private Instant activatedAt;
    private Instant lastRotatedAt;
    private boolean forceKeyRotation;
}
