package vn.baotinmanhhai.kioskadmin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import vn.baotinmanhhai.kioskadmin.config.AdminPortalProperties;
import vn.baotinmanhhai.kioskadmin.dto.AdminPortalEnvelope;
import vn.baotinmanhhai.kioskadmin.dto.AdminPortalPage;
import vn.baotinmanhhai.kioskadmin.dto.CategoryRequest;
import vn.baotinmanhhai.kioskadmin.dto.CategoryResponse;
import vn.baotinmanhhai.kioskadmin.dto.InventoryHistoryResponse;
import vn.baotinmanhhai.kioskadmin.dto.InventoryResponse;
import vn.baotinmanhhai.kioskadmin.dto.PageResponse;
import vn.baotinmanhhai.kioskadmin.dto.ProductRequest;
import vn.baotinmanhhai.kioskadmin.dto.ProductResponse;
import vn.baotinmanhhai.kioskadmin.exception.BusinessException;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * HTTP client sang Backend/admin-portal cho category/product/inventory - kiosk-admin KHONG tu
 * viet lai logic tao san pham/ton kho (an toan ve tien/hang thuc te da nam o admin-portal: Redis
 * distributed lock, trading-hours guard, delta sync Redis qua afterCommit, floor-at-sold-quantity
 * ...). Kenh KIOSK duoc hardcode o day ("ALL" chi dung khi filter/list, con tao moi luon gan
 * đúng 1 partnerConfig KIOSK) - app nay KHONG expose bo chon nhieu kenh cho nguoi dung.
 *
 * <p>Tat ca phan hoi cua admin-portal co hinh dang {status,message,data,fieldErrors}
 * (AdminPortalEnvelope) - khac ApiResponse<T> cua chinh app nay. unwrap(...) la lop adapter DUY
 * NHAT giai ma hinh dang do va quy ve BusinessException khi status != SYS-200, de phan con lai
 * cua app (controller/UI) khong bao gio phai biet toi hinh dang cua admin-portal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPortalClient {

    private static final String PARTNER_CODE = "KIOSK";

    private final AdminPortalProperties properties;
    private final AdminPortalAuthClient authClient;
    private final ObjectMapper objectMapper;

    private RestClient restClient;

    private RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
        }
        return restClient;
    }

    // ---------- Category ----------

    public PageResponse<CategoryResponse> listCategories(int page, int size) {
        return call("lay danh sach nhom san pham", () -> {
            String uri = UriComponentsBuilder.fromPath("/api/v1/product-categories")
                    .queryParam("partnerCode", PARTNER_CODE)
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .build().toUriString();
            AdminPortalPage<CategoryResponse> p = unwrap(client().get().uri(uri)
                    .header("Authorization", bearer())
                    .retrieve()
                    .body(new ParameterizedTypeReference<AdminPortalEnvelope<AdminPortalPage<CategoryResponse>>>() {
                    }), "lay danh sach nhom san pham");
            return PageResponse.from(p);
        });
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        return call("tao nhom san pham", () -> {
            Map<String, Object> body = Map.of(
                    "code", request.getCode(),
                    "name", request.getName(),
                    "description", request.getDescription(),
                    "partnerConfigs", List.of(Map.of("partnerCode", PARTNER_CODE, "isActive", true)));
            return unwrap(client().post().uri("/api/v1/product-categories")
                    .header("Authorization", bearer())
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<AdminPortalEnvelope<CategoryResponse>>() {
                    }), "tao nhom san pham");
        });
    }

    // ---------- Product ----------

    public PageResponse<ProductResponse> listProducts(String categoryCode, int page, int size) {
        return call("lay danh sach san pham", () -> {
            UriComponentsBuilder b = UriComponentsBuilder.fromPath("/api/v1/products")
                    .queryParam("partnerCode", PARTNER_CODE)
                    .queryParam("page", page)
                    .queryParam("size", size);
            if (categoryCode != null && !categoryCode.isBlank()) {
                b.queryParam("categoryCode", categoryCode);
            }
            AdminPortalPage<ProductResponse> p = unwrap(client().get().uri(b.build().toUriString())
                    .header("Authorization", bearer())
                    .retrieve()
                    .body(new ParameterizedTypeReference<AdminPortalEnvelope<AdminPortalPage<ProductResponse>>>() {
                    }), "lay danh sach san pham");
            return PageResponse.from(p);
        });
    }

    public ProductResponse getProduct(String id) {
        return call("lay chi tiet san pham", () -> unwrap(client().get()
                .uri("/api/v1/products/{id}", id)
                .header("Authorization", bearer())
                .retrieve()
                .body(new ParameterizedTypeReference<AdminPortalEnvelope<ProductResponse>>() {
                }), "lay chi tiet san pham"));
    }

    public ProductResponse createProduct(ProductRequest request) {
        return call("tao san pham", () -> unwrap(client().post().uri("/api/v1/products")
                .header("Authorization", bearer())
                .body(toProductBody(request, null))
                .retrieve()
                .body(new ParameterizedTypeReference<AdminPortalEnvelope<ProductResponse>>() {
                }), "tao san pham"));
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        return call("sua san pham", () -> unwrap(client().put().uri("/api/v1/products/{id}", id)
                .header("Authorization", bearer())
                .body(toProductBody(request, id))
                .retrieve()
                .body(new ParameterizedTypeReference<AdminPortalEnvelope<ProductResponse>>() {
                }), "sua san pham"));
    }

    public void deleteProduct(String id) {
        call("xoa san pham", () -> {
            unwrap(client().method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/api/v1/products/{id}", id)
                    .header("Authorization", bearer())
                    .body(Map.of("id", id, "partnerCodes", List.of(PARTNER_CODE)))
                    .retrieve()
                    .body(new ParameterizedTypeReference<AdminPortalEnvelope<Map<String, Object>>>() {
                    }), "xoa san pham");
            return null;
        });
    }

    public void setProductInvisible(String id, boolean invisible) {
        call("doi hien thi san pham", () -> {
            Map<String, Object> body = Map.of("invisible", invisible, "partnerCodes", List.of(PARTNER_CODE));
            unwrap(client().patch().uri("/api/v1/products/{id}/active", id)
                    .header("Authorization", bearer())
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<AdminPortalEnvelope<Map<String, Object>>>() {
                    }), "doi hien thi san pham");
            return null;
        });
    }

    private Map<String, Object> toProductBody(ProductRequest r, String idForUpdate) {
        // exchangeRateId/exchangeRateCode/serviceCode luon null: san pham tao tu kiosk-admin
        // la san pham quan ly thu cong, khong co nguon ty gia/dich vu tu dong (xem ProductRequest).
        Map<String, Object> body = new java.util.HashMap<>();
        if (idForUpdate != null) {
            body.put("id", idForUpdate);
        }
        body.put("code", r.getCode());
        body.put("name", r.getName());
        body.put("categoryId", r.getCategoryId());
        body.put("quantity", r.getQuantity());
        body.put("description", r.getDescription());
        body.put("invisible", r.getInvisible() != null ? r.getInvisible() : Boolean.TRUE);
        body.put("exchangeRateId", null);
        body.put("exchangeRateCode", null);
        body.put("serviceCode", null);
        body.put("partnerConfigs", List.of(Map.of("partnerCode", PARTNER_CODE, "isActive", true)));
        return body;
    }

    // ---------- Inventory ----------

    public PageResponse<InventoryResponse> listInventory(String productName, int page, int size) {
        return call("lay danh sach ton kho", () -> {
            UriComponentsBuilder b = UriComponentsBuilder.fromPath("/api/v1/inventories")
                    .queryParam("partnerCode", PARTNER_CODE)
                    .queryParam("page", page)
                    .queryParam("size", size);
            if (productName != null && !productName.isBlank()) {
                b.queryParam("productName", productName);
            }
            AdminPortalPage<InventoryResponse> p = unwrap(client().get().uri(b.build().toUriString())
                    .header("Authorization", bearer())
                    .retrieve()
                    .body(new ParameterizedTypeReference<AdminPortalEnvelope<AdminPortalPage<InventoryResponse>>>() {
                    }), "lay danh sach ton kho");
            return PageResponse.from(p);
        });
    }

    public void updateInventory(String inventoryId, int quantity) {
        call("cap nhat dinh muc ton kho", () -> {
            unwrap(client().put().uri("/api/v1/inventories/{id}", inventoryId)
                    .header("Authorization", bearer())
                    .body(Map.of("quantity", quantity))
                    .retrieve()
                    .body(new ParameterizedTypeReference<AdminPortalEnvelope<Map<String, Object>>>() {
                    }), "cap nhat dinh muc ton kho");
            return null;
        });
    }

    public PageResponse<InventoryHistoryResponse> inventoryHistory(String inventoryId, int page, int size) {
        return call("lay lich su ton kho", () -> {
            String uri = UriComponentsBuilder.fromPath("/api/v1/inventories/{inventoryId}/history")
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .buildAndExpand(inventoryId)
                    .toUriString();
            AdminPortalPage<InventoryHistoryResponse> p = unwrap(client().get().uri(uri)
                    .header("Authorization", bearer())
                    .retrieve()
                    .body(new ParameterizedTypeReference<AdminPortalEnvelope<AdminPortalPage<InventoryHistoryResponse>>>() {
                    }), "lay lich su ton kho");
            return PageResponse.from(p);
        });
    }

    // ---------- Helpers ----------

    // ---------- Goi tong quat (cua hang, han muc, sua nhom) ----------

    /**
     * Goi 1 API noi bo cua admin-portal va tra ve {@code data} dang JsonNode. Khac call/unwrap o duoi:
     * loi 4xx/status != SYS-200 tra NGUYEN thong diep cua admin-portal (kem fieldErrors) de nguoi dung
     * biet sai gi, thay vi cau chung "HTTP 400".
     */
    public JsonNode raw(HttpMethod method, String uri, Object body, String action) {
        try {
            RestClient.RequestBodySpec spec = client().method(method).uri(uri).header("Authorization", bearer());
            if (body != null) {
                spec.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            String text = spec.retrieve().body(String.class);
            JsonNode node = text == null || text.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(text);
            String status = node.path("status").asText(null);
            if (status != null && !AdminPortalEnvelope.STATUS_SUCCESS.equals(status)) {
                throw new BusinessException(messageOf(node, action), "400", 400);
            }
            return node.path("data");
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("admin-portal tra HTTP {} khi {}: {}", e.getStatusCode(), action, e.getResponseBodyAsString());
            int code = e.getStatusCode().value();
            String message;
            try {
                message = messageOf(objectMapper.readTree(e.getResponseBodyAsString()), action);
            } catch (java.io.IOException parse) {
                message = "admin-portal tra loi HTTP " + code + " khi " + action;
            }
            int out = code >= 400 && code < 500 ? code : 502;
            throw new BusinessException(message, String.valueOf(out), out);
        } catch (RestClientException e) {
            throw new BusinessException("Khong ket noi duoc toi admin-portal khi " + action + ": " + e.getMessage(), "502", 502);
        } catch (java.io.IOException e) {
            throw new BusinessException("Phan hoi admin-portal khong hop le khi " + action, "502", 502);
        }
    }

    private static String messageOf(JsonNode node, String action) {
        String msg = node.path("message").asText("loi khong xac dinh");
        JsonNode fieldErrors = node.path("fieldErrors");
        if (fieldErrors.isArray() && !fieldErrors.isEmpty()) {
            StringBuilder sb = new StringBuilder(msg).append(": ");
            for (int i = 0; i < fieldErrors.size(); i++) {
                JsonNode fe = fieldErrors.get(i);
                if (i > 0) {
                    sb.append("; ");
                }
                sb.append(fe.path("field").asText()).append(" - ").append(fe.path("message").asText());
            }
            msg = sb.toString();
        }
        return "Loi khi " + action + ": " + msg;
    }

    private String bearer() {
        return "Bearer " + authClient.getToken();
    }

    private <T> T unwrap(AdminPortalEnvelope<T> envelope, String action) {
        if (envelope == null) {
            throw new BusinessException("Khong co phan hoi tu admin-portal khi " + action, "502", 502);
        }
        if (!AdminPortalEnvelope.STATUS_SUCCESS.equals(envelope.getStatus())) {
            String msg = envelope.getMessage() != null ? envelope.getMessage() : "loi khong xac dinh";
            throw new BusinessException(
                    "admin-portal tra loi (" + envelope.getStatus() + ") khi " + action + ": " + msg, "502", 502);
        }
        return envelope.getData();
    }

    private <T> T call(String action, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            // admin-portal tra ve HTTP loi (vi du 400 validate) - log than body de truy vet, chi
            // dua thong tin can thiet ve client cua app nay.
            log.warn("admin-portal tra HTTP {} khi {}: {}", e.getStatusCode(), action, e.getResponseBodyAsString());
            throw new BusinessException(
                    "admin-portal tra loi HTTP " + e.getStatusCode().value() + " khi " + action, "502", 502);
        } catch (RestClientException e) {
            throw new BusinessException(
                    "Khong ket noi duoc toi admin-portal khi " + action + ": " + e.getMessage(), "502", 502);
        }
    }
}
