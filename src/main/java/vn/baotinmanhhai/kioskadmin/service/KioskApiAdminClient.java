package vn.baotinmanhhai.kioskadmin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import vn.baotinmanhhai.kioskadmin.config.KioskApiProperties;
import vn.baotinmanhhai.kioskadmin.exception.BusinessException;

import java.util.UUID;

/**
 * Goi bo API quan tri noi bo cua Backend/kiosk-api (/internal/ops/**, header X-Ops-Key) - noi App Kiosk
 * doc quang cao, banner, cau hinh kenh, thiet bi, log loi. kiosk-admin chi chuyen tiep: kiem tra hop le
 * nam o kiosk-api, loi 4xx cua kiosk-api duoc tra nguyen ma + thong diep cho nguoi dung.
 * Envelope kiosk-api: {code, message, data, timestamp}.
 */
@Service
@Slf4j
public class KioskApiAdminClient {

    private final KioskApiProperties properties;
    private final ObjectMapper objectMapper;
    private RestClient restClient;

    public KioskApiAdminClient(KioskApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JsonNode get(String path) {
        return exchange(HttpMethod.GET, path, null);
    }

    public JsonNode post(String path, Object body) {
        return exchange(HttpMethod.POST, path, body);
    }

    public JsonNode put(String path, Object body) {
        return exchange(HttpMethod.PUT, path, body);
    }

    public JsonNode delete(String path) {
        return exchange(HttpMethod.DELETE, path, null);
    }

    private JsonNode exchange(HttpMethod method, String path, Object body) {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            throw new BusinessException("Chua cau hinh KIOSK_API_BASE_URL cho kiosk-admin", "500", 500);
        }
        try {
            RestClient.RequestBodySpec spec = client().method(method).uri(path)
                    .header("X-Ops-Key", properties.getOpsKey())
                    .header("X-Trace-Id", UUID.randomUUID().toString());
            if (body != null) {
                spec.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            String raw = spec.retrieve().body(String.class);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readTree(raw).path("data");
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String message = messageOf(e.getResponseBodyAsString());
            log.warn("kiosk-api tra HTTP {} cho {} {}: {}", status, method, path, e.getResponseBodyAsString());
            // 4xx la loi nghiep vu/du lieu nguoi dung nhap -> tra nguyen; con lai la loi he thong.
            int outStatus = status >= 400 && status < 500 ? status : 502;
            throw new BusinessException(message != null ? message : "kiosk-api tra loi HTTP " + status,
                    String.valueOf(outStatus), outStatus);
        } catch (RestClientException e) {
            throw new BusinessException("Khong ket noi duoc toi kiosk-api: " + e.getMessage(), "502", 502);
        } catch (java.io.IOException e) {
            throw new BusinessException("Phan hoi kiosk-api khong hop le", "502", 502);
        }
    }

    /** Lay thong diep loi tu envelope kiosk-api; loi VALIDATION_ERROR thi ghep danh sach truong sai. */
    private String messageOf(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            String message = node.path("message").asText(null);
            JsonNode items = node.path("data");
            if ("VALIDATION_ERROR".equals(message) && items.isArray() && !items.isEmpty()) {
                StringBuilder sb = new StringBuilder("Du lieu khong hop le: ");
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(items.get(i).path("field").asText()).append(" (")
                            .append(items.get(i).path("code").asText()).append(')');
                }
                return sb.toString();
            }
            return message;
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
        }
        return restClient;
    }
}
