package vn.baotinmanhhai.kioskadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO RIENG cua kiosk-admin cho 1 dong ton kho - du lieu thuc tu admin-portal (InventoryResponse
 * ben do), trong do remainingQuantity/availableQuantity phan anh gia tri Redis
 * (INVENTORY_QTY:{partnerCode}:{productCode}) ma admin-portal da dong bo, KHONG phai do
 * kiosk-admin tu doc Redis.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryResponse {
    private Integer id;
    private String partnerCode;
    private String productCode;
    private String productName;
    private String categoryCode;
    private String categoryName;
    private BigDecimal quantity;
    /** Dinh muc ton kho trong ngay - day la gia tri PUT /inventories/{id} se ghi de, KHONG phai so con lai. */
    private Integer originalQuantity;
    private Integer remainingQuantity;
    private Integer availableQuantity;
    private BigDecimal totalQuantity;
}
