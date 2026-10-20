package vn.baotinmanhhai.kioskadmin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ket noi sang Backend/kiosk-api (backend cua App Kiosk) - de bao "catalog vua doi" ngay sau khi luu
 * tren web, App nhan gia/ten/anh moi trong ~1 giay qua SSE. opsKey phai khop OPS_API_KEY cua kiosk-api.
 * Bo trong baseUrl = tat tinh nang (kiosk-api van tu poll 5 giay/lan).
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "kiosk-api")
public class KioskApiProperties {
    private String baseUrl = "";
    private String opsKey = "";
}
