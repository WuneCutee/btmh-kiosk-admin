package vn.baotinmanhhai.kioskadmin.golddb.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.baotinmanhhai.kioskadmin.golddb.entity.PriceHistoryEntity;

public interface PriceHistoryRepository extends JpaRepository<PriceHistoryEntity, Long> {

    Page<PriceHistoryEntity> findByProductCodeOrderByChangedAtDesc(String productCode, Pageable pageable);
}
