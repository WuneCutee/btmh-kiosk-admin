package vn.baotinmanhhai.kioskadmin.golddb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.baotinmanhhai.kioskadmin.golddb.entity.ProductPrice;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProductPriceRepository extends JpaRepository<ProductPrice, UUID> {

    Optional<ProductPrice> findByProductCode(String productCode);

    /**
     * UNIQUE(product_code) tren bang thuc => upsert tai cho bang ON CONFLICT la cach sach nhat,
     * tranh phai doc-roi-ghi 2 buoc (va tranh race condition neu 2 request sua gia cung luc).
     * @Modifying bat buoc phai co de Spring Data biet day la cau lenh ghi, khong phai SELECT -
     * giong quy uoc @Modifying da dung trong cac repository khac cua app nay.
     */
    // clearAutomatically: ProductPriceService.setPrice doc lai ngay sau upsert trong cung transaction - khong xoa
    // persistence context thi Hibernate tra lai entity CU da nap truoc do va API tra gia cu du DB da doi.
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = """
            INSERT INTO product_prices (id, product_code, price, buy_price, description, created_at, updated_at)
            VALUES (:id, :productCode, :price, :buyPrice, :description, now(), now())
            ON CONFLICT (product_code) DO UPDATE SET
                price = EXCLUDED.price,
                buy_price = EXCLUDED.buy_price,
                description = EXCLUDED.description,
                updated_at = now()
            """, nativeQuery = true)
    void upsert(@Param("id") UUID id, @Param("productCode") String productCode, @Param("price") BigDecimal price,
                @Param("buyPrice") BigDecimal buyPrice, @Param("description") String description);
}
