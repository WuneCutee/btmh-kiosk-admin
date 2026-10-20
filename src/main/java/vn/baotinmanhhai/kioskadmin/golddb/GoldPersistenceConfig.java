package vn.baotinmanhhai.kioskadmin.golddb;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Datasource/EMF/TransactionManager THU 2 - DB "gold_management" (CUNG DB voi admin-portal
 * dang dung cho product/category/inventory, da xac nhan bang \d product_prices tren chinh DB
 * nay - xem README). Chi 2 entity dung datasource nay: ProductPrice (bang chung
 * "product_prices") va PriceHistoryEntity (bang moi "kiosk_admin_price_history", so huu hoan
 * toan boi kiosk-admin). KHONG lien quan gi toi datasource CHINH (DB localdb cua webview, xem
 * PrimaryPersistenceConfig) - 2 DB nay hoan toan doc lap, khong co transaction chung giua 2 ben.
 *
 * <p>Bind tu GOLD_DB_HOST/PORT/NAME/USER/PASSWORD (xem application.yml, gold.datasource.*) -
 * mac dinh khop voi .env cua Backend/admin-portal luc dev local (host localhost, port 5432, db
 * gold_management, user gold_management).
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "vn.baotinmanhhai.kioskadmin.golddb.repository",
        entityManagerFactoryRef = "goldEntityManagerFactory",
        transactionManagerRef = "goldTransactionManager")
public class GoldPersistenceConfig {

    @Bean(name = "goldDataSourceProperties")
    @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "gold.datasource")
    public DataSourceProperties goldDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "goldDataSource")
    public DataSource goldDataSource(@Qualifier("goldDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }

    @Bean(name = "goldEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean goldEntityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("goldDataSource") DataSource dataSource,
            JpaProperties jpaProperties) {
        Map<String, Object> props = new HashMap<>(jpaProperties.getProperties());
        // ddl-auto=validate: bang product_prices/kiosk_admin_price_history da ton tai dung schema
        // (kiosk_admin_price_history tao qua JDBC pre-flight trong KioskAdminApplication.main(),
        // cung co che voi kiosk_admin_user) - Hibernate KHONG duoc tu doi schema cua gold_management.
        props.put("hibernate.hbm2ddl.auto", "validate");
        return builder.dataSource(dataSource)
                .packages("vn.baotinmanhhai.kioskadmin.golddb.entity")
                .persistenceUnit("gold")
                .properties(props)
                .build();
    }

    @Bean(name = "goldTransactionManager")
    public PlatformTransactionManager goldTransactionManager(
            @Qualifier("goldEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
