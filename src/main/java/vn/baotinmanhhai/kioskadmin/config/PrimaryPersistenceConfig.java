package vn.baotinmanhhai.kioskadmin.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Datasource/EMF/TransactionManager CHINH cua app (DB localdb cua webview - kiosk_device,
 * kiosk_activation_code, kiosk_ad, kiosk_admin_user, kiosk_admin_audit_log, kiosk_device_error_log,
 * transaction_log, price_update_logs - xem goi vn.baotinmanhhai.kioskadmin.entity/repository).
 *
 * <p>QUAN TRONG: app nay co 2 Postgres datasource khac nhau (xem GoldPersistenceConfig cho
 * datasource thu 2 - DB gold_management, chi danh cho ProductPrice/PriceHistoryEntity). Mot khi
 * khai bao THEM 1 @EnableJpaRepositories nua (cho goi golddb.repository), Spring Boot se tat
 * JpaRepositoriesAutoConfiguration mac dinh cho TOAN BO app - vi vay phai khai bao ro rang ca
 * @EnableJpaRepositories CHINH nay (basePackages=".repository") de cac repository cu (KioskDevice,
 * KioskAd...) tiep tuc duoc Spring Data quet, khong chi 1 minh goi golddb.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "vn.baotinmanhhai.kioskadmin.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager")
public class PrimaryPersistenceConfig {

    @Primary
    @Bean(name = "dataSourceProperties")
    @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource(@Qualifier("dataSourceProperties") DataSourceProperties props) {
        // QUAN TRONG: phai di qua DataSourceProperties.initializeDataSourceBuilder() (giong
        // GoldPersistenceConfig) - bind @ConfigurationProperties TRUC TIEP vao mot Hikari
        // DataSource se loi "jdbcUrl is required with driverClassName" vi Hikari dung property
        // "jdbcUrl", khong phai "url" nhu Spring Boot's DataSourceProperties quy uoc.
        return props.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("dataSource") DataSource dataSource,
            JpaProperties jpaProperties) {
        Map<String, Object> props = new HashMap<>(jpaProperties.getProperties());
        // Giu nguyen quy uoc cu: ddl-auto=validate, KHONG de Hibernate tu doi schema cua
        // webview/kiosk-admin dang dung (xem application.yml / README).
        props.put("hibernate.hbm2ddl.auto", "validate");
        return builder.dataSource(dataSource)
                .packages("vn.baotinmanhhai.kioskadmin.entity")
                .persistenceUnit("primary")
                .properties(props)
                .build();
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
