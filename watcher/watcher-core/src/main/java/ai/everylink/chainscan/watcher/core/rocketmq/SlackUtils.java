package ai.everylink.chainscan.watcher.core.rocketmq;

import ai.everylink.chainscan.watcher.core.config.EvmConfig;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * SlackUtils
 *
 * @author: david.zhanghui@everylink.ai
 */
public final class SlackUtils {

    private static Logger logger = LoggerFactory.getLogger(SlackUtils.class);

    private SlackUtils() {
    }

    public static void sendSlackNotify(String channelId, String title, String msg) {
        try {
            DefaultMQProducer producer = new DefaultMQProducer("producer_group_dtx_alert");
            producer.setNamesrvAddr(getNamesrvAddr());
            producer.start();

            //Create a message instance, specifying topic, tag and message body.
            Message rocketMsg = new Message("notification-normal", "dtx_alert",
                    JSON.toJSONString(buildSlackMessage(channelId, title, msg)).getBytes(StandardCharsets.UTF_8));

            //Call send message to deliver message to one of brokers.
            SendResult sendResult = producer.send(rocketMsg);
            logger.info("[slack]sendResult={}", sendResult);

            //Shut down once the producer instance is not longer in use.
            producer.shutdown();
        } catch (Exception e) {
            logger.error("[slack]send notification fail.", e);
        }
    }

    public static String getNamesrvAddr() {
        String val = System.getenv("watcher.rocketmq.server.addr");
        if (!StringUtils.isEmpty(val)) {
            logger.info("[slack]got name server addr from env: {}", val);
            return val;
        }

        val = SpringApplicationUtils.getBean(EvmConfig.class).getRocketmqSrvAddr();
        logger.info("[slack]got name server addr from config: {}", val);
        return val;
    }

    public static SlackMessage buildSlackMessage(String channelId, String title, String alertMsg) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("channelId", channelId);
        params.put("title", title);
        params.put("msg", alertMsg);

        SlackMessage bean = new SlackMessage();
        bean.setId(UUID.randomUUID().toString());
        bean.setParameters(params);

        if (System.getenv("watcher.notify.template") != null) {
            params.put("title", System.getenv("watcher.notify.template"));
            logger.info("[slack]template={}", System.getenv("watcher.notify.template"));
        }

        return bean;
    }

    public static void main(String[] args) throws Exception {
            DefaultMQProducer producer = new DefaultMQProducer("producer_group_dtx_alert");
            producer.setNamesrvAddr("rocketmq-namesrv.database.svc.cluster.local:9876");
            producer.start();

            for (int i = 0; i < 1; i++) {
                SlackMessage bean = new SlackMessage();
                bean.setId(UUID.randomUUID().toString());
                Map<String, Object> params = Maps.newHashMap();
                params.put("channelId", "C02UZMQUW5N");
                params.put("title", "DTX Testnet");
                params.put("msg", "闲着没事  \r\n报个警-" + System.currentTimeMillis());
                bean.setParameters(params);


                //Create a message instance, specifying topic, tag and message body.
                Message rocketMsg = new Message("notification-normal", "dtx_alert",
                        JSON.toJSONString(bean).getBytes(StandardCharsets.UTF_8));
                //Call send message to deliver message to one of brokers.
                SendResult sendResult = producer.send(rocketMsg);
                System.out.println(sendResult);

                Thread.sleep(50);
            }

    }

}
