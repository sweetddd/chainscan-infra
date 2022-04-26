package ai.everylink.chainscan.watcher.plugin.service.impl;

import ai.everylink.chainscan.watcher.dao.TokenInfoDao;
import ai.everylink.chainscan.watcher.dao.TransactionDao;
import ai.everylink.chainscan.watcher.entity.TokenInfo;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public void updateTokenTag() {
        TokenInfo   queryInfo = tokenInfoDao.selectByTokenName("txTag");
        String      address     = queryInfo.getAddress();
        long        stratId    = Long.parseLong(address);
        tokenInfoDao.updateAddress((stratId+100L) + "" ,queryInfo.getId());
    }

    @Override
    public List<Transaction> getTxData() {
        TokenInfo   queryInfo = tokenInfoDao.selectByTokenName("txTag");
        if(queryInfo == null){
            TokenInfo tokenInfo = new TokenInfo();
            tokenInfo.setTokenName("txTag");
            tokenInfo.setAddress(1L+"");
            tokenInfo.setTokenType(0);
            tokenInfo.setCreateTime(new Date());
            tokenInfoDao.save(tokenInfo);
            return transactionDao.getTxData(1L,100L);
        }
        String      address     = queryInfo.getAddress();
        long        stratId    = Long.parseLong(address);
        long        endId    = stratId + 100;
        return transactionDao.getTxData(stratId,endId);
    }
}















