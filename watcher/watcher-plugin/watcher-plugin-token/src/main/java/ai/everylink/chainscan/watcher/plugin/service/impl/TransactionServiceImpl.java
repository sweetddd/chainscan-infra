package ai.everylink.chainscan.watcher.plugin.service.impl;

import ai.everylink.chainscan.watcher.dao.TokenInfoDao;
import ai.everylink.chainscan.watcher.dao.TransactionDao;
import ai.everylink.chainscan.watcher.entity.TokenInfo;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author Brett
 * @Description
 * @Date 2022/4/26 2:44 下午
 **/
@Slf4j
@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionDao transactionDao;

    @Autowired
    private TokenInfoDao tokenInfoDao;


    @Override
    public void updateTokenTag(String jobName, int size) {
        TokenInfo  queryInfo = tokenInfoDao.selectByTokenName(jobName);
        String      address  = queryInfo.getAddress();
        BigInteger  decimals = queryInfo.getDecimals();
        BigInteger  count = decimals.add(new BigInteger(size + ""));
        long        stratId    = Long.parseLong(address);
        tokenInfoDao.updateAddress((stratId+100L) + "" ,count,queryInfo.getId());
    }

    @Override
    public List<Transaction> getTxData(String jobName,String jobIndex) {
        long  endIndex = Long.parseLong(jobIndex);
        TokenInfo   queryInfo = tokenInfoDao.selectByTokenName(jobName);
        if(queryInfo == null){
            TokenInfo tokenInfo = new TokenInfo();
            tokenInfo.setTokenName(jobName);
            tokenInfo.setTokenSymbol(jobName);
            endIndex = endIndex - 27856750L;
            tokenInfo.setAddress(endIndex+"");
            tokenInfo.setTokenType(0);
            tokenInfo.setDecimals(BigInteger.ZERO);
            tokenInfo.setCreateTime(new Date());
            tokenInfoDao.save(tokenInfo);
            return transactionDao.getTxData(1L,100L);
        }
        String      address     = queryInfo.getAddress();
        long        stratId    = Long.parseLong(address);
        if(stratId >= endIndex){
            log.info(jobName + "加载数据任务完成!");
            return new ArrayList<>();
        }
        long        endId    = stratId + 100;
        return transactionDao.getTxData(stratId,endId);
    }
}















