package vn.baotinmanhhai.kioskadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Phan "data" cua phan hoi POST /api/v1/auth/login ben admin-portal. Chi lay 2 truong can
 * dung (token, expiresIn) - bo qua refreshToken/refreshExpiresIn vi app nay khong dung refresh
 * token, chi dang nhap lai bang username/password khi token gan het han.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminPortalLoginData {
    private String token;
    private String tokenType;
    private String username;
    /** Thoi gian song cua token, don vi MILLISECOND (xac nhan tu phan hoi thuc te: 3600000 = 1h). */
    private Long expiresIn;
}
