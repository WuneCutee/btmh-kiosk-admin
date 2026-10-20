package vn.baotinmanhhai.kioskadmin.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Them cac response header bao mat co ban cho TOAN BO request (khac JwtAuthFilter chi ap dung
 * /api/admin/* - header nay nen ap dung ca cho /, /api/auth/login, file tinh...). Vi vay DANH
 * DAU @Component de Spring Boot tu dang ky cho "/*" (mac dinh), khong dung FilterRegistrationBean
 * nhu JwtAuthFilter.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        // Huong dan hien dai: tat XSS auditor cu (co the bi loi dung sang tan cong), dua vao
        // CSP/cac bien phap khac thay vi dua vao trinh duyet tu "doan" loc XSS.
        response.setHeader("X-XSS-Protection", "0");
        filterChain.doFilter(request, response);
    }
}
