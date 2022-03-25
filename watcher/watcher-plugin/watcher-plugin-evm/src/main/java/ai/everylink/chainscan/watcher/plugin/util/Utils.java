package ai.everylink.chainscan.watcher.plugin.util;

import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.plugin.config.EvmConfig;
import org.springframework.util.StringUtils;

/**
 * Utils
 *
 * @author: david.zhanghui@everylink.ai
 */
public final class Utils {
    private Utils() {}

    public static String getVmChainUrl() {
        String rpcUrl = System.getenv("watcher.vmChainUrl");
        if (StringUtils.isEmpty(rpcUrl)) {
            rpcUrl = SpringApplicationUtils.getBean(EvmConfig.class).getChainRpcUrl();
        }

        return rpcUrl;
    }

    public static Integer getScanStep() {
        String scanStepStr = System.getenv("watcher.chain.scanStep");
        if (!StringUtils.isEmpty(scanStepStr)) {
            try {
                return Integer.parseInt(scanStepStr);
            } catch (Exception e) {
                // ignore
            }
        }

        return SpringApplicationUtils.getBean(EvmConfig.class).getScanStep();
    }

    public static Integer getChainId() {
        String chainIdStr = System.getenv("watcher.chain.chainId");
        if (!StringUtils.isEmpty(chainIdStr)) {
            try {
                return Integer.parseInt(chainIdStr);
            } catch (Exception e) {
                // ignore
            }
        }

        return SpringApplicationUtils.getBean(EvmConfig.class).getChainId();
    }

    public static String getChainType() {
        String chainType = System.getenv("watcher.chain.chainType");
        if (StringUtils.isEmpty(chainType)) {
            chainType = SpringApplicationUtils.getBean(EvmConfig.class).getChainType();
        }

        return chainType;
    }
}
