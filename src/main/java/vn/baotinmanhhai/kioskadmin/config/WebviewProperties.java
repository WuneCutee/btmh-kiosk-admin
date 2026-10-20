package vn.baotinmanhhai.kioskadmin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cau hinh ket noi sang Backend/webview - dung de doc trang thai "online ngay bay gio" cua may
 * kiosk (KioskSseConnectionRegistry, chi song trong RAM cua webview, khong nam o DB localdb ma
 * kiosk-admin dang doc truc tiep cho CRUD may). Bind tu bien moi truong WEBVIEW_BASE_URL.
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "webview")
public class WebviewProperties {
    private String baseUrl = "http://localhost:8080";
    /** Phai khop KIOSK_INTERNAL_SERVICE_KEY cua webview - cac API /api/v1/kiosk/admin/** deu bi
     * KioskInternalAuthFilter (ben webview) chan bang header nay, fail-closed neu sai/thieu. */
    private String internalServiceKey = "";
}
