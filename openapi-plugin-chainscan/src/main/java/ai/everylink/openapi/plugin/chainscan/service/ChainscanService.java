package ai.everylink.openapi.plugin.chainscan.service;

import org.springframework.web.server.ServerWebExchange;

/**
 * @Author Brett
 * @Description
 * @Date 2021/9/29 15:45
 **/
public interface ChainscanService {

    /**
     * 订单操作
     * @return
     */
    public Object balance(ServerWebExchange exchange);


    /**
     * 获取账户信息
     * @return
     */
    public Object txlist(ServerWebExchange exchange);
}
