package vn.baotinmanhhai.kioskadmin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.service.KioskApiAdminClient;

/** Chi doc - log loi App gui len kiosk-api (II.2.13). */
@RestController
@RequestMapping("/api/admin/device-error-logs")
@RequiredArgsConstructor
public class DeviceErrorLogController {

    private final KioskApiAdminClient kioskApi;

    @GetMapping
    public ApiResponse<JsonNode> list(
            @RequestParam(required = false) String deviceCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/internal/ops/device-error-logs")
                .queryParam("page", page)
                .queryParam("size", size);
        if (deviceCode != null && !deviceCode.isBlank()) {
            uri.queryParam("deviceCode", deviceCode.trim());
        }
        return ApiResponse.ok(kioskApi.get(uri.encode().build().toUriString()));
    }
}
