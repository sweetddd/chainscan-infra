package ai.everylink.chainscan.watcher.plugin.service.impl;

import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.service.DepositHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author brett
 * @Description
 * @Date 2022/3/26 6:54 下午
 **/
@Slf4j
@Service
public class DepositHistoryServiceImpl implements DepositHistoryService {


    @Override
    public void depositHistoryScan(Transaction transaction, EvmData data) {

    }

}
