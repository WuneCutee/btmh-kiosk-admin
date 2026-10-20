package vn.baotinmanhhai.kioskadmin.golddb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Bang MOI "kiosk_admin_price_history", so huu HOAN TOAN boi kiosk-admin - ghi lai lich su
 * sua gia thu cong (truong hop DUY NHAT khong co co che audit nao khac trong he thong thuc, vi
 * he thong thuc khong co API sua gia cho nguoi - gia chi den tu feed ty gia BTMH). Nam trong DB
 * gold_management (cung DB voi product_prices), tao qua JDBC pre-flight trong
 * KioskAdminApplication.main() (cung co che voi kiosk_admin_audit_log, nhung noi vao ket noi
 * GOLD_DB_* thay vi DB_*).
 */
@Entity
@Table(name = "kiosk_admin_price_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "old_price")
    private Long oldPrice;

    @Column(name = "new_price", nullable = false)
    private Long newPrice;

    @Column(name = "old_buy_price")
    private Long oldBuyPrice;

    @Column(name = "new_buy_price")
    private Long newBuyPrice;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
