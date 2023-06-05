package ai.everylink.chainscan.watcher.core.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * @Remark: 加载配置文件中的数据库
 * @Author: brett
 * @Date : 2022/3/24 18:24
 */
@EnableTransactionManagement
@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@Slf4j
public class DataSourceConfig {

    @Value("${spring.datasource.hikari.maximum-pool-size}")
    private int maximumPoolSize;
    @Value("${spring.datasource.hikari.minimum-idle}")
    private int minimumIdle;
    @Value("${spring.datasource.hikari.max-lifetime}")
    private long maxLifetime;
    @Value("${spring.datasource.hikari.idle-timeout}")
    private long idleTimeout;

    @Bean(name = "chainscanDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.chainscan")
    public DataSource chainscanDataSource() {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setMaximumPoolSize(maximumPoolSize);
        hikariDataSource.setMinimumIdle(minimumIdle);
        hikariDataSource.setMaxLifetime(maxLifetime);
        hikariDataSource.setIdleTimeout(idleTimeout);
        log.info("chainscanDataSource:maximumPoolSize:{}, minimumIdle:{}, maxLifetime:{}, idleTimeout:{}", maximumPoolSize, minimumIdle, maxLifetime, idleTimeout);
        return hikariDataSource;
    }

    @Bean(name = "walletDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.wallet")
    public DataSource walletDataSource() {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setMaximumPoolSize(maximumPoolSize);
        hikariDataSource.setMinimumIdle(minimumIdle);
        hikariDataSource.setMaxLifetime(maxLifetime);
        hikariDataSource.setIdleTimeout(idleTimeout);
        log.info("walletDataSource:maximumPoolSize:{}, minimumIdle:{}, maxLifetime:{}, idleTimeout:{}", maximumPoolSize, minimumIdle, maxLifetime, idleTimeout);
        return hikariDataSource;
    }

    @Bean(name = "marketplaceDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.marketplace")
    public DataSource marketplaceDataSource() {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setMaximumPoolSize(maximumPoolSize);
        hikariDataSource.setMinimumIdle(minimumIdle);
        hikariDataSource.setMaxLifetime(maxLifetime);
        hikariDataSource.setIdleTimeout(idleTimeout);
        log.info("marketplaceDataSource:maximumPoolSize:{}, minimumIdle:{}, maxLifetime:{}, idleTimeout:{}", maximumPoolSize, minimumIdle, maxLifetime, idleTimeout);
        return hikariDataSource;
    }

    @Bean
    @Primary
    public DynamicRoutingDataSource dynamicDataSource(
            @Qualifier(value = "chainscanDataSource") DataSource chainscanDataSource,
            @Qualifier(value = "walletDataSource") DataSource walletDataSource,
            @Qualifier(value = "marketplaceDataSource") DataSource marketplaceDataSource) {
        Map<Object, Object> targetDataSources = new HashMap<>(3);
        targetDataSources.put(DataSourceEnum.chainscan, chainscanDataSource);
        targetDataSources.put(DataSourceEnum.wallet, walletDataSource);
        targetDataSources.put(DataSourceEnum.marketplace, marketplaceDataSource);
        DynamicRoutingDataSource dynamicRoutingDataSource = new DynamicRoutingDataSource();
        //设置数据源
        dynamicRoutingDataSource.setTargetDataSources(targetDataSources);
        //设置默认选择的数据源
        dynamicRoutingDataSource.setDefaultTargetDataSource(chainscanDataSource);
        dynamicRoutingDataSource.afterPropertiesSet();
        return dynamicRoutingDataSource;
    }


}

