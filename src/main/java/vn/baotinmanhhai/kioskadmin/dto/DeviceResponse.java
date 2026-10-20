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
    /** true neu webview dang giu ket noi SSE mo cho may nay NGAY LUC GOI - khac voi status=ACTIVE
     * (chi la da kich hoat/chua bi thu hoi, khong phan anh ket noi thuc te). */
    private boolean online;
}
