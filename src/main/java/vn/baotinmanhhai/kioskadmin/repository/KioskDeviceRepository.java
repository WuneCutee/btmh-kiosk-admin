package vn.baotinmanhhai.kioskadmin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.baotinmanhhai.kioskadmin.entity.KioskDevice;

public interface KioskDeviceRepository extends JpaRepository<KioskDevice, String> {
}
