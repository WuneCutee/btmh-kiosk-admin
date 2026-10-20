package vn.baotinmanhhai.kioskadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Shadow mapping CHI DOC cua bang "price_update_logs" (da co san trong DB webview) - chi de xem
 * lai lich su cap nhat gia san pham, khong co CUD.
 */
@Entity
@Table(name = "price_update_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceUpdateLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "price", nullable = false, precision = 20, scale = 0)
    private BigDecimal price;

    @Column(name = "id_request_update", length = 100)
    private String idRequestUpdate;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
