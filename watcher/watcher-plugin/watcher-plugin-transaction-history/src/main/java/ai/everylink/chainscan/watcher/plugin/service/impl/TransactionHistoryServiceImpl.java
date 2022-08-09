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
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

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
    public void transactionHistoryTxScan(Transaction transaction) {
        String bridgeContracts = environment.getProperty("watcher.bridge.contract.address");
        String toAddr = transaction.getToAddr();
        //监听指定合约:
        if (StringUtils.isNotBlank(toAddr) && bridgeContracts.equals(toAddr)) {
            String input = transaction.getInput();
            if (StringUtils.isNotBlank(input) && input.length() > 10) {
                List<String> params = DecodUtils.getParams2List(input);
                String       method = params.get(0);
                //发起跨链
                if (params.size() > 2 && (method.contains("0xa44f5fe6") ||   //ERC20
                        method.contains("0xee1c1c7b") ||  //原生币
                        method.contains("0xfe4464a7"))) {  //NFT
                    bridgeHistoryService.depositBridge(transaction);
                    //目标链接收跨链交易解析;
                } else if (params.size() > 1 && (method.contains("0x20e82d03") || params.size() > 1 && method.contains("0x9cbabcf6"))) {
                    log.info("transactionHistoryScan:method" + "0x20e82d03");
                    bridgeHistoryService.bridgeHistoryScan(transaction);
                }

                //Deposit depositERC20 :0x58242801d371a53f9cddac5a44a17e4ca2523fc7ba7b171a9d71e0b8fd069630
                if (params.size() > 1 && method.contains("0xe17376b5")) {
                    log.info("transactionHistoryScan:method" + "0xe17376b5");
                    depositHistoryService.depositERC20HistoryScan(transaction);
                }
                // depositNativeToken :0x79031410a6b2e95b5cc4e954c236e45c9dab96ad22ea80b26c2611097819b001
                if (params.size() > 1 && method.contains("0x20e2d818")) {
                    log.info("transactionHistoryScan:method" + "0x20e2d818");
                    depositHistoryService.depositNativeTokenHistoryScan(transaction);
                }
            }
        }
    }

    @Override

    public void transactionHistoryScan(EvmData data) {
        String            bridgeContracts = environment.getProperty("watcher.bridge.contract.address");
        int               chainId         = data.getChainId();
        List<Transaction> txList          = buildTransactionList(data, chainId);

        // 事件监听 解析;
        for (Transaction transaction : txList) {
            String toAddr = transaction.getToAddr();
            //监听指定合约:
            if (StringUtils.isNotBlank(toAddr) && bridgeContracts.equals(toAddr)) {
                String input = transaction.getInput();
                if (StringUtils.isNotBlank(input) && input.length() > 10) {
                    List<String> params = DecodUtils.getParams2List(input);
                    String       method = params.get(0);
                    //发起跨链
                    if (params.size() > 2 && (method.contains("0xa44f5fe6") ||   //ERC20
                            method.contains("0xee1c1c7b") ||  //原生币
                            method.contains("0xfe4464a7"))) {  //NFT
                        bridgeHistoryService.depositBridge(transaction);
                        //目标链接收跨链交易解析;
                    } else if (params.size() > 1  && (method.contains("0x20e82d03") || params.size() > 1 && method.contains("0x9cbabcf6"))) {
                        log.info("transactionHistoryScan:method" + "0x20e82d03");
                        bridgeHistoryService.bridgeHistoryScan(transaction);
                    }

                    //Deposit depositERC20 :0x58242801d371a53f9cddac5a44a17e4ca2523fc7ba7b171a9d71e0b8fd069630
                    if (params.size() > 1 && method.contains("0xe17376b5")) {
                        log.info("transactionHistoryScan:method" + "0xe17376b5");
                        depositHistoryService.depositERC20HistoryScan(transaction);
                    }
                    // depositNativeToken :0x79031410a6b2e95b5cc4e954c236e45c9dab96ad22ea80b26c2611097819b001
                    if (params.size() > 1 && method.contains("0x20e2d818")) {
                        log.info("transactionHistoryScan:method" + "0x20e2d818");
                        depositHistoryService.depositNativeTokenHistoryScan(transaction);
                    }
                }
            }
        }
    }

    @Override
    @TargetDataSource(value = DataSourceEnum.wallet)
    public void updateConfirmBlock(EvmData blockData) {
        BigInteger                     chainId     = new BigInteger(blockData.getChainId() + "");
        BigInteger                     confirmBlock = blockData.getBlock().getNumber();
        long    startTime     = System.currentTimeMillis();
        List<WalletTransactionHistory> txHistorys  = wTxHistoryDao.findConfirmBlock();
        log.info("TxHistory-findConfirmBlock-consum:" + (System.currentTimeMillis() - startTime));

        long    startTime2     = System.currentTimeMillis();
        for (WalletTransactionHistory txHistory : txHistorys) {
            try {
                //Integer fromChainId = txHistory.getFromChainId();
                //if(WatcherUtils.getChainId() == fromChainId){
                String     fromTxHash   = txHistory.getFromTxHash();
                String     type         = txHistory.getType();
                BigInteger newNumber = BigInteger.ZERO;
                BigInteger submitBlock   = txHistory.getSubmitBlock();
                if(submitBlock == null || submitBlock.compareTo(BigInteger.ZERO) == 0){
                    long       block     = web3j.ethBlockNumber().send().getBlockNumber().longValue();
                    newNumber = BigInteger.valueOf(block);
                    txHistory.setSubmitBlock(newNumber);
                }
                BigInteger number = submitBlock.subtract(confirmBlock).abs();
                txHistory.setConfirmBlock(number);
                log.info("update confirm block ,submitBlock is [{}],confirmBlock is [{}],number is [{}]",submitBlock,confirmBlock,number);
                if (number.longValue() < 13 && type.equals("Bridge") && 0 < number.longValue()) {
                    if (StringUtils.isEmpty(txHistory.getToTxHash())) {
                        txHistory.setTxState("From Chain Processing (" + number + "/12)");
                    } else {
                        txHistory.setTxState("To Chain Processing (" + number + "/12)");
                    }
                } else if (type.equals("Bridge") && 0 == number.longValue()) {
                    if (StringUtils.isEmpty(txHistory.getToTxHash())) {
                        txHistory.setTxState("From Chain Processing (1/12)");
                    } else {
                        txHistory.setTxState("To Chain Processing (1/12)");
                    }
                } else if (number.longValue() > 13 && type.equals("Bridge")) {
                    txHistory.setConfirmBlock(new BigInteger("12"));
                    if (chainId.equals(txHistory.getToChainId())) {
                        txHistory.setTxState("Finalized");
                    } else {
                        txHistory.setTxState("In Consensus Processing");
                    }
                } else if (number.longValue() < 13 && type.equals("Deposit") && 0 < number.longValue()) {
                    txHistory.setTxState("L1 Depositing (" + number + "/12)");
                } else if (type.equals("Deposit") && 0 == number.longValue()) {
                    txHistory.setTxState("L1 Depositing (1/12)");
                } else if (number.longValue() >= 12 && type.equals("Deposit")) {
                    txHistory.setConfirmBlock(new BigInteger("12"));
                    txHistory.setTxState("Finalized");
                } else {
                    txHistory.setConfirmBlock(new BigInteger("12"));
                    txHistory.setTxState("In Consensus Processing");
                }
                wTxHistoryDao.updateTxHistory(txHistory);
                //}
            } catch (Exception e) {
                //log.info("update txlog error");
            }
        }
        log.info("TxHistory-findConfirmBlock-for-consum:" + (System.currentTimeMillis() - startTime2));
    }


    private List<Transaction> buildTransactionList(EvmData data, int chainId) {
        List<Transaction> txList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(data.getBlock().getTransactions())) {
            return txList;
        }
        for (EthBlock.TransactionResult result : data.getBlock().getTransactions()) {
            Transaction                                          tx   = new Transaction();
            org.web3j.protocol.core.methods.response.Transaction item = ((EthBlock.TransactionObject) result).get();
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

            TransactionReceipt receipt = data.getTxList().get(item.getHash());
            if (receipt != null) {
                // status
                if (receipt.getStatus() != null &&
                        (receipt.getStatus().equalsIgnoreCase("1")
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
                if (item.getInput().length() > 10) {
                    String function = item.getInput().substring(0, 10);
                    if (function.equals("0x60806040") && receipt.getContractAddress() != null) {
                        //设置to地址为合约地址
                        tx.setToAddr(receipt.getContractAddress());
                    }
                    if (function.equals("0x60e06040") && receipt.getContractAddress() != null) {
                        //设置to地址为合约地址
                        tx.setToAddr(receipt.getContractAddress());
                    }
                }
                //合约地址存储
                tx.setContractAddress(receipt.getContractAddress());
            }

            tx.setTokenTag(0);
            tx.setChainType(WatcherUtils.getChainType());
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
            OkHttpClient       httpClient  = OkHttpUtil.buildOkHttpClient();
            HttpService        httpService = new HttpService("http://dna-chain-test-node-4-sandbox.chain-sandbox.svc.cluster.local:9934/v1/72f3a83ea86b41b191264bd16cbac2bf", httpClient, false);
            Web3j              web3j       = Web3j.build(httpService);
            EthGetCode         send        = web3j.ethGetCode("0x0000000000000000000000000000000000000806", DefaultBlockParameterName.LATEST).send();
            //EthGetCode         send        = web3j.ethGetCode("0xB382F247eF75878CAC13C8a8b63816249B43f4B9", DefaultBlockParameterName.LATEST).send();
            System.out.println(send.getCode());
//            EthBlockNumber     blockNumber = web3j.ethBlockNumber().send();
//            TransactionReceipt receipt     = web3j.ethGetTransactionReceipt("0xc69cb77ede3ae7fa1875a7362a38573ef9855a3a411fa60ea597ab8e1cef0787").send().getResult();
//            System.out.println(receipt);
////            org.web3j.protocol.core.methods.response.Transaction tx = web3j.ethGetTransactionByHash("0xc69d3c5031b0ce180ea7975720b376a7ef449388d36cd9d6c37a1590165a2731").send().getResult();
////            System.out.println(tx);
//
//            String result = web3j.ethGetCode("0x6Da573EEc80f63c98b88cED15D32CA270787FB5a", DefaultBlockParameterName.LATEST).send().getResult();
//            System.out.println(result.length());
//
//            BigInteger transactionCount = web3j.ethGetTransactionCount("0x6Da573EEc80f63c98b88cED15D32CA270787FB5a", DefaultBlockParameterName.LATEST).send().getTransactionCount();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

