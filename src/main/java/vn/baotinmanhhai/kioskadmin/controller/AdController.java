package vn.baotinmanhhai.kioskadmin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.baotinmanhhai.kioskadmin.dto.AdRequest;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.entity.KioskAd;
import vn.baotinmanhhai.kioskadmin.service.KioskAdService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ads")
@RequiredArgsConstructor
public class AdController {

    private final KioskAdService kioskAdService;

    @GetMapping
    public ApiResponse<List<KioskAd>> list() {
        return ApiResponse.ok(kioskAdService.list());
    }

    @PostMapping
    public ApiResponse<KioskAd> create(@Valid @RequestBody AdRequest request) {
        return ApiResponse.ok(kioskAdService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<KioskAd> update(@PathVariable String id, @Valid @RequestBody AdRequest request) {
        return ApiResponse.ok(kioskAdService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        kioskAdService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/toggle")
    public ApiResponse<KioskAd> toggle(@PathVariable String id) {
        return ApiResponse.ok(kioskAdService.toggle(id));
    }
}
