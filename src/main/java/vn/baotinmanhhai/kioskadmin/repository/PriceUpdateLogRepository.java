package vn.baotinmanhhai.kioskadmin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.baotinmanhhai.kioskadmin.entity.PriceUpdateLog;

import java.util.UUID;

public interface PriceUpdateLogRepository extends JpaRepository<PriceUpdateLog, UUID> {

    Page<PriceUpdateLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
