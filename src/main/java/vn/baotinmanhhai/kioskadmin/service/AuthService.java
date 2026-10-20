package vn.baotinmanhhai.kioskadmin.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import vn.baotinmanhhai.kioskadmin.dto.LoginRequest;
import vn.baotinmanhhai.kioskadmin.dto.LoginResponse;
import vn.baotinmanhhai.kioskadmin.entity.KioskAdminUser;
import vn.baotinmanhhai.kioskadmin.exception.BusinessException;
import vn.baotinmanhhai.kioskadmin.repository.KioskAdminUserRepository;
import vn.baotinmanhhai.kioskadmin.util.JwtService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KioskAdminUserRepository userRepository;
    private final JwtService jwtService;
    private final LoginRateLimiterService loginRateLimiterService;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        // Chong brute-force: kiem tra khoa truoc ca khi dong vao DB/BCrypt.
        if (loginRateLimiterService.isLocked(request.getUsername(), ip)) {
            auditLogService.record(request.getUsername(), "LOGIN_FAILURE", "KioskAdminUser", request.getUsername(),
                    "Bi khoa do qua nhieu lan dang nhap sai", httpRequest);
            throw new BusinessException("Qua nhieu lan dang nhap sai, vui long thu lai sau", "429", 429);
        }

        Optional<KioskAdminUser> userOpt = userRepository.findByUsername(request.getUsername());
        boolean valid = userOpt.isPresent() && passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash());
        if (!valid) {
            loginRateLimiterService.recordFailure(request.getUsername(), ip);
            auditLogService.record(request.getUsername(), "LOGIN_FAILURE", "KioskAdminUser", request.getUsername(),
                    null, httpRequest);
            throw new BusinessException("Sai ten dang nhap hoac mat khau", "401", 401);
        }

        loginRateLimiterService.recordSuccess(request.getUsername(), ip);
        KioskAdminUser user = userOpt.get();
        String token = jwtService.generateToken(user.getUsername());
        auditLogService.record(user.getUsername(), "LOGIN_SUCCESS", "KioskAdminUser", user.getUsername(), null, httpRequest);
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .expiresInMs(jwtService.getExpirationMs())
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
