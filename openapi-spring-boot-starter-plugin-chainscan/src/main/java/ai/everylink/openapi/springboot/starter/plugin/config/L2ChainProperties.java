package ai.everylink.openapi.springboot.starter.plugin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "openapi.chain")
public class L2ChainProperties {

    /**
     * default: l2ChainUrl.
     */
    private String chainUrl;


}
