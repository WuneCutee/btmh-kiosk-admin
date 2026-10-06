package vn.baotinmanhhai.kioskadmin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import vn.baotinmanhhai.kioskadmin.dto.LoginRequest;
import vn.baotinmanhhai.kioskadmin.dto.LoginResponse;
import vn.baotinmanhhai.kioskadmin.entity.KioskAdminUser;
import vn.baotinmanhhai.kioskadmin.exception.BusinessException;
import vn.baotinmanhhai.kioskadmin.repository.KioskAdminUserRepository;
import vn.baotinmanhhai.kioskadmin.util.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KioskAdminUserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest request) {
        KioskAdminUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("Sai ten dang nhap hoac mat khau", "401", 401));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Sai ten dang nhap hoac mat khau", "401", 401);
        }
        String token = jwtService.generateToken(user.getUsername());
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .expiresInMs(jwtService.getExpirationMs())
                .build();
    }
}
