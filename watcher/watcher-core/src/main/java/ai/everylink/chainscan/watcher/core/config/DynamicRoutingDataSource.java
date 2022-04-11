package ai.everylink.chainscan.watcher.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * @Remark: 加载数据库配置
 * @Author: brett
 * @Date : 2022/3/24 18:25
 */
@Slf4j
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        Object lookupKey = DynamicRoutingDataSource.getRoutingDataSource();
        //log.info(Thread.currentThread().getName() + " determineCurrentLookupKey : " + lookupKey);
        return lookupKey;
    }

    private static final ThreadLocal<Object> threadLocalDataSource = new ThreadLocal<>();


    public static void setRoutingDataSource(Object dataSource) {
        if (dataSource == null) {
            throw new NullPointerException();
        }
        threadLocalDataSource.set(dataSource);
    }

    public static Object getRoutingDataSource() {
        Object dataSourceType = threadLocalDataSource.get();
        if (dataSourceType == null) {
            threadLocalDataSource.set(DataSourceEnum.chainscan);
            return getRoutingDataSource();
        }
        return dataSourceType;
    }

    public static void removeRoutingDataSource() {
        threadLocalDataSource.remove();
    }
}
