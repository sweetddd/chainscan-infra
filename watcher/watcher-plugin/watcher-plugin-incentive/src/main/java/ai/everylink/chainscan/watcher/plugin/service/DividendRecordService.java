package ai.everylink.chainscan.watcher.plugin.service;



import ai.everylink.chainscan.watcher.plugin.entity.DividendRecord;

import java.util.List;

/**
 * 挖矿详情数据记录Service接口
 *
 * @author brett
 * @date 2021-10-15
 */
public interface DividendRecordService {

    List<DividendRecord> findAll();

    void save(DividendRecord walletMiningDetails);

    DividendRecord dividendOverview(Long startIndex, Long endIndex);

}
