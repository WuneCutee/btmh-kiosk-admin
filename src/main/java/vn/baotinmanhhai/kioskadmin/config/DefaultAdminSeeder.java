package vn.baotinmanhhai.kioskadmin.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import vn.baotinmanhhai.kioskadmin.entity.KioskAdminUser;
import vn.baotinmanhhai.kioskadmin.repository.KioskAdminUserRepository;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Seed 1 tai khoan admin mac dinh neu bang kiosk_admin_user rong luc khoi dong.
 * Mat khau sinh ngau nhien va IN RA CONSOLE LOG - day la lan DUY NHAT mat khau nay xuat hien
 * o dang plaintext, KHONG luu lai o dau khac. Neu mat log nay, phai xoa row trong
 * kiosk_admin_user va restart app de seed lai, hoac tu UPDATE password_hash bang BCrypt.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(10) // chay sau KioskAdminUserSchemaInitializer (PostConstruct chay truoc ApplicationRunner)
public class DefaultAdminSeeder implements ApplicationRunner {

    private static final String DEFAULT_USERNAME = "kioskadmin";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private final KioskAdminUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        String randomPassword = generateRandomPassword(16);
        KioskAdminUser user = KioskAdminUser.builder()
                .username(DEFAULT_USERNAME)
                .passwordHash(passwordEncoder.encode(randomPassword))
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);

        log.warn("==========================================================================");
        log.warn(" KIOSK-ADMIN: da tao tai khoan quan tri mac dinh (chua co user nao trong DB)");
        log.warn(" Username: {}", DEFAULT_USERNAME);
        log.warn(" Password: {}", randomPassword);
        log.warn(" >>> GHI LAI MAT KHAU NAY NGAY - se KHONG hien lai lan nao nua. <<<");
        log.warn(" Doi mat khau sau khi dang nhap lan dau (chua co API doi mat khau - tu UPDATE");
        log.warn(" password_hash bang BCrypt neu can, xem README.md).");
        log.warn("==========================================================================");
    }

    private String generateRandomPassword(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
