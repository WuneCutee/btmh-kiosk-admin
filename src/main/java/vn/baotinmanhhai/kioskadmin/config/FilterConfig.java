package vn.baotinmanhhai.kioskadmin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.baotinmanhhai.kioskadmin.filter.CatalogChangeNotifyFilter;
import vn.baotinmanhhai.kioskadmin.filter.JwtAuthFilter;
import vn.baotinmanhhai.kioskadmin.service.KioskApiNotifier;
import vn.baotinmanhhai.kioskadmin.util.JwtService;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtService jwtService, ObjectMapper objectMapper) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(new JwtAuthFilter(jwtService, objectMapper));
        registration.addUrlPatterns("/api/admin/*");
        registration.setOrder(1);
        return registration;
    }

    /** Chay sau JwtAuthFilter: chi request da xac thuc va xu ly thanh cong moi bao kiosk-api. */
    @Bean
    public FilterRegistrationBean<CatalogChangeNotifyFilter> catalogChangeNotifyFilterRegistration(KioskApiNotifier notifier) {
        FilterRegistrationBean<CatalogChangeNotifyFilter> registration =
                new FilterRegistrationBean<>(new CatalogChangeNotifyFilter(notifier));
        registration.addUrlPatterns("/api/admin/*");
        registration.setOrder(2);
        return registration;
    }
}
