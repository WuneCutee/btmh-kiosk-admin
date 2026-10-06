package vn.baotinmanhhai.kioskadmin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Slf4j
@SpringBootApplication
public class KioskAdminApplication {

    public static void main(String[] args) {
        // QUAN TRONG: phai tao bang kiosk_admin_user (bang MOI, rieng cua app nay) TRUOC KHI
        // Spring khoi tao Hibernate, vi Hibernate ddl-auto=validate se kiem tra TAT CA entity
        // (bao gom KioskAdminUser) ngay luc build EntityManagerFactory - qua som de mot
        // @PostConstruct/ApplicationRunner khac tao bang kip luc. Dung JDBC thuan, khong qua
        // Spring container, giong nhu mot migration buoc tien xu ly toi thieu.
        ensureAdminUserTableExists();
        SpringApplication.run(KioskAdminApplication.class, args);
    }

    private static void ensureAdminUserTableExists() {
        String host = env("DB_HOST", "localhost");
        String port = env("DB_PORT", "5434");
        String db = env("DB_NAME", "localdb");
        String user = env("DB_USER", "devbtmh");
        String password = env("DB_PASSWORD", "changeme");
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS kiosk_admin_user (
                        id BIGSERIAL PRIMARY KEY,
                        username VARCHAR(100) NOT NULL UNIQUE,
                        password_hash VARCHAR(100) NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now()
                    )
                    """);
            log.info("Pre-flight: kiosk_admin_user table OK (created if missing)");
        } catch (Exception e) {
            log.error("Pre-flight: khong the tao/kiem tra bang kiosk_admin_user - app se loi khi Hibernate validate schema. "
                    + "Kiem tra ket noi DB (DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD).", e);
            throw new IllegalStateException("Khong ket noi duoc DB de chuan bi bang kiosk_admin_user", e);
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
