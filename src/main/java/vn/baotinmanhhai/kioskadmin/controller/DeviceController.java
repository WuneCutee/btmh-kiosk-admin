package vn.baotinmanhhai.kioskadmin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.filter.JwtAuthFilter;
import vn.baotinmanhhai.kioskadmin.service.AuditLogService;
import vn.baotinmanhhai.kioskadmin.service.KioskApiAdminClient;

/**
 * May kiosk - kiosk-api la noi App kich hoat/lay token/giu ket noi SSE, nen tao may, cap ma kich hoat,
 * thu hoi, ep xoay khoa va trang thai online deu doc/ghi o kiosk-api (truoc day ghi nham vao DB webview).
 */
@RestController
@RequestMapping("/api/admin/devices")
@RequiredArgsConstructor
public class DeviceController {

    private static final String BASE = "/internal/ops/devices";

    private final KioskApiAdminClient kioskApi;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ApiResponse<JsonNode> list() {
        return ApiResponse.ok(kioskApi.get(BASE));
    }

    @GetMapping("/{deviceCode}")
    public ApiResponse<JsonNode> getOne(@PathVariable String deviceCode) {
        return ApiResponse.ok(kioskApi.get(BASE + "/" + deviceCode));
    }

    /** Body {deviceCode, storeId, storeName}; tra ma kich hoat (chi hien 1 lan) kem qrPayload cho App quet. */
    @PostMapping
    public ApiResponse<JsonNode> create(@RequestBody JsonNode request,
                                        @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                        HttpServletRequest httpRequest) {
        JsonNode issued = withQrPayload(kioskApi.post(BASE, request));
        auditLogService.record(actor, "DEVICE_CREATE", "KioskDevice", issued.path("deviceCode").asText(), null, httpRequest);
        return ApiResponse.ok(issued);
    }

    /** Body {storeId, storeName} - cua hang dat may (bootstrap tra ve cho App). */
    @PutMapping("/{deviceCode}")
    public ApiResponse<JsonNode> update(@PathVariable String deviceCode, @RequestBody JsonNode request,
                                        @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                        HttpServletRequest httpRequest) {
        JsonNode device = kioskApi.put(BASE + "/" + deviceCode, request);
        auditLogService.record(actor, "DEVICE_UPDATE", "KioskDevice", deviceCode,
                "store=" + device.path("storeId").asText() + " " + device.path("storeName").asText(), httpRequest);
        return ApiResponse.ok(device);
    }

    @PostMapping("/{deviceCode}/activation-code")
    public ApiResponse<JsonNode> reissue(@PathVariable String deviceCode,
                                         @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                         HttpServletRequest httpRequest) {
        JsonNode issued = withQrPayload(kioskApi.post(BASE + "/" + deviceCode + "/activation-code", null));
        auditLogService.record(actor, "DEVICE_REISSUE_ACTIVATION_CODE", "KioskDevice", deviceCode, null, httpRequest);
        return ApiResponse.ok(issued);
    }

    @PostMapping("/{deviceCode}/revoke")
    public ApiResponse<JsonNode> revoke(@PathVariable String deviceCode,
                                        @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                        HttpServletRequest httpRequest) {
        kioskApi.post(BASE + "/" + deviceCode + "/revoke", null);
        auditLogService.record(actor, "DEVICE_REVOKE", "KioskDevice", deviceCode, null, httpRequest);
        return ApiResponse.ok(kioskApi.get(BASE + "/" + deviceCode));
    }

    @PostMapping("/{deviceCode}/force-key-rotation")
    public ApiResponse<JsonNode> forceKeyRotation(@PathVariable String deviceCode,
                                                  @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                                  HttpServletRequest httpRequest) {
        JsonNode device = kioskApi.post(BASE + "/" + deviceCode + "/force-key-rotation", null);
        auditLogService.record(actor, "DEVICE_ROTATE_KEY", "KioskDevice", deviceCode, null, httpRequest);
        return ApiResponse.ok(device);
    }

    @DeleteMapping("/{deviceCode}")
    public ApiResponse<Void> delete(@PathVariable String deviceCode,
                                    @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                    HttpServletRequest httpRequest) {
        kioskApi.delete(BASE + "/" + deviceCode);
        auditLogService.record(actor, "DEVICE_DELETE", "KioskDevice", deviceCode, null, httpRequest);
        return ApiResponse.ok(null);
    }

    /** QR App quet o man Kich hoat: JSON {deviceCode, activationCode}. */
    private JsonNode withQrPayload(JsonNode issued) {
        if (!(issued instanceof ObjectNode node)) {
            return issued;
        }
        ObjectNode qr = objectMapper.createObjectNode();
        qr.put("deviceCode", node.path("deviceCode").asText());
        qr.put("activationCode", node.path("activationCode").asText());
        node.put("qrPayload", qr.toString());
        return node;
    }
}
