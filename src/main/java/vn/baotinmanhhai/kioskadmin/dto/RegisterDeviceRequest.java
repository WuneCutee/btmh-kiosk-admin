package vn.baotinmanhhai.kioskadmin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDeviceRequest {
    @NotBlank
    private String deviceCode;
    private Long storeId;
    private String storeName;
    private String note;
}
