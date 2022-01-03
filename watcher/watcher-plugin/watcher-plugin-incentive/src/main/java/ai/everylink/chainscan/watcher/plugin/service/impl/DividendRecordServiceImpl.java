package ai.everylink.chainscan.watcher.plugin.service.impl;

import ai.everylink.chainscan.watcher.dao.DividendRecordDao;
import ai.everylink.chainscan.watcher.entity.DividendRecord;
import ai.everylink.chainscan.watcher.plugin.service.DividendRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 挖矿详情数据记录Service业务层处理
 *
 * @author brett
 * @date 2021-10-15
 */
@Service
public class DividendRecordServiceImpl implements DividendRecordService {

    @Autowired
    private DividendRecordDao walletMiningDetailsMapper;

    @Override
    public List<DividendRecord> findAll() {
        return walletMiningDetailsMapper.findAll();
    }

    @Override
    public void save(DividendRecord walletMiningDetails) {
        walletMiningDetailsMapper.save(walletMiningDetails);
    }

    @Override
    public DividendRecord dividendOverview(Long startIndex, Long endIndex) {
        return new DividendRecord();
    }
}
