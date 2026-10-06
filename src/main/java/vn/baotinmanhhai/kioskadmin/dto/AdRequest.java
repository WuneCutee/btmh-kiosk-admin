package vn.baotinmanhhai.kioskadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdRequest {
    @NotBlank
    private String actionType;
    private String categoryId;
    @NotBlank
    private String mediaType;
    private String name;
    @NotNull
    private Integer displayOrder;
    private String productId;
    private String status;
    @NotBlank
    private String url;
    private String websiteUrl;
}
