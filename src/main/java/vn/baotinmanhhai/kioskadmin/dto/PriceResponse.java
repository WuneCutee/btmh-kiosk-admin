package vn.baotinmanhhai.kioskadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PriceResponse {
    private String productCode;
    private BigDecimal price;
    private BigDecimal buyPrice;
    private String description;
    private LocalDateTime updatedAt;
}
