package ai.everylink.chainscan.watcher.plugin.service;

import ai.everylink.chainscan.watcher.core.vo.EvmData;

public interface LendingHistoryService {

    /**
     *transaction 信息扫描
     */
    void transactionHistoryScan(EvmData blockData);
}
