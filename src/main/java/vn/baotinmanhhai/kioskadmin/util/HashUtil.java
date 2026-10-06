package vn.baotinmanhhai.kioskadmin.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Copy nguyen ham sha256Hex tu
 * Backend/webview/src/main/java/vn/baotinmanhhai/miniapp/b2bank/util/KioskSignatureUtil.java
 * de dam bao tuong thich tuyet doi voi cach webview verify ma kich hoat khi may kiosk goi
 * /api/v1/kiosk/devices/enroll. KHONG duoc doi thuat toan nay.
 */
public final class HashUtil {

    private HashUtil() {
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
