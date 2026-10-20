package vn.baotinmanhhai.kioskadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request tao nhom san pham (category) - proxy sang POST /api/v1/product-categories ben
 * admin-portal. Rang buoc khop CHINH XAC voi CreateProductCategoryRequest ben do (code pattern
 * chi chu/so/_/- toi da 20 ky tu, name toi da 100, description toi da 2000) de bao loi 400 som
 * ngay tai kiosk-admin thay vi phai doi admin-portal tra loi.
 */
@Data
public class CategoryRequest {
    @NotBlank(message = "Vui long nhap ma nhom san pham")
    @Size(max = 20, message = "Ma nhom san pham khong duoc vuot qua 20 ky tu")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Ma nhom san pham chi duoc chua chu cai, chu so, '_' hoac '-'")
    private String code;

    @NotBlank(message = "Vui long nhap ten nhom san pham")
    @Size(max = 100, message = "Ten nhom san pham khong duoc vuot qua 100 ky tu")
    private String name;

    @NotBlank(message = "Vui long nhap mo ta")
    @Size(max = 2000, message = "Mo ta khong duoc vuot qua 2000 ky tu")
    private String description;
}
