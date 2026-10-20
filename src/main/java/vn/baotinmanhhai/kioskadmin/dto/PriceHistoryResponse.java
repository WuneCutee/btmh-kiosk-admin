package vn.baotinmanhhai.kioskadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PriceHistoryResponse {
    private Long id;
    private String productCode;
    private Long oldPrice;
    private Long newPrice;
    private Long oldBuyPrice;
    private Long newBuyPrice;
    private String changedBy;
    private Instant changedAt;
}
