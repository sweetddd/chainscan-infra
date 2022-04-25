package ai.everylink.chainscan.watcher.core.util;

import ai.everylink.chainscan.watcher.core.config.EvmConfig;
import org.apache.commons.io.IOUtils;
import org.quartz.CronExpression;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

    public static String dbUrl() {
        String val = System.getenv("spring.datasource.chainscan.jdbc-url");
        if (StringUtils.isEmpty(val)) {
            return "jdbc:mysql://rinkeby-mysql8.database.svc.cluster.local:3306/chainscan_rinkeby?characterEncoding=utf-8&serverTimezone=UTC&useSSL=false";
        }

        return val;
    }

    public static String dbUserName() {
        String val = System.getenv("spring.datasource.chainscan.username");
        if (StringUtils.isEmpty(val)) {
            return "root";
        }

        return val;
    }

    public static String dbPassword() {
        String val = System.getenv("spring.datasource.chainscan.password");
        if (StringUtils.isEmpty(val)) {
            return "ZxcvAsdf";
        }

        return val;
    }

    public static String getChainMonitorCron() {
        String cronStr = System.getenv("watcher.monitor.cron");
        if (StringUtils.isEmpty(cronStr)) {
            return null;
        }
        try {
            if (CronExpression.isValidExpression(cronStr)) {
                return cronStr;
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    public static boolean isEthereum(Integer chainId) {
        return chainId != null && (chainId == 4 || chainId ==1);
    }

    public static InputStream str2Stream(String str) throws IOException {
        if (str == null) {
            str = "";
        }
        return IOUtils.toInputStream(str, StandardCharsets.UTF_8.name());
    }

    public static String stream2Str(InputStream stream) throws IOException {
        return IOUtils.toString(stream, StandardCharsets.UTF_8.name());
    }
}
