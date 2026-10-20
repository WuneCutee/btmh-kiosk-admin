package vn.baotinmanhhai.kioskadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request sua nhom san pham - khop UpdateProductCategoryRequest ben admin-portal: mo ta KHONG bat buoc
 * (khac luc tao), vi nhieu nhom co san co description = null.
 */
@Data
public class CategoryUpdateRequest {
    @NotBlank(message = "Vui long nhap ma nhom san pham")
    @Size(max = 20, message = "Ma nhom san pham khong duoc vuot qua 20 ky tu")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Ma nhom san pham chi duoc chua chu cai, chu so, '_' hoac '-'")
    private String code;

    @NotBlank(message = "Vui long nhap ten nhom san pham")
    @Size(max = 100, message = "Ten nhom san pham khong duoc vuot qua 100 ky tu")
    private String name;

    @Size(max = 2000, message = "Mo ta khong duoc vuot qua 2000 ky tu")
    private String description;
}
