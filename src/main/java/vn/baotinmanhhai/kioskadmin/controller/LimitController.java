package vn.baotinmanhhai.kioskadmin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.exception.BusinessException;
import vn.baotinmanhhai.kioskadmin.filter.JwtAuthFilter;
import vn.baotinmanhhai.kioskadmin.service.AdminPortalClient;
import vn.baotinmanhhai.kioskadmin.service.AuditLogService;

import java.math.BigDecimal;
import java.util.Map;

/** Han muc giao dich (so chi toi da/giao dich) cua kenh KIOSK - nam o admin-portal (bang "limit"). */
@RestController
@RequestMapping("/api/admin/limits")
@RequiredArgsConstructor
public class LimitController {

    private final AdminPortalClient adminPortal;
    private final AuditLogService auditLogService;

    @GetMapping
    public ApiResponse<JsonNode> list() {
        return ApiResponse.ok(adminPortal.raw(HttpMethod.GET, "/api/v1/limits?partnerCode=KIOSK&page=0&size=50", null,
                "lay han muc"));
    }

    /** Body {limitQuantity: so chi > 0}. */
    @PatchMapping("/{id}/quantity")
    public ApiResponse<JsonNode> setQuantity(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                             @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                             HttpServletRequest httpRequest) {
        BigDecimal quantity;
        try {
            quantity = new BigDecimal(String.valueOf(body.get("limitQuantity")));
        } catch (NumberFormatException e) {
            throw new BusinessException("Han muc phai la so", "400", 400);
        }
        if (quantity.signum() <= 0) {
            throw new BusinessException("Han muc phai lon hon 0", "400", 400);
        }
        JsonNode limit = adminPortal.raw(HttpMethod.PATCH, "/api/v1/limits/" + id + "/quantity",
                Map.of("limitQuantity", quantity), "sua han muc");
        auditLogService.record(actor, "LIMIT_QUANTITY", "Limit", String.valueOf(id), "limitQuantity=" + quantity, httpRequest);
        return ApiResponse.ok(limit);
    }

    /** Body {active: true|false}. */
    @PatchMapping("/{id}/active")
    public ApiResponse<JsonNode> setActive(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                           @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                           HttpServletRequest httpRequest) {
        Object active = body.get("active");
        if (!(active instanceof Boolean)) {
            throw new BusinessException("Thieu truong active (true/false)", "400", 400);
        }
        JsonNode limit = adminPortal.raw(HttpMethod.PATCH, "/api/v1/limits/" + id + "/active",
                Map.of("isActive", active), "bat/tat han muc");
        auditLogService.record(actor, "LIMIT_ACTIVE", "Limit", String.valueOf(id), "active=" + active, httpRequest);
        return ApiResponse.ok(limit);
    }
}
