package vn.baotinmanhhai.kioskadmin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.baotinmanhhai.kioskadmin.config.AdminPortalProperties;
import vn.baotinmanhhai.kioskadmin.dto.AdminPortalEnvelope;
import vn.baotinmanhhai.kioskadmin.dto.AdminPortalLoginData;
import vn.baotinmanhhai.kioskadmin.exception.BusinessException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Dang nhap vao admin-portal (POST /api/v1/auth/login) bang tai khoan ky thuat rieng cua
 * kiosk-admin (ADMIN_PORTAL_USERNAME/ADMIN_PORTAL_PASSWORD, tai khoan nay da duoc cap quyen
 * partner_code=KIOSK ben admin-portal), cache JWT trong bo nho va tu dang nhap lai khi token
 * gan het han (bien tru 5 phut). App nay tai thap (dung noi bo), 1 bien cache + synchronized la
 * du, khong can co che phuc tap hon (vi du refresh token rieng - admin-portal co tra
 * refreshToken nhung dang nhap lai bang password don gian hon va du dung cho muc dich nay).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPortalAuthClient {

    private static final Duration REFRESH_MARGIN = Duration.ofMinutes(5);
    private static final long DEFAULT_EXPIRES_IN_MS = 3_600_000L;

    private final AdminPortalProperties properties;

    private RestClient restClient;
    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    private RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
        }
        return restClient;
    }

    /** Tra ve JWT con hieu luc, tu dang nhap lai neu chua co hoac gan het han. */
    public synchronized String getToken() {
        if (cachedToken == null || Instant.now().isAfter(expiresAt.minus(REFRESH_MARGIN))) {
            login();
        }
        return cachedToken;
    }

    private void login() {
        try {
            AdminPortalEnvelope<AdminPortalLoginData> envelope = client().post()
                    .uri("/api/v1/auth/login")
                    .body(Map.of("username", properties.getUsername(), "password", properties.getPassword()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<AdminPortalEnvelope<AdminPortalLoginData>>() {
                    });
            if (envelope == null || !AdminPortalEnvelope.STATUS_SUCCESS.equals(envelope.getStatus())
                    || envelope.getData() == null || envelope.getData().getToken() == null) {
                String msg = envelope != null ? envelope.getMessage() : "khong co phan hoi";
                throw new BusinessException("Dang nhap admin-portal that bai: " + msg, "502", 502);
            }
            long expiresInMs = envelope.getData().getExpiresIn() != null
                    ? envelope.getData().getExpiresIn() : DEFAULT_EXPIRES_IN_MS;
            cachedToken = envelope.getData().getToken();
            expiresAt = Instant.now().plusMillis(expiresInMs);
            log.info("Da dang nhap admin-portal bang tai khoan ky thuat, token het han luc {}", expiresAt);
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            // QUAN TRONG: day la thong tin chuan doan can cho nguoi van hanh kiosk-admin (vi du
            // admin-portal chua chay / sai ADMIN_PORTAL_BASE_URL) - KHAC voi loi he thong noi bo
            // (GlobalExceptionHandler#handleGeneric) can giau chi tiet voi client cuoi.
            throw new BusinessException(
                    "Khong ket noi duoc toi admin-portal de dang nhap (" + properties.getBaseUrl() + "): "
                            + e.getMessage(), "502", 502);
        }
    }
}
