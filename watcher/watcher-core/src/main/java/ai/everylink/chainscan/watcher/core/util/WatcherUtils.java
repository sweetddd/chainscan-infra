package ai.everylink.chainscan.watcher.core.util;

import ai.everylink.chainscan.watcher.core.config.EvmConfig;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.quartz.CronExpression;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utils
 *
 * @author: david.zhanghui@everylink.ai
 */
@Slf4j
public final class WatcherUtils {

    private static Map<String, Object> CONFIG_MAP = new ConcurrentHashMap<>();
    public final static String TRANSFER_CONTRACT_ADDRESS = "watcher.nft.transfer.address";
    public final static String MONITOR_SELECT_TRANSACTION_LOG = "watcher.monitor.select.transaction.log";


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

    public static boolean isFinalizedStatus() {
        String flag = System.getenv("watcher.finalized.status.switch");
        if (!StringUtils.isEmpty(flag)) {
            return flag.trim().equalsIgnoreCase("true");
        }

        return false;
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

    public static <T> T getConfigValue(Environment environment, String configName, Class<T> clazz) {
        if (CONFIG_MAP.get(configName) == null) {
            synchronized (WatcherUtils.class) {
                if (CONFIG_MAP.get(configName) == null) {
                    String configValue = environment.getProperty(configName);
                    if (configValue != null) {
                        log.info("getConfigValue, configName:{}, configValue:{}", configName, configValue);
                        CONFIG_MAP.put(configName, configValue);
                    }
                }
            }
        }
        return clazz.cast(CONFIG_MAP.get(configName));
    }

    public static List<String> getConfigValues(Environment environment, String configName) {
        if (CONFIG_MAP.get(configName) == null) {
            synchronized (WatcherUtils.class) {
                if (CONFIG_MAP.get(configName) == null) {
                    String configValue = environment.getProperty(configName);
                    if (StrUtil.isNotBlank(configValue)) {
                        log.info("getConfigValues, configName:{}, configValue:{}", configName, configValue);
                        CONFIG_MAP.put(configName, Arrays.asList(configValue.split(",")));
                    }
                }
            }
        }
        return (List<String>) CONFIG_MAP.get(configName);
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

    public static Integer getChainMonitorThreshold() {
        String str = System.getenv("watcher.monitor.threshold");
        if (!StringUtils.isEmpty(str)) {
            try {
                return Integer.parseInt(str);
            } catch (Exception e) {
                // ignore
            }
        }

        // default 1 minute
        return 1;
    }

    public static boolean isEthereum(Integer chainId) {
        return chainId != null && (chainId == 4 || chainId == 1);
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
