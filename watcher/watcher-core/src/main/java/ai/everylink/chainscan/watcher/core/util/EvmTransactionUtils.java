package ai.everylink.chainscan.watcher.core.util;

import ai.everylink.chainscan.watcher.core.vo.EvmData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

@Slf4j
public class EvmTransactionUtils {

    public static TransactionReceipt replayTx(String txHash, Web3j web3j) throws Exception {

        // 获取receipt
        try {
            EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send();
            if (receipt.getResult() == null) {
                log.warn("[EvmWatcher]tx receipt not found. tx={}",  txHash);
                return null;
            }
            return receipt.getResult();
        } catch (Exception e) {
            log.error("获取 Transaction Receipt 异常：", e);
        }

        return null;
    }



}
