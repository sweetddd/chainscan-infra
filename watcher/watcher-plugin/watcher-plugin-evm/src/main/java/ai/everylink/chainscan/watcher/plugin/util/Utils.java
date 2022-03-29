package ai.everylink.chainscan.watcher.plugin.util;

import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.plugin.EvmWatcher;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Utils
 *
 * @author: david.zhanghui@everylink.ai
 */
public final class Utils {
    private static Logger logger = LoggerFactory.getLogger(Utils.class);

    public static Long getNetworkBlockHeight(Logger logger, Web3j web3j) {
        try {
            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            return blockNumber.getBlockNumber().longValue();
        } catch (Throwable e) {
            logger.error("Error occured when request web3j.ethBlockNumber.", e);
            return 0L;
        }
    }


    public static class ScanChainThread implements Runnable {
        private CountDownLatch latch;
        private EvmWatcher watcher;
        public ScanChainThread(CountDownLatch latch, EvmWatcher watcher) {
            this.latch = latch;
            this.watcher = watcher;
        }

        @Override
        public void run() {
            try {
                watcher.scanChain();
            } catch (Exception e) {
                logger.error("[EvmWatcher]ScanChainThread scanChain error", e);
            } finally {
                latch.countDown();
            }
        }
    }

    public static class ListBlockThread implements Runnable {
        private CountDownLatch latch;
        private EvmWatcher watcher;
        private List<EvmData> list;
        public ListBlockThread(CountDownLatch latch, EvmWatcher watcher, List<EvmData> list) {
            this.latch = latch;
            this.watcher = watcher;
            this.list = list;
        }

        @Override
        public void run() {
            try {
                List<EvmData> tmpList = watcher.listBlock();
                if (CollectionUtils.isEmpty(tmpList)) {
                    logger.info("[EvmWatcher]no blocks found.");
                    return;
                }

                list.addAll(tmpList);
            } catch (Exception e) {
                logger.error("[EvmWatcher]ListBlockThread scanChain error", e);
            } finally {
                latch.countDown();
            }
        }
    }


}
