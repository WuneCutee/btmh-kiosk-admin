package vn.baotinmanhhai.kioskadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryHistoryResponse {
    private String id;
    private Integer inventoryId;
    private String productId;
    private String actionType;
    private Integer oldQuantity;
    private Integer newQuantity;
    private Integer changeQuantity;
    private String createdAt;
    private String createdBy;
}
