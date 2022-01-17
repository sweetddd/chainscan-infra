package ai.everylink.chainscan.watcher.bootstrap;

import ai.everylink.chainscan.watcher.core.util.SlackNotifyUtils;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
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
