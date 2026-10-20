package vn.baotinmanhhai.kioskadmin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.baotinmanhhai.kioskadmin.dto.PriceHistoryResponse;
import vn.baotinmanhhai.kioskadmin.dto.PriceResponse;
import vn.baotinmanhhai.kioskadmin.exception.BusinessException;
import vn.baotinmanhhai.kioskadmin.golddb.entity.PriceHistoryEntity;
import vn.baotinmanhhai.kioskadmin.golddb.entity.ProductPrice;
import vn.baotinmanhhai.kioskadmin.golddb.repository.PriceHistoryRepository;
import vn.baotinmanhhai.kioskadmin.golddb.repository.ProductPriceRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Sua gia THU CONG - TRUONG HOP NGOAI LE DUY NHAT ma kiosk-admin ghi truc tiep vao bang dung
 * chung voi he thong thuc (product_prices), khong qua admin-portal (xem README phan "Kien truc
 * / quyet dinh ky thuat" de biet ly do: he thong thuc KHONG co API sua gia cho nguoi, gia chi
 * den tu feed ty gia BTMH - nhung nguoi dung van muon co man sua gia thu cong).
 *
 * <p>setPrice(...) doc gia tri CU truoc, upsert gia tri MOI, roi ghi 1 dong vao
 * kiosk_admin_price_history - ca 3 buoc trong 1 @Transactional (dung TransactionManager cua
 * datasource "gold", xem GoldPersistenceConfig) de dam bao atomic: khong the co truong hop
 * product_prices doi nhung lich su khong ghi duoc (hoac nguoc lai).
 */
@Service
@RequiredArgsConstructor
public class ProductPriceService {

    private static final int MAX_PAGE_SIZE = 200;

    private final ProductPriceRepository productPriceRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public PriceResponse getCurrent(String productCode) {
        ProductPrice price = productPriceRepository.findByProductCode(productCode)
                .orElseThrow(() -> new BusinessException(
                        "San pham " + productCode + " chua duoc thiet lap gia", "404", 404));
        return toResponse(price);
    }

    @Transactional("goldTransactionManager")
    public PriceResponse setPrice(String productCode, BigDecimal price, BigDecimal buyPrice, String description,
                                   String actor) {
        Optional<ProductPrice> existing = productPriceRepository.findByProductCode(productCode);
        Long oldPrice = existing.map(ProductPrice::getPrice).map(BigDecimal::longValue).orElse(null);
        Long oldBuyPrice = existing.map(ProductPrice::getBuyPrice).map(BigDecimal::longValue).orElse(null);

        UUID id = existing.map(ProductPrice::getId).orElseGet(UUID::randomUUID);
        productPriceRepository.upsert(id, productCode, price, buyPrice, description);

        priceHistoryRepository.save(PriceHistoryEntity.builder()
                .productCode(productCode)
                .oldPrice(oldPrice)
                .newPrice(price.longValue())
                .oldBuyPrice(oldBuyPrice)
                .newBuyPrice(buyPrice != null ? buyPrice.longValue() : null)
                .changedBy(actor)
                .changedAt(Instant.now())
                .build());

        return getCurrent(productCode);
    }

    public Page<PriceHistoryResponse> history(String productCode, int page, int size) {
        int s = size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        return priceHistoryRepository.findByProductCodeOrderByChangedAtDesc(productCode, PageRequest.of(Math.max(page, 0), s))
                .map(this::toHistoryResponse);
    }

    private PriceResponse toResponse(ProductPrice p) {
        return PriceResponse.builder()
                .productCode(p.getProductCode())
                .price(p.getPrice())
                .buyPrice(p.getBuyPrice())
                .description(p.getDescription())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private PriceHistoryResponse toHistoryResponse(PriceHistoryEntity h) {
        return PriceHistoryResponse.builder()
                .id(h.getId())
                .productCode(h.getProductCode())
                .oldPrice(h.getOldPrice())
                .newPrice(h.getNewPrice())
                .oldBuyPrice(h.getOldBuyPrice())
                .newBuyPrice(h.getNewBuyPrice())
                .changedBy(h.getChangedBy())
                .changedAt(h.getChangedAt())
                .build();
    }
}
