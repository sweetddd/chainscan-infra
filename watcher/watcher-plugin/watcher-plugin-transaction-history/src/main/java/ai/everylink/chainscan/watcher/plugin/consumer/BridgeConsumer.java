package ai.everylink.chainscan.watcher.plugin.consumer;

import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.service.BridgeHistoryService;
import ai.everylink.common.json.JacksonHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Notification normal consumer
 *
 * @author Woody
 * @date 2021/1/20
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "bridge-proposal", consumerGroup = "watcher-transaction-bridge-group",
        consumeMode = ConsumeMode.CONCURRENTLY)
public class BridgeConsumer implements RocketMQListener<String> {


    @Resource
    private BridgeHistoryService bridgeHistoryService;


    @Override
    public void onMessage(String message) {
        Transaction body = JacksonHelper.deserialize(message, Transaction.class);
        log.info("消费延迟消息队列, [{}]",body);
        bridgeHistoryService.bridgeHistoryScan(body);
    }
}
