package vn.baotinmanhhai.kioskadmin.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.util.JwtService;

import java.io.IOException;

/**
 * Filter thu cong kieu OncePerRequestFilter (bat chuoc phong cach
 * KioskInternalAuthFilter ben webview) - khong dung Spring Security day du vi muc tieu
 * "nho gon". Bao ve moi thu duoi /api/admin/**, bo qua /api/auth/login va cac file tinh.
 *
 * <p>KHONG danh dau @Component: filter nay duoc dang ky thu cong qua FilterRegistrationBean
 * (xem FilterConfig) chi cho /api/admin/* - neu de @Component, Spring Boot se tu dong dang ky
 * THEM 1 lan nua cho toan bo /* (chay 2 lan / sai url-pattern).
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTR_USERNAME = "kioskAdminUsername";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(response, "Thieu Authorization Bearer token");
            return;
        }
        String token = header.substring(7);
        try {
            String username = jwtService.parseAndValidate(token);
            request.setAttribute(REQUEST_ATTR_USERNAME, username);
        } catch (JwtException | IllegalArgumentException e) {
            unauthorized(response, "Token khong hop le hoac het han");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("401", message)));
    }
}
