package vn.baotinmanhhai.kioskadmin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.baotinmanhhai.kioskadmin.config.KioskApiProperties;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bao kiosk-api POST /internal/ops/catalog-changed sau moi lan sua catalog thanh cong. Chay nen, loi
 * chi log - KHONG duoc lam hong thao tac luu cua nguoi dung (kiosk-api van tu poll 5 giay du phong).
 */
@Service
@Slf4j
public class KioskApiNotifier {

    private final KioskApiProperties properties;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kiosk-api-notify");
        t.setDaemon(true);
        return t;
    });
    private RestClient restClient;

    public KioskApiNotifier(KioskApiProperties properties) {
        this.properties = properties;
    }

    public void catalogChanged(String reason) {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            return;
        }
        executor.execute(() -> {
            try {
                client().post()
                        .uri("/internal/ops/catalog-changed")
                        .header("X-Ops-Key", properties.getOpsKey())
                        .header("X-Trace-Id", UUID.randomUUID().toString())
                        .retrieve()
                        .toBodilessEntity();
                log.debug("Da bao kiosk-api catalog doi ({})", reason);
            } catch (RestClientException e) {
                log.warn("Khong bao duoc kiosk-api catalog doi ({}): {} - App se nhan qua vong poll 5 giay",
                        reason, e.toString());
            }
        });
    }

    private RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
        }
        return restClient;
    }
}
