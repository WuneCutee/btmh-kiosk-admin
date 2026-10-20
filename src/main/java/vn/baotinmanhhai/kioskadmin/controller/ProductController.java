package vn.baotinmanhhai.kioskadmin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.dto.PageResponse;
import vn.baotinmanhhai.kioskadmin.dto.ProductRequest;
import vn.baotinmanhhai.kioskadmin.dto.ProductResponse;
import vn.baotinmanhhai.kioskadmin.filter.JwtAuthFilter;
import vn.baotinmanhhai.kioskadmin.service.AuditLogService;
import vn.baotinmanhhai.kioskadmin.service.ProductService;

/**
 * San pham - tu CRUD truc tiep vao bang "product" (da go bo) chuyen thanh proxy sang
 * admin-portal (xem ProductService / AdminPortalClient / README). Giu nguyen URL
 * /api/admin/products va cac ten hanh dong audit log (PRODUCT_CREATE/UPDATE/DELETE/
 * TOGGLE_VISIBILITY) de lien tuc voi log cu.
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> list(
            @RequestParam(required = false) String categoryCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ApiResponse.ok(productService.list(categoryCode, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getOne(@PathVariable String id) {
        return ApiResponse.ok(productService.get(id));
    }

    @PostMapping
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest request,
                                                @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                                HttpServletRequest httpRequest) {
        ProductResponse product = productService.create(request);
        auditLogService.record(actor, "PRODUCT_CREATE", "Product", product.getId(), product.getName(), httpRequest);
        return ApiResponse.ok(product);
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable String id, @Valid @RequestBody ProductRequest request,
                                                @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                                HttpServletRequest httpRequest) {
        ProductResponse product = productService.update(id, request);
        auditLogService.record(actor, "PRODUCT_UPDATE", "Product", id, product.getName(), httpRequest);
        return ApiResponse.ok(product);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                     @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                     HttpServletRequest httpRequest) {
        productService.delete(id);
        auditLogService.record(actor, "PRODUCT_DELETE", "Product", id, null, httpRequest);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/toggle-visibility")
    public ApiResponse<ProductResponse> toggleVisibility(@PathVariable String id,
                                                           @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                                           HttpServletRequest httpRequest) {
        ProductResponse product = productService.toggleVisibility(id);
        auditLogService.record(actor, "PRODUCT_TOGGLE_VISIBILITY", "Product", id,
                "invisible=" + product.getInvisible(), httpRequest);
        return ApiResponse.ok(product);
    }
}
