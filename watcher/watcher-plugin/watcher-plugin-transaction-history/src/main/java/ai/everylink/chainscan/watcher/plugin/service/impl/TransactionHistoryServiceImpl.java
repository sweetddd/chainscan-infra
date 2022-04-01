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
import ai.everylink.chainscan.watcher.core.util.OkHttpUtil;
import ai.everylink.chainscan.watcher.core.util.WatcherUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.dao.WalletTranactionHistoryDao;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.entity.WalletTransactionHistory;
import ai.everylink.chainscan.watcher.plugin.service.BridgeHistoryService;
import ai.everylink.chainscan.watcher.plugin.service.ConvertHistoryService;
import ai.everylink.chainscan.watcher.plugin.service.DepositHistoryService;
import ai.everylink.chainscan.watcher.plugin.service.TransactionHistoryService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * TransactionHistory统计扫描
 *
 * @author brett
 * @since 2022-03-26
 */
@Slf4j
@Service
public class TransactionHistoryServiceImpl implements TransactionHistoryService {

    private Web3j web3j;

    @Autowired
    private BridgeHistoryService bridgeHistoryService;

    @Autowired
    private DepositHistoryService depositHistoryService;

    @Autowired
    private ConvertHistoryService convertHistoryService;

    @Autowired
    private WalletTranactionHistoryDao wTxHistoryDao;

    @Autowired
    Environment environment;


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
            HttpService  httpService = new HttpService(rpcUrl, httpClient, false);
            web3j = Web3j.build(httpService);
        } catch (Exception e) {
            log.error("初始化web3j异常", e);
        }
    }

    @Override
    @TargetDataSource(value = DataSourceEnum.wallet)
    public void transactionHistoryScan(EvmData data) {
       // String            property1 = environment.getProperty("spring.datasource.url");
        int   chainId = data.getChainId();
        List<Transaction> txList = buildTransactionList(data, chainId);

        if( txList.size() > 0){
            log.error("transactionHistoryScan:txList" + txList.toString());
        }
        // 事件监听 解析;
        for (Transaction transaction : txList) {
            String input           = transaction.getInput();
            if(StringUtils.isNotBlank(input) && input.length() >  10){
                List<String> params = DecodUtils.getParams2List(input);
                String method = params.get(0);
                //发起跨链
                if(params.size() > 2 && (method.contains("0xa44f5fe6") ||   //ERC20
                    method.contains("0xee1c1c7b")||  //原生币
                    method.contains("0xfe4464a7"))){  //NFT
                    bridgeHistoryService.depositBridge(transaction,data);
                //目标链接收跨链交易解析;
                }else if( params.size() > 1 && method.contains("0x20e82d03")){
                    log.info("transactionHistoryScan:method" + "0x20e82d03");
                    bridgeHistoryService.bridgeHistoryScan(transaction,data);
                }

                //Deposit depositERC20 :0x58242801d371a53f9cddac5a44a17e4ca2523fc7ba7b171a9d71e0b8fd069630
                if( params.size() > 1 && method.contains("0xe17376b5")){
                    log.info("transactionHistoryScan:method" + "0xe17376b5");
                    depositHistoryService.depositERC20HistoryScan(transaction,data);
                }
                // depositNativeToken :0x79031410a6b2e95b5cc4e954c236e45c9dab96ad22ea80b26c2611097819b001
                if( params.size() > 1 && method.contains("0x20e2d818")){
                    log.info("transactionHistoryScan:method" + "0x20e2d818");
                    depositHistoryService.depositNativeTokenHistoryScan(transaction,data);
                }
            }
        }
    }

    @Override
    @TargetDataSource(value = DataSourceEnum.wallet)
    public void updateConfirmBlock(EvmData blockData) {
        BigInteger blockNumber = blockData.getBlock().getNumber();
       List<WalletTransactionHistory> txHistorys = wTxHistoryDao.findConfirmBlock();
        for (WalletTransactionHistory txHistory : txHistorys) {
            String type = txHistory.getType();
            int confirmBlock = 0;
            if(txHistory.getConfirmBlock()!= null){
                 confirmBlock = txHistory.getConfirmBlock().intValue();
            }
            int   number   = blockNumber.intValue() - confirmBlock;
            if(number < 13 && type.equals("Bridge")) {
                txHistory.setConfirmBlock(new BigInteger(number + ""));
                if(StringUtils.isEmpty(txHistory.getToTxHash())){
                    txHistory.setTxState("From Chain Processing (" + number + "/12)");
                }else {
                    txHistory.setTxState("To Chain Processing (" + number + "/12)");
                }
            }else if(number > 13 && type.equals("Bridge")){
                txHistory.setTxState("Finalized");
            }else if(number < 13 && type.equals("Deposit")){
                txHistory.setConfirmBlock(new BigInteger(number + ""));
                txHistory.setTxState("L1 Depositing (" + number + "/12)");
            }else if(number > 12 && type.equals("Deposit")){
                txHistory.setTxState("L2 Processing");
            }else {
                txHistory.setConfirmBlock(new BigInteger("12"));
                txHistory.setTxState("In Consensus Processing");
            }
            wTxHistoryDao.updateTxHistory(txHistory);
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

    private Date convertTime(long mills) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mills);
        return cal.getTime();
    }

    public static void main(String[] args) {
        try {
            OkHttpClient httpClient = OkHttpUtil.buildOkHttpClient();
            HttpService httpService = new HttpService("http://vmtest.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf", httpClient, false);
            Web3j              web3j       = Web3j.build(httpService);
            EthBlockNumber     blockNumber = web3j.ethBlockNumber().send();
            TransactionReceipt receipt     = web3j.ethGetTransactionReceipt("0xb13b4da6108a19386c4f2d28c930994e30254d8d4b032356032fa1da1018622f").send().getResult();
            System.out.println(receipt);
            org.web3j.protocol.core.methods.response.Transaction tx = web3j.ethGetTransactionByHash("0xb13b4da6108a19386c4f2d28c930994e30254d8d4b032356032fa1da1018622f").send().getResult();
            System.out.println(tx);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

