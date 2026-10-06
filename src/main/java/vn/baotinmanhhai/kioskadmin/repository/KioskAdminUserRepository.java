package vn.baotinmanhhai.kioskadmin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.baotinmanhhai.kioskadmin.entity.KioskAdminUser;

import java.util.Optional;

public interface KioskAdminUserRepository extends JpaRepository<KioskAdminUser, Long> {
    Optional<KioskAdminUser> findByUsername(String username);
}
