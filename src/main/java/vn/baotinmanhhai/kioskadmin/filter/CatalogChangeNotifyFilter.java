package vn.baotinmanhhai.kioskadmin.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.baotinmanhhai.kioskadmin.service.KioskApiNotifier;

import java.io.IOException;
import java.util.List;

/**
 * Mot diem chan duy nhat thay vi sua tung service: moi request GHI (khong phai GET) thanh cong (2xx) vao
 * san pham/gia/nhom/ton -> bao kiosk-api de App cap nhat ngay.
 */
public class CatalogChangeNotifyFilter extends OncePerRequestFilter {

    private static final List<String> CATALOG_PREFIXES = List.of(
            "/api/admin/products", "/api/admin/prices", "/api/admin/categories", "/api/admin/inventory",
            "/api/admin/stores", "/api/admin/limits");

    private final KioskApiNotifier notifier;

    public CatalogChangeNotifyFilter(KioskApiNotifier notifier) {
        this.notifier = notifier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("GET".equals(request.getMethod()) || "OPTIONS".equals(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return CATALOG_PREFIXES.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(request, response);
        int status = response.getStatus();
        if (status >= 200 && status < 300) {
            notifier.catalogChanged(request.getMethod() + " " + request.getRequestURI());
        }
    }
}
