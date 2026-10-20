package vn.baotinmanhhai.kioskadmin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.filter.JwtAuthFilter;
import vn.baotinmanhhai.kioskadmin.service.AuditLogService;
import vn.baotinmanhhai.kioskadmin.service.KioskApiAdminClient;

/**
 * Quang cao man cho - du lieu that cua App nam o kiosk-api (bang kiosk_ad cua DB kiosk_api), KHONG phai
 * bang cung ten cua webview nhu truoc. kiosk-api tu tai media de tinh checksum/kich thuoc va ban SSE
 * "playlist-changed" sau moi lan ghi, App doi quang cao ngay.
 */
@RestController
@RequestMapping("/api/admin/ads")
@RequiredArgsConstructor
public class AdController {

    private static final String BASE = "/internal/ops/ads";

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
        JsonNode ad = kioskApi.post(BASE, request);
        auditLogService.record(actor, "AD_CREATE", "KioskAd", ad.path("id").asText(), ad.path("name").asText(), httpRequest);
        return ApiResponse.ok(ad);
    }

    @PutMapping("/{id}")
    public ApiResponse<JsonNode> update(@PathVariable String id, @RequestBody JsonNode request,
                                        @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                        HttpServletRequest httpRequest) {
        JsonNode ad = kioskApi.put(BASE + "/" + id, request);
        auditLogService.record(actor, "AD_UPDATE", "KioskAd", id, ad.path("name").asText(), httpRequest);
        return ApiResponse.ok(ad);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                    HttpServletRequest httpRequest) {
        kioskApi.delete(BASE + "/" + id);
        auditLogService.record(actor, "AD_DELETE", "KioskAd", id, null, httpRequest);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/toggle")
    public ApiResponse<JsonNode> toggle(@PathVariable String id,
                                        @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                        HttpServletRequest httpRequest) {
        JsonNode ad = kioskApi.post(BASE + "/" + id + "/toggle", null);
        auditLogService.record(actor, "AD_TOGGLE", "KioskAd", id, "enabled=" + ad.path("enabled").asText(), httpRequest);
        return ApiResponse.ok(ad);
    }
}
