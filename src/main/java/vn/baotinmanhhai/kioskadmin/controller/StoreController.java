package vn.baotinmanhhai.kioskadmin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.exception.BusinessException;
import vn.baotinmanhhai.kioskadmin.filter.JwtAuthFilter;
import vn.baotinmanhhai.kioskadmin.service.AdminPortalClient;
import vn.baotinmanhhai.kioskadmin.service.AuditLogService;

import java.util.List;
import java.util.Map;

/**
 * Cua hang nhan hang (admin-portal) - chi bat/tat THEO KENH KIOSK (UpdateStoreActiveHandler chi ghi
 * partner_store_config cua kenh do, khong anh huong BIDV/MB). Sua ten/dia chi can danh muc phuong/xa
 * hop le ben admin-portal nen lam tai admin-portal.
 */
@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
public class StoreController {

    private static final String KIOSK = "KIOSK";

    private final AdminPortalClient adminPortal;
    private final AuditLogService auditLogService;

    @GetMapping
    public ApiResponse<JsonNode> list(@RequestParam(required = false) String name,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "200") int size) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/api/v1/stores")
                .queryParam("partnerCode", KIOSK)
                .queryParam("page", Math.max(page, 0))
                .queryParam("size", Math.min(Math.max(size, 1), 500))
                .queryParam("sort", "id");
        if (name != null && !name.isBlank()) {
            uri.queryParam("name", name.trim());
        }
        return ApiResponse.ok(adminPortal.raw(HttpMethod.GET, uri.encode().build().toUriString(), null,
                "lay danh sach cua hang"));
    }

    @GetMapping("/provinces")
    public ApiResponse<JsonNode> provinces() {
        return ApiResponse.ok(adminPortal.raw(HttpMethod.GET, "/api/v1/stores/provinces", null, "lay danh sach tinh/thanh"));
    }

    /** Body {active: true|false}. */
    @PatchMapping("/{id}/active")
    public ApiResponse<JsonNode> setActive(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                           @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                           HttpServletRequest httpRequest) {
        Object active = body.get("active");
        if (!(active instanceof Boolean)) {
            throw new BusinessException("Thieu truong active (true/false)", "400", 400);
        }
        JsonNode store = adminPortal.raw(HttpMethod.PATCH, "/api/v1/stores/" + id + "/active",
                Map.of("isActive", active, "partnerCodes", List.of(KIOSK)), "bat/tat cua hang");
        auditLogService.record(actor, "STORE_KIOSK_ACTIVE", "Store", String.valueOf(id), "active=" + active, httpRequest);
        return ApiResponse.ok(store);
    }
}
