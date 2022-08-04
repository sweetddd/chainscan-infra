package ai.everylink.chainscan.watcher.plugin.service;

import ai.everylink.chainscan.watcher.entity.Transaction;

import java.math.BigInteger;
import java.util.List;

/**
 * @Author apple
 * @Description
 * @Date 2022/4/26 2:44 下午
 **/
public interface TransactionService {

    /**
     * 更新标记;
     * @param jobName
     * @param size
     */
    void updateTokenTag(String jobName, int size);

    /**
     * 更新标记;
     */
    void updateProcessingCursor(BigInteger blockNumber);

    /**
     * 加载交易数据
     * @param jobName
     * @param jobIndex
     * @return
     */
    List<Transaction> getTxData(String jobName,String jobIndex);

    /**
     * 加载交易数据
     * @return
     */
    List<Transaction> getTxData();
}
