package vn.baotinmanhhai.kioskadmin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Body cho PUT /api/admin/prices/{productCode}. Day la TRUONG HOP NGOAI LE DUY NHAT ma kiosk-admin
 * ghi truc tiep vao bang "product_prices" (khong qua admin-portal) - xem README phan "Kien truc/
 * quyet dinh ky thuat" de biet ly do (he thong thuc KHONG co API chinh sua gia danh cho nguoi,
 * gia chi den tu feed ty gia BTMH).
 */
@Data
public class PriceRequest {
    @NotNull(message = "Vui long nhap gia ban ra")
    @DecimalMin(value = "0", message = "Gia ban ra khong duoc am")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "Gia mua vao khong duoc am")
    private BigDecimal buyPrice;

    private String description;
}
