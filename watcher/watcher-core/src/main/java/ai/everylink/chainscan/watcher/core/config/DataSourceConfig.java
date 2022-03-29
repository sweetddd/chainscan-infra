package ai.everylink.chainscan.watcher.core.config;

import org.springframework.beans.factory.annotation.Qualifier;
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
public class DataSourceConfig {

    @Bean(name = "chainscanDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.chainscan")
    public DataSource chainscanDataSource() {
        DataSource build = DataSourceBuilder.create().build();
        return build;
    }

    @Bean(name = "walletDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.wallet")
    public DataSource walletDataSource() {
        DataSource build = DataSourceBuilder.create().build();
        return build;
    }

    @Bean
    @Primary
    public DynamicRoutingDataSource dynamicDataSource(
            @Qualifier(value = "chainscanDataSource") DataSource chainscanDataSource,
            @Qualifier(value = "walletDataSource") DataSource walletDataSource) {
        Map<Object, Object> targetDataSources = new HashMap<>(2);
        targetDataSources.put(DataSourceEnum.chainscan, chainscanDataSource);
        targetDataSources.put(DataSourceEnum.wallet, walletDataSource);
        DynamicRoutingDataSource dynamicRoutingDataSource = new DynamicRoutingDataSource();
        //设置数据源
        dynamicRoutingDataSource.setTargetDataSources(targetDataSources);
        //设置默认选择的数据源
        dynamicRoutingDataSource.setDefaultTargetDataSource(chainscanDataSource);
        dynamicRoutingDataSource.afterPropertiesSet();
        return dynamicRoutingDataSource;
    }


}

