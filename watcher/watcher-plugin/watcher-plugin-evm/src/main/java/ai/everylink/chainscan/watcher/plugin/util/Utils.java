package ai.everylink.chainscan.watcher.plugin.util;

import ai.everylink.chainscan.watcher.core.vo.EvmData;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.springframework.util.CollectionUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Transaction;

import java.util.List;

/**
 * Utils
 *
 * @author: david.zhanghui@everylink.ai
 */
public final class Utils {
    private Utils(){}

    public static List<EvmData> replayBlock(Long startBlockNumber, Long endBlockNumber,
                                     Logger logger, Web3j web3j, Integer chainId) throws Exception {
        List<EvmData> dataList = Lists.newArrayList();

        for (Long blockHeight = startBlockNumber; blockHeight <= endBlockNumber; blockHeight++) {
            logger.info("Begin to scan block={}", blockHeight);

            EvmData data = new EvmData();
            data.setChainId(chainId);

            // 查询block
            EthBlock block = web3j.ethGetBlockByNumber(
                    new DefaultBlockParameterNumber(blockHeight), true).send();
            if (block == null || block.getBlock() == null) {
                logger.error("Block is null. block={}", blockHeight);
                continue;
            }

            data.setBlock(block.getBlock());
            dataList.add(data);
            if (CollectionUtils.isEmpty(block.getBlock().getTransactions())) {
                logger.info("No transactions found. block={}", blockHeight);
                continue;
            }

            // 交易列表
            logger.info("Found txs.block={},count={}", blockHeight, block.getBlock().getTransactions().size());
            for (EthBlock.TransactionResult transactionResult : block.getBlock().getTransactions()) {
                Transaction tx = ((EthBlock.TransactionObject) transactionResult).get();
                if (tx.getInput() == null || tx.getInput().length() < 138) {
                    logger.info("No logs.block={},tx={}", blockHeight, tx.getHash());
                    continue;
                }

                // 获取Logs
                EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(tx.getHash()).send();
                if (receipt.getResult() != null && receipt.getResult().getLogs() != null) {
                    logger.info("Found logs.block={},tx={},count={}",
                            blockHeight, tx.getHash(), receipt.getResult().getLogs().size());
                    data.getTransactionLogMap().put(tx.getHash(), receipt.getResult().getLogs());
                }
            }
        }

        return dataList;
    }

    public static EvmData replayOneBlock(Long blockNumber, Logger logger, Web3j web3j, Integer chainId) throws Exception {
        logger.info("Begin to scan block={}", blockNumber);
        EvmData data = new EvmData();
        data.setChainId(chainId);

        // 查询block
        EthBlock block = web3j.ethGetBlockByNumber(
                new DefaultBlockParameterNumber(blockNumber), true).send();
        if (block == null || block.getBlock() == null) {
            logger.error("Block is null. block={}", blockNumber);
            return null;
        }

        data.setBlock(block.getBlock());
        if (CollectionUtils.isEmpty(block.getBlock().getTransactions())) {
            logger.info("No transactions found. block={}", blockNumber);
            return data;
        }

        // 交易列表
        logger.info("Found txs.block={},count={}", blockNumber, block.getBlock().getTransactions().size());
        for (EthBlock.TransactionResult transactionResult : block.getBlock().getTransactions()) {
            Transaction tx = ((EthBlock.TransactionObject) transactionResult).get();
            if (tx.getInput() == null || tx.getInput().length() < 138) {
                logger.info("No logs.block={},tx={}", blockNumber, tx.getHash());
                continue;
            }

            // 获取Logs
            EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(tx.getHash()).send();
            if (receipt.getResult() != null && receipt.getResult().getLogs() != null) {
                logger.info("Found logs.block={},tx={},count={}",
                        blockNumber, tx.getHash(), receipt.getResult().getLogs().size());
                data.getTransactionLogMap().put(tx.getHash(), receipt.getResult().getLogs());
            }
        }

        return data;
    }

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
