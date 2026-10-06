package vn.baotinmanhhai.kioskadmin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.baotinmanhhai.kioskadmin.dto.ActivationCodeResponse;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.dto.DeviceResponse;
import vn.baotinmanhhai.kioskadmin.dto.RegisterDeviceRequest;
import vn.baotinmanhhai.kioskadmin.service.KioskDeviceService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final KioskDeviceService kioskDeviceService;

    @GetMapping
    public ApiResponse<List<DeviceResponse>> list() {
        return ApiResponse.ok(kioskDeviceService.listAll());
    }

    @GetMapping("/{deviceCode}")
    public ApiResponse<DeviceResponse> getOne(@PathVariable String deviceCode) {
        return ApiResponse.ok(kioskDeviceService.getByCode(deviceCode));
    }

    @PostMapping
    public ApiResponse<ActivationCodeResponse> create(@Valid @RequestBody RegisterDeviceRequest request) {
        return ApiResponse.ok(kioskDeviceService.register(request));
    }

    @PostMapping("/{deviceCode}/activation-code")
    public ApiResponse<ActivationCodeResponse> reissue(@PathVariable String deviceCode) {
        return ApiResponse.ok(kioskDeviceService.reissueActivationCode(deviceCode));
    }

    @PostMapping("/{deviceCode}/revoke")
    public ApiResponse<DeviceResponse> revoke(@PathVariable String deviceCode) {
        return ApiResponse.ok(kioskDeviceService.revoke(deviceCode));
    }

    @PostMapping("/{deviceCode}/force-key-rotation")
    public ApiResponse<DeviceResponse> forceKeyRotation(@PathVariable String deviceCode) {
        return ApiResponse.ok(kioskDeviceService.forceKeyRotation(deviceCode));
    }

    @DeleteMapping("/{deviceCode}")
    public ApiResponse<Void> delete(@PathVariable String deviceCode) {
        kioskDeviceService.delete(deviceCode);
        return ApiResponse.ok(null);
    }
}
