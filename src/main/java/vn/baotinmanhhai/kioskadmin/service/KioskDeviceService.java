package vn.baotinmanhhai.kioskadmin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.baotinmanhhai.kioskadmin.dto.ActivationCodeResponse;
import vn.baotinmanhhai.kioskadmin.dto.DeviceResponse;
import vn.baotinmanhhai.kioskadmin.dto.RegisterDeviceRequest;
import vn.baotinmanhhai.kioskadmin.entity.KioskActivationCode;
import vn.baotinmanhhai.kioskadmin.entity.KioskDevice;
import vn.baotinmanhhai.kioskadmin.exception.BusinessException;
import vn.baotinmanhhai.kioskadmin.repository.KioskActivationCodeRepository;
import vn.baotinmanhhai.kioskadmin.repository.KioskDeviceRepository;
import vn.baotinmanhhai.kioskadmin.util.HashUtil;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Port 1:1 logic tu
 * Backend/webview/src/main/java/vn/baotinmanhhai/miniapp/b2bank/service/kiosk/KioskDeviceAdminService.java
 * - CHI khac: khong co KioskSseConnectionRegistry (khong co ket noi SSE dang mo o service nay,
 * chap nhan may kiosk phat hien bi revoke o lan goi API tiep theo thay vi real-time).
 */
@Service
@RequiredArgsConstructor
public class KioskDeviceService {

    // BUG THAT da tim thay 15/10/2026 (khop voi webview - port 1:1 tu KioskDeviceAdminService.java): bang nay
    // TRUOC DAY la Base32 chuan, khong khop voi app that (ActivationInput.kt: khong co I/L/O/U, co ca 0/1,
    // tu doi I/L->1 va O->0 khi nhan vien go tay) - nhan vien khong go duoc dung moi ma chua I/O/L/U.
    private static final String BASE32_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int ACTIVATION_CODE_LENGTH = 12;
    private static final Duration ACTIVATION_CODE_TTL = Duration.ofHours(24);

    private final KioskDeviceRepository kioskDeviceRepository;
    private final KioskActivationCodeRepository kioskActivationCodeRepository;
    private final ObjectMapper objectMapper;
    private final WebviewClient webviewClient;
    private final SecureRandom secureRandom = new SecureRandom();

    public List<DeviceResponse> listAll() {
        java.util.Set<String> online = webviewClient.onlineDeviceCodes();
        return kioskDeviceRepository.findAll().stream().map(d -> toDto(d, online)).toList();
    }

    public DeviceResponse getByCode(String deviceCode) {
        return toDto(findDeviceOrThrow(deviceCode), webviewClient.onlineDeviceCodes());
    }

    @Transactional
    public ActivationCodeResponse register(RegisterDeviceRequest request) {
        if (kioskDeviceRepository.existsById(request.getDeviceCode())) {
            throw new BusinessException("Ma may da ton tai: " + request.getDeviceCode(), "409", 409);
        }
        KioskDevice device = KioskDevice.builder()
                .deviceCode(request.getDeviceCode())
                .storeId(request.getStoreId())
                .storeName(request.getStoreName())
                .note(request.getNote())
                .status(KioskDevice.STATUS_PENDING)
                .createdAt(Instant.now())
                .forceKeyRotation(false)
                .build();
        kioskDeviceRepository.save(device);
        return issueActivationCode(device.getDeviceCode());
    }

    @Transactional
    public ActivationCodeResponse reissueActivationCode(String deviceCode) {
        KioskDevice device = findDeviceOrThrow(deviceCode);
        if (device.isActive()) {
            throw new BusinessException("May da ACTIVE, khong can ma kich hoat", "409", 409);
        }
        if (device.isRevoked()) {
            throw new BusinessException("May da bi thu hoi", "409", 409);
        }
        kioskActivationCodeRepository
                .findFirstByDeviceCodeAndUsedFalseAndRevokedFalseOrderByCreatedAtDesc(deviceCode)
                .ifPresent(old -> {
                    old.setRevoked(true);
                    kioskActivationCodeRepository.save(old);
                });
        return issueActivationCode(deviceCode);
    }

    @Transactional
    public DeviceResponse revoke(String deviceCode) {
        KioskDevice device = findDeviceOrThrow(deviceCode);
        device.setStatus(KioskDevice.STATUS_REVOKED);
        return toDto(kioskDeviceRepository.save(device), webviewClient.onlineDeviceCodes());
    }

    @Transactional
    public DeviceResponse forceKeyRotation(String deviceCode) {
        KioskDevice device = findDeviceOrThrow(deviceCode);
        device.setForceKeyRotation(true);
        return toDto(kioskDeviceRepository.save(device), webviewClient.onlineDeviceCodes());
    }

    @Transactional
    public void delete(String deviceCode) {
        if (!kioskDeviceRepository.existsById(deviceCode)) {
            throw new BusinessException("Khong tim thay may kiosk: " + deviceCode, "404", 404);
        }
        // Xoa luon cac ma kich hoat lien quan de tranh mo coi (khong co FK ON DELETE CASCADE trong
        // schema goc, tu xu ly o tang service).
        kioskActivationCodeRepository.deleteByDeviceCode(deviceCode);
        kioskDeviceRepository.deleteById(deviceCode);
    }

    private ActivationCodeResponse issueActivationCode(String deviceCode) {
        String code = generateActivationCode();
        Instant expiresAt = Instant.now().plus(ACTIVATION_CODE_TTL);
        KioskActivationCode entity = KioskActivationCode.builder()
                .id(UUID.randomUUID().toString())
                .deviceCode(deviceCode)
                .codeHash(HashUtil.sha256Hex(code))
                .expiresAt(expiresAt)
                .used(false)
                .revoked(false)
                .attempts(0)
                .createdAt(Instant.now())
                .build();
        kioskActivationCodeRepository.save(entity);

        String qrPayload;
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("deviceCode", deviceCode);
            payload.put("activationCode", code);
            qrPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            qrPayload = null;
        }

        return ActivationCodeResponse.builder()
                .deviceCode(deviceCode)
                .activationCode(code)
                .expiresAt(expiresAt)
                .qrPayload(qrPayload)
                .build();
    }

    private String generateActivationCode() {
        StringBuilder sb = new StringBuilder(ACTIVATION_CODE_LENGTH);
        for (int i = 0; i < ACTIVATION_CODE_LENGTH; i++) {
            sb.append(BASE32_ALPHABET.charAt(secureRandom.nextInt(BASE32_ALPHABET.length())));
        }
        return sb.toString();
    }

    private KioskDevice findDeviceOrThrow(String deviceCode) {
        return kioskDeviceRepository.findById(deviceCode)
                .orElseThrow(() -> new BusinessException("Khong tim thay may kiosk: " + deviceCode, "404", 404));
    }

    private static DeviceResponse toDto(KioskDevice d, java.util.Set<String> online) {
        return DeviceResponse.builder()
                .deviceCode(d.getDeviceCode())
                .publicKey(d.getPublicKey())
                .keyId(d.getKeyId())
                .storeId(d.getStoreId())
                .storeName(d.getStoreName())
                .status(d.getStatus())
                .note(d.getNote())
                .appVersion(d.getAppVersion())
                .osVersion(d.getOsVersion())
                .deviceModel(d.getDeviceModel())
                .createdAt(d.getCreatedAt())
                .activatedAt(d.getActivatedAt())
                .lastRotatedAt(d.getLastRotatedAt())
                .forceKeyRotation(d.isForceKeyRotation())
                .online(online.contains(d.getDeviceCode()))
                .build();
    }
}
