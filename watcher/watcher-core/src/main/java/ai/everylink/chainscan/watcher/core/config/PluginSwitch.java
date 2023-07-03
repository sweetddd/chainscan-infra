package ai.everylink.chainscan.watcher.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "watcher.plugin")
@Data
public class PluginSwitch {

    private Boolean batchSwitch;

}