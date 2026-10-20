package vn.baotinmanhhai.kioskadmin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.filter.JwtAuthFilter;
import vn.baotinmanhhai.kioskadmin.service.AuditLogService;
import vn.baotinmanhhai.kioskadmin.service.KioskApiAdminClient;

/** Banner tren man danh sach san pham cua App (bang kiosk_banner cua kiosk-api). */
@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class BannerController {

    private static final String BASE = "/internal/ops/banners";

    private final KioskApiAdminClient kioskApi;
    private final AuditLogService auditLogService;

    @GetMapping
    public ApiResponse<JsonNode> list() {
        return ApiResponse.ok(kioskApi.get(BASE));
    }

    @PostMapping
    public ApiResponse<JsonNode> create(@RequestBody JsonNode request,
                                        @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                        HttpServletRequest httpRequest) {
        JsonNode banner = kioskApi.post(BASE, request);
        auditLogService.record(actor, "BANNER_CREATE", "KioskBanner", banner.path("id").asText(), banner.path("name").asText(), httpRequest);
        return ApiResponse.ok(banner);
    }

    @PutMapping("/{id}")
    public ApiResponse<JsonNode> update(@PathVariable String id, @RequestBody JsonNode request,
                                        @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                        HttpServletRequest httpRequest) {
        JsonNode banner = kioskApi.put(BASE + "/" + id, request);
        auditLogService.record(actor, "BANNER_UPDATE", "KioskBanner", id, banner.path("name").asText(), httpRequest);
        return ApiResponse.ok(banner);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                    HttpServletRequest httpRequest) {
        kioskApi.delete(BASE + "/" + id);
        auditLogService.record(actor, "BANNER_DELETE", "KioskBanner", id, null, httpRequest);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/toggle")
    public ApiResponse<JsonNode> toggle(@PathVariable String id,
                                        @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                        HttpServletRequest httpRequest) {
        JsonNode banner = kioskApi.post(BASE + "/" + id + "/toggle", null);
        auditLogService.record(actor, "BANNER_TOGGLE", "KioskBanner", id, "enabled=" + banner.path("enabled").asText(), httpRequest);
        return ApiResponse.ok(banner);
    }
}
