package vn.baotinmanhhai.kioskadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * DTO RIENG cua kiosk-admin cho nhom san pham - chi lay cac truong UI can hien, KHONG import
 * truc tiep ProductCategoryResponse cua admin-portal (module Maven khac, khong nam tren
 * classpath cua app nay).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private Integer priority;
    /** Admin-portal chi tra config cua kenh token duoc quyen (o day: KIOSK). */
    private List<PartnerConfig> partnerConfigs;

    /** Nhom co dang hien tren App Kiosk khong (config KIOSK isActive). */
    public boolean isKioskActive() {
        return partnerConfigs != null && partnerConfigs.stream()
                .anyMatch(c -> "KIOSK".equals(c.getPartnerCode()) && Boolean.TRUE.equals(c.getIsActive()));
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PartnerConfig {
        private String partnerCode;
        private Boolean isActive;
    }
}
