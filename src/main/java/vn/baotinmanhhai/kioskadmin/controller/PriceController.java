package vn.baotinmanhhai.kioskadmin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.dto.PriceHistoryResponse;
import vn.baotinmanhhai.kioskadmin.dto.PriceRequest;
import vn.baotinmanhhai.kioskadmin.dto.PriceResponse;
import vn.baotinmanhhai.kioskadmin.filter.JwtAuthFilter;
import vn.baotinmanhhai.kioskadmin.service.AuditLogService;
import vn.baotinmanhhai.kioskadmin.service.ProductPriceService;

/**
 * Sua gia THU CONG - xem ProductPriceService / README de biet vi sao day la ngoai le DUY NHAT
 * ghi truc tiep vao DB chung (product_prices) thay vi proxy qua admin-portal.
 */
@RestController
@RequestMapping("/api/admin/prices")
@RequiredArgsConstructor
public class PriceController {

    private final ProductPriceService productPriceService;
    private final AuditLogService auditLogService;

    @GetMapping("/{productCode}")
    public ApiResponse<PriceResponse> getCurrent(@PathVariable String productCode) {
        return ApiResponse.ok(productPriceService.getCurrent(productCode));
    }

    @PutMapping("/{productCode}")
    public ApiResponse<PriceResponse> setPrice(@PathVariable String productCode, @Valid @RequestBody PriceRequest request,
                                                @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                                HttpServletRequest httpRequest) {
        PriceResponse price = productPriceService.setPrice(
                productCode, request.getPrice(), request.getBuyPrice(), request.getDescription(), actor);
        auditLogService.record(actor, "PRICE_UPDATE", "ProductPrice", productCode, "price=" + request.getPrice(),
                httpRequest);
        return ApiResponse.ok(price);
    }

    @GetMapping("/{productCode}/history")
    public ApiResponse<Page<PriceHistoryResponse>> history(@PathVariable String productCode,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(productPriceService.history(productCode, page, size));
    }
}
