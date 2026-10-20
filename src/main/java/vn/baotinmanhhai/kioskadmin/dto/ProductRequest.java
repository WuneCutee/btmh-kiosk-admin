package vn.baotinmanhhai.kioskadmin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request tao/sua san pham - proxy sang POST|PUT /api/v1/products ben admin-portal. Thay cho
 * ProductRequest cu (anh xa thang vao bang "product" - da go bo, xem README). Rang buoc khop
 * CreateProductRequest/UpdateProductRequest ben admin-portal. KHONG co exchangeRateId/
 * exchangeRateCode/serviceCode: san pham tao tu man nay la san pham QUAN LY THU CONG (khong co
 * nguon gia/ty gia tu dong), AdminPortalClient luon gui null cho 3 truong nay - serviceCode neu
 * dien bat ky gia tri nao se bi admin-portal tu choi voi loi "Khong ton tai ServiceID" vi phai
 * tro toi 1 service-code co san.
 */
@Data
public class ProductRequest {
    @NotBlank(message = "Vui long nhap ma san pham")
    @Size(max = 30, message = "Ma san pham khong duoc vuot qua 30 ky tu")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Ma san pham chi duoc chua chu cai, chu so, '_' hoac '-'")
    private String code;

    @NotBlank(message = "Vui long nhap ten san pham")
    @Size(max = 100, message = "Ten san pham khong duoc vuot qua 100 ky tu")
    private String name;

    @NotNull(message = "Vui long chon nhom san pham")
    private String categoryId;

    @NotNull(message = "Vui long nhap so luong")
    @DecimalMin(value = "0.0", inclusive = false, message = "So luong phai la gia tri duong")
    private BigDecimal quantity;

    @NotBlank(message = "Vui long nhap mo ta")
    @Size(max = 2000, message = "Mo ta khong duoc vuot qua 2000 ky tu")
    private String description;

    private Boolean invisible;
}
