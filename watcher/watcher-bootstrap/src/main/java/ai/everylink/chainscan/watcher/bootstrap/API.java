package ai.everylink.chainscan.watcher.bootstrap;

import ai.everylink.chainscan.watcher.core.util.SlackNotifyUtils;
import com.google.common.util.concurrent.RateLimiter;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API
 *
 * @author david.zhang@everylink.ai
 * @since 2022-01-17
 */
@Slf4j
@RestController
@RequestMapping("/watcher/api/")
public class API {

    private RateLimiter rateLimiter = RateLimiter.create(10.0);
    private static App slackClient = null;
    private static final String BOT_TOKEN = "xoxb-1357501703478-2959698065829-3Fhg0mKMvRjBZ9vw06TqXpV3";
    private static final String SIGN_SECRET = "a816de71dceb7f1a85281d4da9f49351";
    static {
        AppConfig config = new AppConfig();
        config.setSingleTeamBotToken(System.getenv(BOT_TOKEN));
        config.setSigningSecret(System.getenv(SIGN_SECRET));
        slackClient = new App(config);
    }

    @GetMapping("sendSlackChannelMsg")
    public SlackNotifyUtils.SlackNotifyResult sendSlackChannelMsg(
            @RequestParam("channelId") String channelId,
            @RequestParam("msg") String msg) {
        if (!rateLimiter.tryAcquire()) {
            return new SlackNotifyUtils.SlackNotifyResult(false, new IllegalStateException("access limited"));
        }

        if (StringUtils.isEmpty(channelId)) {
            return new SlackNotifyUtils.SlackNotifyResult(false,
                    new IllegalArgumentException("channelId is null"));
        }
        if (StringUtils.isEmpty(msg)) {
            return new SlackNotifyUtils.SlackNotifyResult(false,
                    new IllegalArgumentException("msg is null"));
        }
        if (msg.length() >= SlackNotifyUtils.MAX_MSG_LEN) {
            return new SlackNotifyUtils.SlackNotifyResult(false,
                    new IllegalArgumentException("msg's length is over " + SlackNotifyUtils.MAX_MSG_LEN));
        }

        try {
            ChatPostMessageRequest req = ChatPostMessageRequest.builder().build();
            req.setText(msg);
            req.setChannel(channelId);
            req.setToken("xoxb-1357501703478-2887349608772-k88GQcoAhRQR7r47M1CwWJGQ");
            ChatPostMessageResponse resp = slackClient.client().chatPostMessage(req);
            return resp.isOk()
                    ? new SlackNotifyUtils.SlackNotifyResult(true)
                    : new SlackNotifyUtils.SlackNotifyResult(false, new IllegalStateException(resp.getError()));
        } catch (Throwable e) {
            return new SlackNotifyUtils.SlackNotifyResult(false, new IllegalStateException(e.getMessage()));
        }
    }

    @GetMapping("/sendSlackMsg")
    public SlackNotifyUtils.SlackNotifyResult sendSlackMsg(
            @RequestParam("slackWebhookUrl") String slackWebhookUrl,
            @RequestParam("msg") String msg) {
        if (rateLimiter.tryAcquire()) {
            return SlackNotifyUtils.sendSlackNotificationWithRawText(slackWebhookUrl, msg);
        } else {
            return new SlackNotifyUtils.SlackNotifyResult(false, new IllegalStateException("access limited"));
        }
    }
}
