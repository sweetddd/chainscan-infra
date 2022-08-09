package ai.everylink.chainscan.watcher.plugin.service.impl;

import ai.everylink.chainscan.watcher.core.config.DataSourceEnum;
import ai.everylink.chainscan.watcher.core.config.TargetDataSource;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.dao.WalletTranactionHistoryDao;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.entity.WalletTransactionHistory;
import ai.everylink.chainscan.watcher.plugin.service.DepositHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.sql.Timestamp;

/**
 * @Author brett
 * @Description
 * @Date 2022/3/26 6:54 下午
 **/
@Slf4j
@Service
public class DepositHistoryServiceImpl implements DepositHistoryService {


    @Autowired
    private WalletTranactionHistoryDao wTxHistoryDao;

    @Override
    @TargetDataSource(value = DataSourceEnum.wallet)
    public void depositERC20HistoryScan(Transaction transaction) {
        String  transactionHash = transaction.getTransactionHash();
        int txSatte = Integer.parseInt(transaction.getStatus().replace("0x", ""), 16);
        WalletTransactionHistory txHistory  = wTxHistoryDao.findByAddTxHash(transaction.getFromAddr(), transactionHash);
        log.info("txHistory:params" + transaction.getFromAddr() + ",Hash=" + transactionHash);
        log.info("txHistory" + txHistory.toString());
        if(txHistory != null){
            txHistory.setFromTxState(txSatte);
            txHistory.setFromTxTime(new Timestamp(transaction.getTxTimestamp().getTime()));
            txHistory.setConfirmBlock(new BigInteger("0"));
            txHistory.setSubmitBlock(new BigInteger(transaction.getBlockNumber().toString()));
            if(txSatte == 1){
                txHistory.setTxState("L1 Depositing (1/12)");
            }else if(txSatte == 0){
                txHistory.setTxState("Failure");
            }
            wTxHistoryDao.updateTxHistory(txHistory);
        }else {
            log.error("txhistory-plugin(depositERC20HistoryScan):There is no such data:" + transactionHash);
        }
    }

    @Override
    @TargetDataSource(value = DataSourceEnum.wallet)
    public void depositNativeTokenHistoryScan(Transaction transaction) {
        String  transactionHash = transaction.getTransactionHash();
        int txSatte = Integer.parseInt(transaction.getStatus().replace("0x", ""), 16);
        WalletTransactionHistory txHistory  = wTxHistoryDao.findByAddTxHash(transaction.getFromAddr(), transactionHash);
        if(txHistory != null){
            txHistory.setFromTxState(txSatte);
            txHistory.setFromTxTime(new Timestamp(transaction.getTxTimestamp().getTime()));
            txHistory.setConfirmBlock(new BigInteger("0"));
            txHistory.setSubmitBlock(new BigInteger(transaction.getBlockNumber().toString()));
            if(txSatte == 1){
                txHistory.setTxState("L1 Depositing (1/12)");
            }else if(txSatte == 0){
                txHistory.setTxState("Failure");
            }
            wTxHistoryDao.updateTxHistory(txHistory);
        }else {
            log.error("txhistory-plugin(depositNativeTokenHistoryScan):There is no such data:" + transactionHash);
        }
    }

    public static void main(String[] args) {
        String s = "0000000000000000000000006da573eec80f63c98b88ced15d32ca270787fb5a";
        String substring = s.substring(s.length() - 40);
        System.out.println(substring);
    }

}
