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
        // QUAN TRONG: phai tao cac bang MOI (kiosk_admin_user, kiosk_device_error_log,
        // kiosk_admin_audit_log) TRUOC KHI Spring khoi tao Hibernate, vi Hibernate
        // ddl-auto=validate se kiem tra TAT CA entity ngay luc build EntityManagerFactory - qua
        // som de mot @PostConstruct/ApplicationRunner khac tao bang kip luc. Dung JDBC thuan,
        // khong qua Spring container, giong nhu mot migration buoc tien xu ly toi thieu.
        ensureAdminUserTableExists();
        ensureDeviceErrorLogTableExists();
        ensureAdminAuditLogTableExists();
        ensurePriceHistoryTableExists();
        SpringApplication.run(KioskAdminApplication.class, args);
    }

    private static void ensureAdminUserTableExists() {
        execute("""
                CREATE TABLE IF NOT EXISTS kiosk_admin_user (
                    id BIGSERIAL PRIMARY KEY,
                    username VARCHAR(100) NOT NULL UNIQUE,
                    password_hash VARCHAR(100) NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
                )
                """, "kiosk_admin_user");
    }

    // Bang nay la bang CHUNG voi webview (webview GHI, kiosk-admin CHI DOC) - ca 2 ben deu tu
    // tao bang nay bang JDBC pre-flight (CREATE TABLE IF NOT EXISTS, idempotent) de khong phu
    // thuoc thu tu deploy giua 2 service. DDL phai khop CHINH XAC voi ben webview.
    private static void ensureDeviceErrorLogTableExists() {
        execute("""
                CREATE TABLE IF NOT EXISTS kiosk_device_error_log (
                    id BIGSERIAL PRIMARY KEY,
                    device_code VARCHAR(100),
                    error_id VARCHAR(36),
                    error_type VARCHAR(30),
                    message VARCHAR(500),
                    occurred_at TIMESTAMPTZ,
                    context TEXT,
                    received_at TIMESTAMPTZ DEFAULT now(),
                    CONSTRAINT uq_kiosk_device_error_log_device_error UNIQUE (device_code, error_id)
                )
                """, "kiosk_device_error_log");
    }

    // Bang MOI, so huu HOAN TOAN boi kiosk-admin - khong co service nao khac dong vao.
    private static void ensureAdminAuditLogTableExists() {
        execute("""
                CREATE TABLE IF NOT EXISTS kiosk_admin_audit_log (
                    id BIGSERIAL PRIMARY KEY,
                    username VARCHAR(100) NOT NULL,
                    action VARCHAR(100) NOT NULL,
                    target_type VARCHAR(50),
                    target_id VARCHAR(100),
                    detail VARCHAR(1000),
                    ip_address VARCHAR(64),
                    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
                )
                """, "kiosk_admin_audit_log");
    }

    // Bang MOI, so huu HOAN TOAN boi kiosk-admin - luu lich su sua gia THU CONG (truong hop
    // DUY NHAT khong co co che audit nao khac, vi he thong thuc khong co API sua gia cho nguoi).
    // QUAN TRONG: bang nay nam trong DB gold_management (CUNG DB voi product_prices ma
    // admin-portal dang dung), KHONG phai DB localdb nhu cac bang pre-flight khac ben tren - da
    // xac nhan bang \d product_prices tren ca 2 DB truoc khi quyet dinh (xem README). Vi vay
    // dung executeOn(...) voi bo tham so GOLD_DB_* rieng, khong dung chung ham execute(...) mac
    // dinh noi vao DB_* (localdb).
    private static void ensurePriceHistoryTableExists() {
        executeOn("""
                CREATE TABLE IF NOT EXISTS kiosk_admin_price_history (
                    id BIGSERIAL PRIMARY KEY,
                    product_code VARCHAR(50) NOT NULL,
                    old_price BIGINT, new_price BIGINT NOT NULL,
                    old_buy_price BIGINT, new_buy_price BIGINT,
                    changed_by VARCHAR(100) NOT NULL,
                    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
                )
                """, "kiosk_admin_price_history",
                env("GOLD_DB_HOST", "localhost"), env("GOLD_DB_PORT", "5432"),
                env("GOLD_DB_NAME", "gold_management"), env("GOLD_DB_USER", "gold_management"),
                env("GOLD_DB_PASSWORD", "changeme"));
    }

    private static void execute(String sql, String tableName) {
        executeOn(sql, tableName, env("DB_HOST", "localhost"), env("DB_PORT", "5434"),
                env("DB_NAME", "localdb"), env("DB_USER", "devbtmh"), env("DB_PASSWORD", "changeme"));
    }

    private static void executeOn(String sql, String tableName, String host, String port, String db,
                                   String user, String password) {
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Pre-flight: {} table OK (created if missing) tren {}", tableName, url);
        } catch (Exception e) {
            log.error("Pre-flight: khong the tao/kiem tra bang {} tren {} - app se loi khi Hibernate "
                    + "validate schema. Kiem tra ket noi DB tuong ung (DB_* hoac GOLD_DB_*).", tableName, url, e);
            throw new IllegalStateException("Khong ket noi duoc DB de chuan bi bang " + tableName, e);
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
