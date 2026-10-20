package vn.baotinmanhhai.kioskadmin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.baotinmanhhai.kioskadmin.entity.TransactionLog;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {

    Page<TransactionLog> findAllByOrderByTimeDesc(Pageable pageable);
}
