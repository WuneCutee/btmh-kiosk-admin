package vn.baotinmanhhai.kioskadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO RIENG cua kiosk-admin cho san pham (thay cho entity Product shadow-mapping cu, da go bo).
 * Du lieu thuc lay tu admin-portal (ProductResponse ben do) qua AdminPortalClient - chi lay cac
 * truong UI can hien.
 *
 * <p>QUAN TRONG: truong "invisible" top-level nay KHONG duoc admin-portal cap nhat qua PATCH
 * .../active (da xac nhan qua test thuc te - PATCH .../active chi doi partnerConfigs[].isActive,
 * KHONG doi "invisible" top-level) - vi vay KHONG dung "invisible" de xac dinh trang thai hien
 * thi THEO KENH KIOSK. Dung isKioskVisible() (doc tu partnerConfigs) cho muc dich do (xem
 * ProductService#toggleVisibility).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductResponse {
    private String id;
    private String name;
    private String code;
    private String categoryId;
    private String categoryCode;
    private String categoryName;
    private BigDecimal quantity;
    private String description;
    private Boolean invisible;
    private List<PartnerConfig> partnerConfigs;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PartnerConfig {
        private String partnerCode;
        private Boolean isActive;
    }

    /** True neu kenh KIOSK dang AN (isActive=false hoac khong co partnerConfig KIOSK nao ca). */
    public boolean isKioskInvisible() {
        if (partnerConfigs == null) {
            return true;
        }
        return partnerConfigs.stream()
                .filter(pc -> "KIOSK".equals(pc.getPartnerCode()))
                .findFirst()
                .map(pc -> pc.getIsActive() == null || !pc.getIsActive())
                .orElse(true);
    }
}
