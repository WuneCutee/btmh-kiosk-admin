package vn.baotinmanhhai.kioskadmin.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chong brute-force dang nhap, don gian trong-tien-trinh (khong them infra moi - dung
 * ConcurrentHashMap, dung voi triet ly "nho gon" cua project). Key = username + "|" + ip.
 * Toi da MAX_ATTEMPTS lan sai trong WINDOW, qua nguong thi khoa toi khi het WINDOW tinh tu lan
 * sai gan nhat. Reset ngay khi dang nhap thanh cong.
 *
 * <p>Luu y: state nay mat khi app restart va khong chia se giua nhieu instance (khong co Redis) -
 * du cho quy mo 1 instance cua project nay. Don dep entry cu (de tranh phinh map vo han) chi lam
 * luoi khi co lan check moi cho cung key, khong co scheduled job rieng (co the cham nhe memory
 * neu co rat nhieu username/ip khac nhau tung thu sai, nhung chap nhan duoc cho quy mo nho gon).
 */
@Service
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String username, String ip) {
        Attempt attempt = attempts.get(key(username, ip));
        if (attempt == null) {
            return false;
        }
        if (isExpired(attempt)) {
            attempts.remove(key(username, ip));
            return false;
        }
        return attempt.count.get() >= MAX_ATTEMPTS;
    }

    public void recordFailure(String username, String ip) {
        String k = key(username, ip);
        attempts.compute(k, (key1, existing) -> {
            if (existing == null || isExpired(existing)) {
                return new Attempt();
            }
            existing.count.incrementAndGet();
            return existing;
        });
    }

    public void recordSuccess(String username, String ip) {
        attempts.remove(key(username, ip));
    }

    private boolean isExpired(Attempt attempt) {
        return Instant.now().toEpochMilli() - attempt.firstFailureAtMs > WINDOW_MS;
    }

    private String key(String username, String ip) {
        return (username == null ? "" : username) + "|" + (ip == null ? "" : ip);
    }

    private static class Attempt {
        private final long firstFailureAtMs = Instant.now().toEpochMilli();
        private final AtomicInteger count = new AtomicInteger(1);
    }
}
