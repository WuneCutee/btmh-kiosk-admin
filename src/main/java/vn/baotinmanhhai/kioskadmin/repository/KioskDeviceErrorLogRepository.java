package vn.baotinmanhhai.kioskadmin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.baotinmanhhai.kioskadmin.entity.KioskDeviceErrorLog;

public interface KioskDeviceErrorLogRepository extends JpaRepository<KioskDeviceErrorLog, Long> {

    Page<KioskDeviceErrorLog> findAllByOrderByReceivedAtDesc(Pageable pageable);

    Page<KioskDeviceErrorLog> findByDeviceCodeOrderByReceivedAtDesc(String deviceCode, Pageable pageable);
}
