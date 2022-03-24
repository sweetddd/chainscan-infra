/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.everylink.chainscan.watcher.plugin.service.impl;

import ai.everylink.chainscan.watcher.core.config.DataSourceEnum;
import ai.everylink.chainscan.watcher.core.config.TargetDataSource;
import ai.everylink.chainscan.watcher.core.util.DecodUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.dao.BridgeHistoryDao;
import ai.everylink.chainscan.watcher.dao.BridgeResourceDao;
import ai.everylink.chainscan.watcher.entity.BridgeHistory;
import ai.everylink.chainscan.watcher.entity.BridgeResource;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.service.BridgeHistoryService;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * token统计扫描
 *
 * @author brett
 * @since 2021-12-30
 */
@Slf4j
@Service
public class BridgeHistoryServiceImpl implements BridgeHistoryService {

    private Web3j web3j;

    @Value("${watcher.vmChainUrl:}")
    private String vmChainUrl;



    @Autowired
    private BridgeResourceDao bridgeResourceDao;

    @Autowired
    private BridgeHistoryDao bridgeHistoryDao;

    @Autowired
    Environment environment;


    @PostConstruct
    private void initWeb3j() {
        if (web3j != null) {
            return;
        }
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            builder.writeTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            builder.readTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            OkHttpClient httpClient  = builder.build();
            HttpService  httpService = new HttpService(vmChainUrl, httpClient, false);
            web3j = Web3j.build(httpService);
        } catch (Exception e) {
            log.error("初始化web3j异常", e);
        }
    }

    @Override
    @TargetDataSource(value = DataSourceEnum.bridge)
    public void bridgeHistoryScan(EvmData data) {
        int   chainId = data.getChainId();
        List<Transaction> txList = buildTransactionList(data, chainId);
        // 事件监听 解析;
        for (Transaction transaction : txList) {
            String transactionHash = transaction.getTransactionHash();
            String input           = transaction.getInput();
            if(StringUtils.isNotBlank(input) && input.length() > 74){
                List<String> params = DecodUtils.getParams2List(input);
                String method = params.get(0);
                String chainIDStr = params.get(1).replace("0x", "");
                //发起跨链
                if(params.size() > 2 && (method.contains("0xa44f5fe6") ||   //ERC20
                    method.contains("0xee1c1c7b")||  //原生币
                    method.contains("0xfe4464a7"))){  //NFT
                    Integer chainID = Integer.parseInt(chainIDStr,16);
                    String         resourceID     = "0x" + params.get(2);
                    BridgeHistory  bridgeHistory  = new BridgeHistory();
                    List<Log> los = new ArrayList<>();
                    List<Log> itemList = data.getTransactionLogMap().get(transactionHash);
                    if(itemList.size() > 3 ){
                        los = itemList;
                    }
                    Log log = los.get(2);
                    List<String> topcs     = new ArrayList<>();
                    List<String> topics = log.getTopics();
                    if(topics.size() > 0){
                        topcs = topics;
                    }
                    if(topcs.size() == 4){
                        Integer depositNonce = Integer.parseInt(topcs.get(3).replace("0x",""),16);
                        bridgeHistory.setSrcDepositNonce(depositNonce);
                    }
                    bridgeHistory.setAddress(transaction.getFromAddr());
                    bridgeHistory.setResourceId(resourceID);
                    List<BridgeResource> resources = bridgeResourceDao.findByResourceID(resourceID);
                    if(resources.size() > 0){
                        BridgeResource bridgeResource = resources.get(0);
                        bridgeHistory.setBridgeFee(bridgeResource.getReferBridgeFee());
                        bridgeHistory.setSrcNetwork(bridgeResource.getDestinationNetwork());
                        bridgeHistory.setTokenSymbol(bridgeResource.getToken());
                        bridgeHistory.setTokenAddress(bridgeResource.getReferCoinContactAddress());
                    }
                    bridgeHistory.setSrcTxTime(transaction.getTxTimestamp());
                    String txState = transaction.getStatus().replace("0x", "");
                    bridgeHistory.setSrcTxState(Integer.parseInt(txState,16));
                    bridgeHistory.setSrcTxHash(transactionHash);
                    bridgeHistory.setSrcChainId(chainID);
                    bridgeHistory.setBridgeState(Integer.parseInt(txState,16));
                    bridgeHistoryDao.save(bridgeHistory);
                //目标链接收跨链交易解析;
                }else if( params.size() > 2 && method.contains("0x20e82d03")){
                    String resourceID = "0x" + params.get(4);
                    List<BridgeResource> resources = bridgeResourceDao.findByResourceID(resourceID);
                    Integer chainID = Integer.parseInt(chainIDStr,16);
                    Integer depositNonce = Integer.parseInt(params.get(2).replace("0x",""),16);
                    List<BridgeHistory> bridgeHistorys = bridgeHistoryDao.findByChainId(chainID,depositNonce);
                    for (BridgeHistory bridgeHistory : bridgeHistorys) {
                        bridgeHistory.setDstChainId(chainID);
                        bridgeHistory.setDstDepositNonce(depositNonce);
                        bridgeHistory.setResourceId(resourceID);
                        if(resources.size() > 0){
                            BridgeResource bridgeResource = resources.get(0);
                            if(bridgeHistory.getSrcNetwork().equals(bridgeResource.getNetwork())){
                                bridgeHistory.setDstNetwork(bridgeResource.getReferNetwork());
                                bridgeHistory.setDstTxHash(transactionHash);
                                int txSatte = Integer.parseInt(transaction.getStatus().replace("0x",""), 16);
                                bridgeHistory.setDstTxState(txSatte);
                                bridgeHistory.setDstTxTime(transaction.getTxTimestamp());
                                //bridge 状态设置;
                                if(txSatte == 0){
                                    bridgeHistory.setBridgeState(0);
                                }else if(txSatte == 1){
                                    bridgeHistory.setBridgeState(2);
                                }
                            }else {
                                bridgeHistory.setDstNetwork(bridgeResource.getNetwork());
                            }
                        }
                        //更新beidge
                        bridgeHistoryDao.updaeBridgeHistory(bridgeHistory);
                    }
                    if(bridgeHistorys.size() < 1){
                        BridgeHistory history = new BridgeHistory();
                        if(resources.size() > 0){
                            BridgeResource bridgeResource = resources.get(0);
                            history.setSrcNetwork(bridgeResource.getReferNetwork());
                            history.setTokenSymbol(bridgeResource.getToken());
                            history.setTokenAddress(bridgeResource.getCoinContactAddress());
                        }
                        history.setBridgeFee(new BigDecimal(0));
                        history.setAddress(transaction.getFromAddr());
                        history.setResourceId(resourceID);
                        history.setSrcChainId(chainID);
                        history.setSrcDepositNonce(depositNonce);
                        history.setSrcTxHash(transactionHash);
                        int txSatte = Integer.parseInt(transaction.getStatus().replace("0x",""), 16);
                        history.setSrcTxState(txSatte);
                        history.setBridgeState(txSatte);
                        history.setSrcTxTime(transaction.getTxTimestamp());
                        history.setResourceId(resourceID);
                        bridgeHistoryDao.save(history);
                    }
                }
            }
        }
    }

    private List<Transaction> buildTransactionList(EvmData data, int chainId) {
        List<Transaction> txList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(data.getBlock().getTransactions())) {
            return txList;
        }
        for (EthBlock.TransactionResult result : data.getBlock().getTransactions()) {
            Transaction tx = new Transaction();
            org.web3j.protocol.core.methods.response.Transaction item                  = ((EthBlock.TransactionObject) result).get();
            tx.setTransactionHash(item.getHash());
            tx.setBlockHash(item.getBlockHash());
            tx.setBlockNumber(item.getBlockNumber().longValue());
            tx.setChainId(chainId);
            tx.setTransactionIndex(item.getTransactionIndex().intValue());
            tx.setFailMsg("");
            tx.setTxTimestamp(convertTime(data.getBlock().getTimestamp().longValue() * 1000));
            tx.setFromAddr(item.getFrom());
            if (Objects.nonNull(item.getTo())) {
                tx.setToAddr(item.getTo());
            }
            tx.setValue(item.getValue().toString());
            tx.setGasLimit(item.getGas());
            tx.setGasPrice(item.getGasPrice().toString());
            tx.setNonce(item.getNonce().toString());
            tx.setInput(item.getInput());
            if (StringUtils.equalsIgnoreCase("0x", item.getInput())) {
                tx.setTxType(0);
            } else {
                tx.setTxType(1);
            }
            tx.setCreateTime(new Date());

            try {
                TransactionReceipt receipt = web3j.ethGetTransactionReceipt(item.getHash()).send().getResult();
                if(receipt != null){
                    // status
                    if (receipt.getStatus() != null &&
                            ( receipt.getStatus().equalsIgnoreCase("1")
                                    || receipt.getStatus().equalsIgnoreCase("0x1"))) {
                        tx.setStatus("0x1");
                    } else {
                        tx.setStatus("0x0");
                    }
                    // gas fee
                    if (receipt.getGasUsed() != null) {
                        tx.setGasUsed(receipt.getGasUsed());
                        if (item.getGasPrice() != null) {
                            tx.setTxFee(item.getGasPrice().multiply(receipt.getGasUsed()).toString());
                        }
                    }
                    //创建合约交易
                    if(item.getInput().length() > 10){
                        String function = item.getInput().substring(0, 10);
                        if(function.equals("0x60806040") && receipt.getContractAddress() != null){
                            //设置to地址为合约地址
                            tx.setToAddr(receipt.getContractAddress());
                        }
                        if(function.equals("0x60e06040") && receipt.getContractAddress() != null){
                            //设置to地址为合约地址
                            tx.setToAddr(receipt.getContractAddress());
                        }
                    }
                    //合约地址存储
                    tx.setContractAddress(receipt.getContractAddress());
                } else {
                    log.info("[save]cannot get gas used and tx fee. txHash={}", item.getHash());
                }
            } catch (IOException e) {
                log.error("[save]error occurred when query tx receipt. tx=" + item.getHash() + ",msg=" + e.getMessage(), e);
            }
            tx.setTokenTag(0);
            txList.add(tx);
        }
        return txList;
    }
    private String toJsonString(Object obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }

    private Date convertTime(long mills) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mills);
        return cal.getTime();
    }

    public static void main(String[] args) {
//        try {
//            OkHttpClient httpClient = OkHttpUtil.buildOkHttpClient();
//            HttpService httpService = new HttpService("http://vmtestnet.chainfra.io/v1/df8371a1d8cbc835147df382bb1144a9", httpClient, false);
//            Web3j              web3j   = Web3j.build(httpService);
//            EthBlock.Block block = web3j.ethGetBlockByHash("0x705c863ae0dadfce0fd78c798a14b32e0f34b676db3fca3020cfe9374baa5653", true).send().getResult();
//            BigInteger timestamp = block.getTimestamp();
//            Date date = new Date(timestamp.longValue()*1000);
//            String strDateFormat = "yyyy-MM-dd HH:mm:ss";
//            SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
//            System.out.println(sdf.format(date));
//            System.out.println(timestamp);
//            TransactionReceipt receipt = web3j.ethGetTransactionReceipt("0x1b833516336b7e31bc0da350f9915da7a3c2d815bb115b4826e96c5a1ca8fadb").send().getResult();
//            System.out.println(receipt);
//            org.web3j.protocol.core.methods.response.Transaction tx = web3j.ethGetTransactionByHash("0x1b833516336b7e31bc0da350f9915da7a3c2d815bb115b4826e96c5a1ca8fadb").send().getResult();
//            System.out.println(tx);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}

