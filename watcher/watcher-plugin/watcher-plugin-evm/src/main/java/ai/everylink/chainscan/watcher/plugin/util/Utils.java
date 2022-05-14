package ai.everylink.chainscan.watcher.plugin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;


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

}
