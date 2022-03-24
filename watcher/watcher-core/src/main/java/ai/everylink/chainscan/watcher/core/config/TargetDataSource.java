package ai.everylink.chainscan.watcher.core.config;

import java.lang.annotation.*;

/**
 * @Remark: 自定义 动态切换数据库的注解
 * @Author: brett
 * @Date : 2022/3/24 18:27
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface TargetDataSource {
    String value();
}
