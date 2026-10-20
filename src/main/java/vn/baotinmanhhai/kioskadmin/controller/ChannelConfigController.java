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
 * Cau hinh kenh KIOSK (gio mo ban, hotline, thoi gian cho, phien ban App toi thieu, tam dung kenh) -
 * kiosk-api ban SSE "config-changed" sau khi luu nen App tai lai bootstrap ngay.
 */
@RestController
@RequestMapping("/api/admin/channel-config")
@RequiredArgsConstructor
public class ChannelConfigController {

    private static final String BASE = "/internal/ops/channel-config";

    private final KioskApiAdminClient kioskApi;
    private final AuditLogService auditLogService;

    @GetMapping
    public ApiResponse<JsonNode> get() {
        return ApiResponse.ok(kioskApi.get(BASE));
    }

    @PutMapping
    public ApiResponse<JsonNode> update(@RequestBody JsonNode request,
                                        @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                        HttpServletRequest httpRequest) {
        JsonNode saved = kioskApi.put(BASE, request);
        auditLogService.record(actor, "CHANNEL_CONFIG_UPDATE", "KioskChannelConfig", "1",
                "paused=" + saved.path("channelPaused").asText() + ", hours=" + saved.path("openTime").asText()
                        + "-" + saved.path("closeTime").asText(), httpRequest);
        return ApiResponse.ok(saved);
    }
}
