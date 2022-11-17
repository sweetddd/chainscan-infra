package ai.everylink.chainscan.watcher.core.rocketmq;

import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString
class SlackMessage {
  /**
   * Message id。消息唯一ID，每个消息的id必须不同。可以使用UUID，也可以使用System.nanoTime等，只要保证全局唯一就行。
  */
  private String id;

  /**
   * Account id。固定传"SLACK_ACCOUNT_001"
   */
  private String account = "SLACK_ACCOUNT_001";

  /**
   * Template name。固定传"DTX_WATCHER_ALERT_TMPL"
  */
  private String template = "DTX_WATCHER_ALERT_TMPL";
 
  /**
   * Notification channel.固定传数字4
  */
  private Integer channel = 4;
 
  /**
   * Location language。固定传zh_CN
  */
  private String language = "zh_CN";
 
  /**
   * Template parameters。消息内容。必须包含2个key。
   * 1. channelId - slack频道的id，参考上面的第2步【查找频道的ID】
   * 2. msg ：要发送的消息内容，如"hello there"。可以发送MD格式字符，参考https://slack.com/intl/zh-tw/help/articles/202288908-   %E8%A8%AD%E5%AE%9A%E8%A8%8A%E6%81%AF%E6%A0%BC%E5%BC%8F
   */
  private Map<String, Object> parameters;
}