package ai.everylink.chainscan.watcher.core.util;

import ai.everylink.chainscan.watcher.core.config.EvmConfig;
import org.springframework.util.StringUtils;

/**
 * Utils
 *
 * @author: david.zhanghui@everylink.ai
 */
public final class WatcherUtils {
    private WatcherUtils() {}

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


    public static Integer getProcessStep() {
        String scanStepStr = System.getenv("watcher.chain.processStep");
        if (!StringUtils.isEmpty(scanStepStr)) {
            try {
                return Integer.parseInt(scanStepStr);
            } catch (Exception e) {
                // ignore
            }
        }

        return SpringApplicationUtils.getBean(EvmConfig.class).getProcessStep();
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

    public static Integer getBatchInsertSize() {
        String batchInsertSize = System.getenv("watcher.chain.batchInsertSize");
        if (!StringUtils.isEmpty(batchInsertSize)) {
            try {
                return Integer.parseInt(batchInsertSize);
            } catch (Exception e) {
                // ignore
            }
        }

        return 30;
    }

    public static boolean isScanStop() {
        String flag = System.getenv("watcher.scan.switch");
        if (!StringUtils.isEmpty(flag)) {
            return flag.trim().equalsIgnoreCase("true");
        }

        return false;
    }

    public static boolean isProcessConcurrent() {
        String flag = System.getenv("watcher.process.concurrent.switch");
        if (!StringUtils.isEmpty(flag)) {
            return flag.trim().equalsIgnoreCase("true");
        }

        return false;
    }

    public static boolean onlyEvmPlugin() {
        String flag = System.getenv("watcher.process.only.evmplugin");
        if (!StringUtils.isEmpty(flag)) {
            return flag.trim().equalsIgnoreCase("true");
        }

        return false;
    }
}
