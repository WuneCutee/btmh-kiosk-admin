package vn.baotinmanhhai.kioskadmin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.dto.InventoryHistoryResponse;
import vn.baotinmanhhai.kioskadmin.dto.InventoryResponse;
import vn.baotinmanhhai.kioskadmin.dto.InventoryUpdateRequest;
import vn.baotinmanhhai.kioskadmin.dto.PageResponse;
import vn.baotinmanhhai.kioskadmin.filter.JwtAuthFilter;
import vn.baotinmanhhai.kioskadmin.service.AuditLogService;
import vn.baotinmanhhai.kioskadmin.service.InventoryService;

/**
 * Ton kho (moi) - proxy sang admin-portal (xem InventoryService). "quantity" trong
 * InventoryUpdateRequest la DINH MUC trong ngay, KHONG phai so con lai - xem help text trong UI.
 */
@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ApiResponse<PageResponse<InventoryResponse>> list(
            @RequestParam(required = false) String productName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ApiResponse.ok(inventoryService.list(productName, page, size));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable String id, @Valid @RequestBody InventoryUpdateRequest request,
                                     @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                     HttpServletRequest httpRequest) {
        inventoryService.update(id, request.getQuantity());
        auditLogService.record(actor, "INVENTORY_UPDATE", "Inventory", id, "quantity=" + request.getQuantity(),
                httpRequest);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/history")
    public ApiResponse<PageResponse<InventoryHistoryResponse>> history(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(inventoryService.history(id, page, size));
    }
}
