package ai.everylink.chainscan.watcher.plugin.service;

import ai.everylink.chainscan.watcher.entity.Transaction;

import java.util.List;

/**
 * @Author apple
 * @Description
 * @Date 2022/4/26 2:44 下午
 **/
public interface TransactionService {

    /**
     * 更新标记;
     * @param id
     */
    void updateTokenTag();

    /**
     * 加载交易数据
     * @return
     */
    List<Transaction> getTxData();
}
