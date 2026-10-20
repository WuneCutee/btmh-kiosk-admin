package vn.baotinmanhhai.kioskadmin.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.baotinmanhhai.kioskadmin.config.WebviewProperties;

import java.util.Set;

/**
 * Doc trang thai "online ngay bay gio" cua may kiosk tu Backend/webview
 * (KioskSseConnectionRegistry qua KioskDeviceAdminController./online) - danh sach nay chi song
 * trong RAM cua webview luc goi, KHONG nam trong DB localdb ma kiosk-admin dang tu doc/ghi cho
 * CRUD may (xem KioskDeviceService). Loi/webview tat -> coi nhu "khong biet" (tra set rong), KHONG
 * duoc chan man Danh sach may vi 1 tinh nang phu nay.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebviewClient {

    private final WebviewProperties properties;
    private RestClient restClient;

    private RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
        }
        return restClient;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OnlineEnvelope {
        private Set<String> data;
    }

    public Set<String> onlineDeviceCodes() {
        try {
            OnlineEnvelope envelope = client().get()
                    .uri("/api/v1/kiosk/admin/devices/online")
                    .header("X-Internal-Service-Key", properties.getInternalServiceKey())
                    .retrieve()
                    .body(OnlineEnvelope.class);
            return envelope != null && envelope.getData() != null ? envelope.getData() : Set.of();
        } catch (RestClientException e) {
            log.warn("Khong doc duoc trang thai online tu webview ({}): {}", properties.getBaseUrl(), e.toString());
            return Set.of();
        }
    }
}
