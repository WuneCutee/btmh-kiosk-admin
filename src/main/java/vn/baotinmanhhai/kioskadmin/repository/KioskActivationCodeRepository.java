package vn.baotinmanhhai.kioskadmin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.baotinmanhhai.kioskadmin.entity.KioskActivationCode;

import java.util.Optional;

public interface KioskActivationCodeRepository extends JpaRepository<KioskActivationCode, String> {

    Optional<KioskActivationCode> findFirstByDeviceCodeAndUsedFalseAndRevokedFalseOrderByCreatedAtDesc(String deviceCode);

    void deleteByDeviceCode(String deviceCode);
}
