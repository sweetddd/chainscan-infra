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
import ai.everylink.chainscan.watcher.entity.Transaction;
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
        String            property1 = environment.getProperty("spring.datasource.url");
        int   chainId = data.getChainId();
        List<Transaction> txList = buildTransactionList(data, chainId);
        // 事件监听 解析;
        for (Transaction transaction : txList) {
            String input           = transaction.getInput();
            if(StringUtils.isNotBlank(input) && input.length() > 74){
                List<String> params = DecodUtils.getParams2List(input);
                String method = params.get(0);
                //发起跨链
                if(params.size() > 2 && (method.contains("0xa44f5fe6") ||   //ERC20
                    method.contains("0xee1c1c7b")||  //原生币
                    method.contains("0xfe4464a7"))){  //NFT
                    bridgeHistoryService.depositBridge(transaction,data);
                //目标链接收跨链交易解析;
                }else if( params.size() > 2 && method.contains("0x20e82d03")){
                    bridgeHistoryService.bridgeHistoryScan(transaction,data);
                }

                //Deposit depositERC20 :0x58242801d371a53f9cddac5a44a17e4ca2523fc7ba7b171a9d71e0b8fd069630
                if( params.size() > 2 && method.contains("0xe17376b5")){
                    depositHistoryService.depositHistoryScan(transaction,data);
                }
                // depositNativeToken :0x79031410a6b2e95b5cc4e954c236e45c9dab96ad22ea80b26c2611097819b001
                if( params.size() > 2 && method.contains("0x20e2d818")){
                    depositHistoryService.depositHistoryScan(transaction,data);
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
            TransactionReceipt receipt     = web3j.ethGetTransactionReceipt("0x33d4612ce3fffb58eed2ad1d7d28c0f9f110de0721159dc6a0f4b26775097676").send().getResult();
            System.out.println(receipt);
            org.web3j.protocol.core.methods.response.Transaction tx = web3j.ethGetTransactionByHash("0x33d4612ce3fffb58eed2ad1d7d28c0f9f110de0721159dc6a0f4b26775097676").send().getResult();
            System.out.println(tx);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
