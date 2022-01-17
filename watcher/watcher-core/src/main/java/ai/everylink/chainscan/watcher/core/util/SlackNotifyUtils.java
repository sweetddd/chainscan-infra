package ai.everylink.chainscan.watcher.core.util;


import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Slack通知类
 *
 * @author david.zhang@everylink.ai
 * @since 2021-12-27
 */
public final class SlackNotifyUtils {

    private SlackNotifyUtils() {}

    public static int MAX_MSG_LEN = 20000;

    /**
     * slack通知发送结果
     */
    public static class SlackNotifyResult {

        public SlackNotifyResult() {}

        public SlackNotifyResult(boolean success) {
            this.success = success;
        }

        public SlackNotifyResult(boolean success, Throwable e) {
            this.success = success;
            this.e = e;
        }

        /**
         * true - 发送成功
         * false - 发送失败
         */
        public boolean success = true;

        /**
         * 发送失败(success=false)时的异常信息
         */
        public Throwable e;
    }

    /**
     * 发送slack通知
     *
     * @param slackWebhookUrl slack频道中配置的webhook url
     * @param msg 要通知到额内容
     * @return
     */
    public static SlackNotifyResult sendSlackNotificationWithRawText(String slackWebhookUrl, String msg) {
        if (StringUtils.isEmpty(slackWebhookUrl)) {
            return buildSlackNotifyResult(false, "slackWebhookUrl is null");
        }
        if (StringUtils.isEmpty(msg)) {
            return buildSlackNotifyResult(false, "msg is null");
        }
        if (msg.length() >= MAX_MSG_LEN) {
            return buildSlackNotifyResult(false, "msg's length is over " + MAX_MSG_LEN);
        }

        String json = "{\"text\":\"" + msg + "\"}";
        return sendSlackNotification(slackWebhookUrl, json);
    }

    /**
     * 发送slack通知
     * @param slackWebhookUrl slack频道中配置的webhook url
     * @param msg 要发送到slack频道的JSON格式字符串。{"text":"要发送的内容字符串"}
     */
    public static SlackNotifyResult sendSlackNotification(String slackWebhookUrl, String msg) {
        SlackNotifyResult result = new SlackNotifyResult();
        try {
            HttpURLConnection conn = (HttpURLConnection) (new URL(slackWebhookUrl)).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.connect();

            OutputStream out = conn.getOutputStream();
            out.write(msg.getBytes(StandardCharsets.UTF_8));
            out.flush();

            StringBuffer rest = new StringBuffer();
            BufferedReader br = null;

            // 读取数据
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            String line = null;
            while (null != (line = br.readLine())) {
                rest.append(line);
            }
            br.close();
            out.close();
            conn.disconnect();

            if ("ok".equalsIgnoreCase(rest.toString())) {
                result.success = true;
            }
        } catch (Exception e) {
            result.success = false;
            result.e = e;
        }

        return result;
    }

    private static SlackNotifyResult buildSlackNotifyResult(boolean success, String errorMsg) {
        if (success) {
            return new SlackNotifyResult(true);
        }

        return new SlackNotifyResult(false, new IllegalArgumentException(errorMsg));
    }
}
