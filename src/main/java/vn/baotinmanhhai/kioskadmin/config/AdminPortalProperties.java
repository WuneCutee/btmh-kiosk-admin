package vn.baotinmanhhai.kioskadmin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cau hinh ket noi sang Backend/admin-portal (cong quan tri thuc su quan ly san pham/nhom san
 * pham/ton kho) - kiosk-admin la HTTP CLIENT cua cac API nay, KHONG tu ghi lai logic do (xem
 * README phan "Vi sao kiosk-admin khong tu lam CRUD san pham"). Bind tu bien moi truong
 * ADMIN_PORTAL_BASE_URL/ADMIN_PORTAL_USERNAME/ADMIN_PORTAL_PASSWORD, cung quy uoc voi
 * KIOSK_JWT_SECRET da dung trong application.yml.
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "admin-portal")
public class AdminPortalProperties {
    private String baseUrl = "http://localhost:8386";
    private String username;
    private String password;
}
