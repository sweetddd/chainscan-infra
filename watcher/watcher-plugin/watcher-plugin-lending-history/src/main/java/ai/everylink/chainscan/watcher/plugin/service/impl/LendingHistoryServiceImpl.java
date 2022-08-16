package ai.everylink.chainscan.watcher.plugin.service.impl;

import ai.everylink.chainscan.watcher.core.config.DataSourceEnum;
import ai.everylink.chainscan.watcher.core.config.TargetDataSource;
import ai.everylink.chainscan.watcher.core.util.DecodUtils;
import ai.everylink.chainscan.watcher.core.util.OkHttpUtil;
import ai.everylink.chainscan.watcher.core.util.VM30Utils;
import ai.everylink.chainscan.watcher.core.util.WatcherUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.dao.WalletTranactionHistoryDao;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.entity.TransactionLog;
import ai.everylink.chainscan.watcher.entity.WalletTransactionHistory;
import ai.everylink.chainscan.watcher.plugin.constant.LendingTopicEnum;
import ai.everylink.chainscan.watcher.plugin.service.LendingHistoryService;
import ai.everylink.chainscan.watcher.plugin.utils.LendingContractUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Service
public class LendingHistoryServiceImpl implements LendingHistoryService {


    @Resource
    private LendingContractUtils lendingContractUtils;
    @Resource
    private WalletTranactionHistoryDao walletTranactionHistoryDao;


    private Web3j web3j;

    @Autowired
    private VM30Utils vm30Utils;

    @Override
    @TargetDataSource(value = DataSourceEnum.wallet)
    public void transactionHistoryScan(EvmData data) {



        Map<String, List<Log>> transactionLogMap = data.getTransactionLogMap();

        Map<String, TransactionReceipt> txList = data.getTxList();
        for(String transactionHash : txList.keySet()){
            TransactionReceipt transactionReceipt = txList.get(transactionHash);
            String to = transactionReceipt.getTo();
            if(StringUtils.isEmpty(to)){
                continue;
            }
            String symbol = lendingContractUtils.containsContract(to);
            if(StringUtils.isNotEmpty(symbol)){
                //lending
                List<Log> logs = transactionLogMap.get(transactionHash);
                lendingEvent(logs,true,transactionHash,to,data.getChainId(),symbol,data.getBlock().getNumber(),data.getBlock().getTimestamp());
            }

            String stableSymbol = lendingContractUtils.stableContains(to);

            if(StringUtils.isNotEmpty(stableSymbol)){
                //lending
                List<Log> logs = transactionLogMap.get(transactionHash);
                lendingEvent(logs,false,transactionHash,to,data.getChainId(),stableSymbol,data.getBlock().getNumber(),data.getBlock().getTimestamp());
            }

        }
    }

    private void lendingEvent(List<Log> logs,boolean isLending,String txHash,String contractAddress,Integer chainId,String symbol,
    BigInteger blockNumber,BigInteger time ){
        for(Log log : logs){
            List<String> topics = log.getTopics();
            if(!CollectionUtils.isEmpty(topics)){
                String s = topics.get(0);
                if(StringUtils.isNotEmpty(s)){
                    String logData = log.getData();
                    List<String> strings = analysisData(logData);

                    if(strings.size() >= 3){
                        String address = strings.get(0);
                        address = "0x"+address.substring(address.length()-40);
                        String amountData = strings.get(1);
                        BigInteger amount = new BigInteger(amountData.replace("0x", ""),16);           // 16进制转成大数类型
                        if(s.equals(LendingTopicEnum.Supply.getValue())){
                            saveHistory(LendingTopicEnum.Supply.name()
                            ,"-"
                            ,address
                            ,txHash
                            ,chainId
                            ,amount
                            ,contractAddress
                            ,symbol
                            ,blockNumber
                            ,new Timestamp(time.longValue()*1000)
                            );
                        }else if(s.equals(LendingTopicEnum.Withdraw.getValue())){
                            saveHistory(LendingTopicEnum.Withdraw.name()
                                    ,"+"
                                    ,address
                                    ,txHash
                                    ,chainId
                                    ,amount
                                    ,contractAddress
                                    ,symbol
                                    ,blockNumber
                                    ,new Timestamp(time.longValue()*1000)
                            );
                        }else if(s.equals(LendingTopicEnum.Repay.getValue())){
                            amountData = strings.get(2);
                            amount = new BigInteger(amountData.replace("0x", ""),16);
                            String type = LendingTopicEnum.Repay.name();

                            if(!isLending){
                                type = LendingTopicEnum.Burnt.name();
                            }
                            saveHistory(type
                                    ,"+"
                                    ,address
                                    ,txHash
                                    ,chainId
                                    ,amount
                                    ,contractAddress
                                    ,symbol
                                    ,blockNumber
                                    ,new Timestamp(time.longValue()*1000)
                            );
                        }else if(s.equals(LendingTopicEnum.Borrow.getValue())){
                            String type = LendingTopicEnum.Borrow.name();

                            if(!isLending){
                                type = LendingTopicEnum.Mint.name();
                            }
                            saveHistory(type
                                    ,"-"
                                    ,address
                                    ,txHash
                                    ,chainId
                                    ,amount
                                    ,contractAddress
                                    ,symbol
                                    ,blockNumber
                                    ,new Timestamp(time.longValue()*1000)
                            );
                        }

                    }


                }
            }
        }
    }

