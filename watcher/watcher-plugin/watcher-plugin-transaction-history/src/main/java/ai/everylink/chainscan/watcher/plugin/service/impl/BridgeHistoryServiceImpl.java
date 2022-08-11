package ai.everylink.chainscan.watcher.plugin.service.impl;

import ai.everylink.chainscan.watcher.core.config.DataSourceEnum;
import ai.everylink.chainscan.watcher.core.config.TargetDataSource;
import ai.everylink.chainscan.watcher.core.util.DecodUtils;
import ai.everylink.chainscan.watcher.dao.TransactionLogDao;
import ai.everylink.chainscan.watcher.dao.WalletTranactionHistoryDao;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.entity.TransactionLog;
import ai.everylink.chainscan.watcher.entity.WalletTransactionHistory;
import ai.everylink.chainscan.watcher.plugin.service.BridgeHistoryService;
import com.alibaba.fastjson.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
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
    private WalletTranactionHistoryDao wTxHistoryDao;

    @Autowired
    private TransactionLogDao transactionLogDao;




    @Override
    @TargetDataSource(value = DataSourceEnum.chainscan)
    public List<TransactionLog> txLog(String transactionHash) {
        List<TransactionLog> logs  = transactionLogDao.findByTxHash(transactionHash);

        return logs;

    }

    @Override
    @TargetDataSource(value = DataSourceEnum.wallet)
    public void depositBridge(Transaction transaction,List<Log> transactionLogs) {
        String transactionHash = transaction.getTransactionHash();
        int txSatte = Integer.parseInt(transaction.getStatus().replace("0x", ""), 16);
        WalletTransactionHistory walletTxHistory = new WalletTransactionHistory();
        walletTxHistory.setFromTxHash(transactionHash);
        walletTxHistory.setFromAddress(transaction.getFromAddr());
        Example<WalletTransactionHistory> exp       = Example.of(walletTxHistory);
        List<WalletTransactionHistory>   txHistorys = wTxHistoryDao.findAll(exp);
       // List<Log> logs = data.getTransactionLogMap().get(transactionHash);
        List<Log> logs  = transactionLogs;
        log.info("deposit bridge ,logs size is [{}],tx history size is [{}]",logs.size(),txHistorys.size());

        for (WalletTransactionHistory txHistory : txHistorys) {
            txHistory.setFromTxState(txSatte);
            txHistory.setFromTxTime(new Timestamp(transaction.getTxTimestamp().getTime()));
            txHistory.setConfirmBlock(new BigInteger("0"));
            txHistory.setSubmitBlock(new BigInteger(transaction.getBlockNumber().toString()));
            if(logs!= null && logs.size() ==3 ){
                List<String> topics1 = logs.get(2).getTopics();
                log.info("log is [{}]",topics1);

                String    topicData    = topics1.get(3);
                Integer depositNonce = Integer.parseInt(topicData.replace("0x", ""), 16);
                txHistory.setFromDepositNonce(depositNonce);
            }
            if(txSatte == 1){
                //txHistory.setTxState("From Chain Processing (1/12)");
            }else if(txSatte == 0){
                txHistory.setTxState("Failure");
            }
            wTxHistoryDao.updateTxHistory(txHistory);
        }
    }

    @Override
    @TargetDataSource(value = DataSourceEnum.wallet)
    public void bridgeHistoryScan(Transaction transaction) {
        String                   transactionHash = transaction.getTransactionHash();
        String                   input           = transaction.getInput();
        List<String>             params          = DecodUtils.getParams2List(input);
        String                   chainIDStr      = params.get(1).replace("0x", "");
        Integer                  chainID         = Integer.parseInt(chainIDStr, 16);
        Integer                  depositNonce    = Integer.parseInt(params.get(2).replace("0x", ""), 16);


        if(chainID > 256){
            chainIDStr      = params.get(2).replace("0x", "");
            chainID         = Integer.parseInt(chainIDStr, 16);
            depositNonce    = Integer.parseInt(params.get(3).replace("0x", ""), 16);
        }
        log.info("bridge chain id is [{}],depoosit nonce is {[]}",chainID,depositNonce);
        WalletTransactionHistory txHistory = wTxHistoryDao.findByChainNonce(chainID,depositNonce);
        int txSatte = Integer.parseInt(transaction.getStatus().replace("0x", ""), 16);
        if(txHistory != null){
            txHistory.setToTxState(txSatte);
            txHistory.setToTxTime(new Timestamp(transaction.getTxTimestamp().getTime()));
            txHistory.setToTxHash(transactionHash);
            if(txSatte == 1){
                txHistory.setTxState("To Chain Processing (1/12)");
            }else if(txSatte == 0){
                txHistory.setTxState("Failure");
            }
            txHistory.setConfirmBlock(BigInteger.ZERO);
            wTxHistoryDao.updateTxToHistory(txHistory);
        }else {
        }

    }
}
