package vn.baotinmanhhai.kioskadmin.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import vn.baotinmanhhai.kioskadmin.dto.CategoryRequest;
import vn.baotinmanhhai.kioskadmin.dto.CategoryResponse;
import vn.baotinmanhhai.kioskadmin.dto.CategoryUpdateRequest;
import vn.baotinmanhhai.kioskadmin.dto.PageResponse;
import vn.baotinmanhhai.kioskadmin.exception.BusinessException;

import java.util.List;
import java.util.Map;

/**
 * Nhom san pham (category) - proxy sang admin-portal (/api/v1/product-categories, kenh KIOSK) qua
 * AdminPortalClient, vi category "thuc" ma App doc la category co partner_category_config KIOSK ben do.
 * Thu tu (priority) la cua nhom, dung chung moi kenh: admin-portal chua cho dat display_order rieng
 * theo kenh qua API.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final int DEFAULT_SIZE = 100;
    private static final String KIOSK = "KIOSK";
    private static final String BASE = "/api/v1/product-categories";

    private final AdminPortalClient adminPortalClient;

    public PageResponse<CategoryResponse> list(int page, int size) {
        return adminPortalClient.listCategories(page, size <= 0 ? DEFAULT_SIZE : size);
    }

    public CategoryResponse create(CategoryRequest request) {
        return adminPortalClient.createCategory(request);
    }

    /** Sua ten/ma/mo ta; giu nguyen trang thai hien/an tren kenh KIOSK. */
    public JsonNode update(String id, CategoryUpdateRequest request) {
        JsonNode current = adminPortalClient.raw(HttpMethod.GET, BASE + "/" + id, null, "lay nhom san pham");
        boolean kioskActive = false;
        for (JsonNode cfg : current.path("partnerConfigs")) {
            if (KIOSK.equals(cfg.path("partnerCode").asText())) {
                kioskActive = cfg.path("isActive").asBoolean(false);
            }
        }
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("code", request.getCode());
        body.put("name", request.getName());
        body.put("description", request.getDescription());
        body.put("partnerConfigs", List.of(Map.of("partnerCode", KIOSK, "isActive", kioskActive)));
        return adminPortalClient.raw(HttpMethod.PUT, BASE + "/" + id, body, "sua nhom san pham");
    }

    /** Hien/an nhom tren App Kiosk (chi config KIOSK, kenh khac khong doi). */
    public JsonNode setVisible(String id, boolean visible) {
        return adminPortalClient.raw(HttpMethod.DELETE, BASE + "/" + id + "/invisible",
                Map.of("inVisible", !visible, "partnerCodes", List.of(KIOSK)), "hien/an nhom san pham");
    }

    /** Body [{categoryId, priority}] - thu tu tab tren App (so nho dung truoc). */
    public JsonNode updatePriorities(List<Map<String, Object>> priorities) {
        if (priorities == null || priorities.isEmpty()) {
            throw new BusinessException("Danh sach thu tu rong", "400", 400);
        }
        return adminPortalClient.raw(HttpMethod.PUT, BASE + "/priority", Map.of("categoryPriorities", priorities),
                "sap xep nhom san pham");
    }

    /** Go nhom khoi kenh KIOSK (admin-portal tu chan neu nhom con san pham tren kenh nay). */
    public void deleteFromKiosk(String id) {
        adminPortalClient.raw(HttpMethod.DELETE, BASE + "/" + id, Map.of("partnerCodes", List.of(KIOSK)), "xoa nhom san pham");
    }
}
