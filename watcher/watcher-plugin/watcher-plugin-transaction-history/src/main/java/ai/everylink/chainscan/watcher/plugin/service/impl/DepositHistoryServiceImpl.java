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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

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


    @Autowired
    Environment environment;

    @Override
    @TargetDataSource(value = DataSourceEnum.wallet)
    public void depositERC20HistoryScan(Transaction transaction){
        depositERC20HistoryScanRetry(transaction, 0);
    }

    public void depositERC20HistoryScanRetry(Transaction transaction, int retryCount) {
        String  transactionHash = transaction.getTransactionHash();
        int txSatte = Integer.parseInt(transaction.getStatus().replace("0x", ""), 16);
        WalletTransactionHistory txHistory  = wTxHistoryDao.findByAddTxHash(transaction.getFromAddr(), transactionHash);
        log.info("txHistory:params" + transaction.getFromAddr() + ",Hash=" + transactionHash);
        log.info("txHistory" + txHistory);
        if(txHistory != null){
            txHistory.setFromTxState(txSatte);
            txHistory.setFromTxTime(new Timestamp(transaction.getTxTimestamp().getTime()));
            txHistory.setConfirmBlock(new BigInteger("0"));
            txHistory.setSubmitBlock(new BigInteger(transaction.getBlockNumber().toString()));
            if(txSatte == 1){
                Integer confirmBlock = Integer.valueOf(environment.getProperty("watcher.confirm.block"));

                txHistory.setTxState("L1 Depositing (1/"+confirmBlock+")");
            }else if(txSatte == 0){
                txHistory.setTxState("Failure");
            }
            log.error("扫描到deposit 二层的记录，状态是[{}]",txHistory);
            wTxHistoryDao.updateTxHistory(txHistory);
        }else {
            retryCount = retryCount + 1;
            if(retryCount <= 30) {
                log.error("txhistory-plugin(depositERC20HistoryScan):There is no such data, 重试第" + retryCount + "次:" + transactionHash);
                try {
                    TimeUnit.SECONDS.sleep(1L);
                } catch (Exception ignored){}
                depositERC20HistoryScanRetry(transaction, retryCount);
            }
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
                Integer confirmBlock = Integer.valueOf(environment.getProperty("watcher.confirm.block"));

                txHistory.setTxState("L1 Depositing (1/"+confirmBlock+")");
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