    private void saveHistory(String type,String actionSymbol,String address,String txHash,Integer chainId,BigInteger amount,String contractAddress,String symbol
    ,BigInteger blockNumber,Timestamp fromTxTime){

        WalletTransactionHistory history = new WalletTransactionHistory();
        history.setType(type);
        history.setTxState("Pending");
        history.setTokenType("ERC20");
        history.setActionSymbol(actionSymbol);
        history.setSymbol(symbol);
        history.setConfirmBlock(BigInteger.ZERO);
        history.setFromAddress(address);
        history.setFromTxHash(txHash);
        history.setToAddress(address);
        history.setTokenAddress(contractAddress);
        history.setFromChainId(chainId);
        history.setAmount(fromWei(contractAddress,amount));
        history.setLayer("L1");
        history.setCreateTime(new Timestamp(new Date().getTime()));
        history.setSubmitBlock(blockNumber);
        history.setFromTxTime(fromTxTime);
        walletTranactionHistoryDao.save(history);

    }

    /**
     * 初始化web3j
     */
    @PostConstruct
    private void initWeb3j() {
        if (web3j != null) {
            return;
        }
        try {
            String rpcUrl = WatcherUtils.getVmChainUrl();
            log.info("[rpc_url]url=" + rpcUrl);
            OkHttpClient httpClient  = OkHttpUtil.buildOkHttpClient();
            HttpService httpService = new HttpService(rpcUrl, httpClient, false);
            web3j = Web3j.build(httpService);
        } catch (Exception e) {
            log.error("初始化web3j异常", e);
        }
    }
    private BigDecimal fromWei(String contractAddress, BigInteger amount){

        BigDecimal total = new BigDecimal(amount);
        try {
            String address = vm30Utils.underlying(web3j, contractAddress);
            int decimals = 18;
            if(!StringUtils.isEmpty(address)){
                BigInteger bigInteger = vm30Utils.decimals(web3j, address);
                if(null == bigInteger){
                    return total;
                }
                decimals = bigInteger.intValue();
            }

            total = total.divide(new BigDecimal(10).pow(decimals));
        }catch (Exception e){
            log.error("获取decimal 异常,[{}]",e);
        }

        return total;




    }

    private void withdrawEvent(List<String> dataList){
        if(dataList.size() >= 3){
            String address = dataList.get(0);
            String amountData = dataList.get(1);
            BigInteger amount = new BigInteger(amountData.replace("0x", ""),16);           // 16进制转成大数类型
        }
    }

    private void borrowEvent(List<String> dataList){
        if(dataList.size() >= 3){
            String address = dataList.get(0);
            String amountData = dataList.get(1);
            BigInteger amount = new BigInteger(amountData.replace("0x", ""),16);           // 16进制转成大数类型
        }
    }

    private void repayEvent(List<String> dataList){
        if(dataList.size() >= 3){
            String address = dataList.get(0);
            String amountData = dataList.get(2);
            BigInteger amount = new BigInteger(amountData.replace("0x", ""),16);           // 16进制转成大数类型
        }
    }



    public List<String> analysisData(String data){

        if(StringUtils.isEmpty(data) || data.length() <= 2){
            return new ArrayList<>();
        }
        data = data.substring(2,data.length());

        if((data.length()) % 64 !=0){
            return new ArrayList<>();
        }

        int total = (data.length()+1)/64;
        List<String> list = new ArrayList<>();
        for(int i = 0 ; i< total; i++){
            list.add(data.substring(i*64,i*64+64));
        }
        return list;

    }
}
