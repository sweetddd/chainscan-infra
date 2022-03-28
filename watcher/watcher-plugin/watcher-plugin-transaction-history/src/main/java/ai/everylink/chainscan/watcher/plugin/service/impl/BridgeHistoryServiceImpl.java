package ai.everylink.chainscan.watcher.plugin.service.impl;

import ai.everylink.chainscan.watcher.core.util.DecodUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.dao.BridgeHistoryDao;
import ai.everylink.chainscan.watcher.dao.BridgeResourceDao;
import ai.everylink.chainscan.watcher.dao.WalletTranactionHistoryDao;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.entity.WalletTransactionHistory;
import ai.everylink.chainscan.watcher.plugin.service.BridgeHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Log;

import java.sql.Timestamp;
import java.util.List;

/**
 * @Author brett
 * @Description
 * @Date 2022/3/26 6:54 下午
 **/
@Slf4j
@Service
public class BridgeHistoryServiceImpl implements BridgeHistoryService {

    @Autowired
    private BridgeHistoryDao bridgeHistoryDao;

    @Autowired
    private BridgeResourceDao bridgeResourceDao;

    @Autowired
    private WalletTranactionHistoryDao wTxHistoryDao;

    @Override
    public void depositBridge(Transaction transaction, EvmData data) {
        String transactionHash = transaction.getTransactionHash();
        int txSatte = Integer.parseInt(transaction.getStatus().replace("0x", ""), 16);
        WalletTransactionHistory walletTxHistory = new WalletTransactionHistory();
        walletTxHistory.setFromTxHash(transactionHash);
        walletTxHistory.setFromAddress(transaction.getFromAddr());
        Example<WalletTransactionHistory> exp       = Example.of(walletTxHistory);
        List<WalletTransactionHistory>   txHistorys = wTxHistoryDao.findAll(exp);

        List<Log> logs = data.getTransactionLogMap().get(transactionHash);
        for (WalletTransactionHistory txHistory : txHistorys) {
            txHistory.setFromTxState(txSatte);
            txHistory.setFromTxTime(new Timestamp(transaction.getTxTimestamp().getTime()));
            txHistory.setConfirmBlock(data.getBlock().getNumber());
            if(logs.size() ==3){
                String    topicData    = logs.get(2).getTopics().get(3);
                Integer depositNonce = Integer.parseInt(topicData.replace("0x", ""), 16);
                txHistory.setFromDepositNonce(depositNonce);
            }
            txHistory.setTxState("");
            wTxHistoryDao.updateTxHistory(txHistory);
        }
    }

    @Override
    public void bridgeHistoryScan(Transaction transaction, EvmData data) {
        String                   transactionHash = transaction.getTransactionHash();
        String                   input           = transaction.getInput();
        List<String>             params          = DecodUtils.getParams2List(input);
        String                   chainIDStr      = params.get(1).replace("0x", "");
        Integer                  chainID         = Integer.parseInt(chainIDStr, 16);
        Integer                  depositNonce    = Integer.parseInt(params.get(2).replace("0x", ""), 16);
        WalletTransactionHistory txHistory = wTxHistoryDao.findByChainNonce(chainID,depositNonce);
        int txSatte = Integer.parseInt(transaction.getStatus().replace("0x", ""), 16);
        txHistory.setToTxState(txSatte);
        txHistory.setToTxTime(new Timestamp(transaction.getTxTimestamp().getTime()));
        txHistory.setToTxHash(transactionHash);
        if(txSatte ==0){
            txHistory.setTxState("Processing");
        }
        wTxHistoryDao.updateTxToHistory(txHistory);
    }
}