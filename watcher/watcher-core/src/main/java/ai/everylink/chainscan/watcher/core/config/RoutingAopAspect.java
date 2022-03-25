package ai.everylink.chainscan.watcher.core.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @Remark: 切面拦截动态数据源注解
 * @Author: brett
 * @Date : 2022/3/24 18:26
 */
@Order(0)
@Aspect
@Component
public class RoutingAopAspect {

    @Around("@annotation(targetDataSource)")
    public Object routingDataSource(ProceedingJoinPoint joinPoint, TargetDataSource targetDataSource) throws Throwable {
        try {
            DynamicRoutingDataSource.setRoutingDataSource(targetDataSource.value());
            return joinPoint.proceed();
        } finally {
            DynamicRoutingDataSource.removeRoutingDataSource();
        }
    }
}
