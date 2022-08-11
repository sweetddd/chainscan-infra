package ai.everylink.chainscan.watcher.core.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.everylink.chainscan.watcher.plugin")
@Data
public class PluginChainId {

    private String evmWatcher;
    private String chainMonitorWatcher;
    private String nFTAuctionSpiPlugin;
    private String tokenSpiPlugin;
    private String tokenWatcher;
    private String transactionHistorySpiPlugin;
}
