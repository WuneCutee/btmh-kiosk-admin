package vn.baotinmanhhai.kioskadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Body cho PUT /api/admin/inventory/{id}. "quantity" la DINH MUC ton kho trong ngay (cot
 * original_quantity ben admin-portal), KHONG phai so luong con lai - admin-portal tu tinh delta
 * va dong bo Redis. Xem help text tuong ung trong UI (index.html) de nguoi dung khong nham voi
 * "so con lai".
 */
@Data
public class InventoryUpdateRequest {
    @NotNull(message = "Vui long nhap dinh muc ton kho")
    @Min(value = 0, message = "Dinh muc ton kho khong duoc la so am")
    private Integer quantity;
}
