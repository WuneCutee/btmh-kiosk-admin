package vn.baotinmanhhai.kioskadmin.golddb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Shadow mapping cua bang "product_prices" - bang nay CHUNG voi admin-portal/webview nhung
 * NAM TRONG DB gold_management (cung DB voi "product"/"category"/"inventory" ma AdminPortalClient
 * dang thao tac qua API), KHONG nam trong DB localdb cua webview - da xac nhan bang
 * \d product_prices truc tiep tren ca 2 DB (xem README). Dung EntityManagerFactory/DataSource
 * RIENG (xem GoldPersistenceConfig), KHAC voi cac entity khac trong goi
 * vn.baotinmanhhai.kioskadmin.entity (dung datasource mac dinh / localdb).
 *
 * <p>Cot KHOP CHINH XAC voi \d product_prices tren DB thuc (gold_management) - KHONG dung lai
 * entity ProductPrice (neu co) ben Backend/webview vi da xac nhan entity do THIEU cot
 * buy_price so voi bang thuc.
 *
 * <p>UNIQUE(product_code) => day la bang upsert-in-place (xem ProductPriceRepository).
 */
@Entity
@Table(name = "product_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPrice {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private java.util.UUID id;

    @Column(name = "product_code", length = 50, nullable = false, unique = true)
    private String productCode;

    @Column(name = "price", precision = 20, scale = 0, nullable = false)
    private BigDecimal price;

    @Column(name = "buy_price", precision = 20, scale = 0)
    private BigDecimal buyPrice;

    @Column(name = "description")
    private String description;

    @Column(name = "id_request_update", length = 100)
    private String idRequestUpdate;

    @Column(name = "price_history_id", columnDefinition = "uuid")
    private java.util.UUID priceHistoryId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
