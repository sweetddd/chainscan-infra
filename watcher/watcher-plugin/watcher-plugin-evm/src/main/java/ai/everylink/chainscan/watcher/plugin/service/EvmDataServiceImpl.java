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

package ai.everylink.chainscan.watcher.plugin.service;

import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.dao.*;
import ai.everylink.chainscan.watcher.entity.Block;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.entity.TransactionLog;
import ai.everylink.chainscan.watcher.plugin.EvmData;
import ai.everylink.chainscan.watcher.plugin.config.EvmConfig;
import ai.everylink.chainscan.watcher.plugin.util.DecodUtils;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * EVM数据服务
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
@Slf4j
@Service
public class EvmDataServiceImpl implements EvmDataService {

    private static final String CHAIN_TYPE = "EVM_PoS";

    private Web3j web3j;

    @Autowired
    private BlockDao blockDao;

    @Autowired
    private TransactionDao transactionDao;

    @Autowired
    private TransactionLogDao transactionLogDao;

    @PostConstruct
    private void initWeb3j() {
        if (web3j != null) {
            return ;
        }
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            builder.writeTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            builder.readTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            OkHttpClient httpClient  = builder.build();

            String rpcUrl = System.getenv("watcher.vmChainUrl");
            if (rpcUrl == null) {
                rpcUrl = SpringApplicationUtils.getBean(EvmConfig.class).getRinkebyUrl();
            }
            log.info("[rpc_url]url=" + rpcUrl);

            HttpService  httpService = new HttpService(rpcUrl, httpClient, false);
            web3j = Web3j.build(httpService);
        } catch (Exception e) {
            log.error("初始化web3j异常", e);
        }
    }

    @Override
    public Long getMaxBlockNum(int chainId) {
        Long maxBlockNum = blockDao.getMaxBlockNum(chainId);
        return maxBlockNum == null ? 0L : maxBlockNum;
    }

    @Override
    public Date getMaxBlockCreationTime(int chainId) {
        return blockDao.getMaxBlockCreationTime(chainId);
    }

    @Override
    public void updateBlockByHash(String finalizedHash) {
        blockDao.updateBlockByHash(finalizedHash);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveEvmData(EvmData data) {
        int   chainId = data.getChainId();
        int   gasUsed = 0;
        Block block = buildBlock(data, chainId);

        List<Transaction> txList = buildTransactionList(data, chainId);

        if(txList.size() > 0){
            System.out.println("1");
        }
        // block gas used
        for (Transaction transaction : txList) {
            gasUsed += transaction.getGasUsed().intValue();
        }
        block.setGasUsed(new BigInteger(String.valueOf(gasUsed)));

        // block reward = sum(tx's fee) * 0.2
        BigInteger sumTxsFee = BigInteger.valueOf(0);
        for (Transaction tx : txList) {
            if (!org.springframework.util.StringUtils.isEmpty(tx.getTxFee())) {
                try {
                    sumTxsFee = sumTxsFee.add(new BigInteger(tx.getTxFee()));
                } catch (Throwable e) {
                    // ignore
                }
            }
        }
        sumTxsFee = sumTxsFee.multiply(BigInteger.valueOf(20)).divide(BigInteger.valueOf(100));
        block.setReward(sumTxsFee.toString());


        blockDao.save(block);
        log.info("[save]block={},block saved", data.getBlock().getNumber());

        if (!CollectionUtils.isEmpty(txList)) {
            transactionDao.saveAll(txList);
            log.info("[save]block={},txs saved.size={}", data.getBlock().getNumber(), txList.size());
        }

        List<TransactionLog> logList = buildTransactionLogList(data, chainId);
        if (!CollectionUtils.isEmpty(logList)) {
            transactionLogDao.saveAll(logList);
            log.info("[save]block={},logs saved,size={}", data.getBlock().getNumber(), logList.size());
        }

    }

    private Block buildBlock(EvmData data, int chainId) {
        Block block = new Block();
        block.setBlockNumber(data.getBlock().getNumber().longValue());
        block.setBlockHash(data.getBlock().getHash());
        block.setChainId(chainId);
        block.setBlockTimestamp(convertTime(data.getBlock().getTimestamp().longValue()*1000));
        block.setParentHash(data.getBlock().getParentHash());
        try {
            block.setNonce(data.getBlock().getNonce().toString());
        } catch (Exception e) {
            block.setNonce(data.getBlock().getNonceRaw());
        }
        block.setTxSize(data.getBlock().getTransactions().size());
        block.setDifficulty(data.getBlock().getDifficulty().toString());
        block.setTotalDifficulty(data.getBlock().getTotalDifficulty().toString());
        block.setBlockSize(data.getBlock().getSize().intValue());
       // block.setGasUsed(data.getBlock().getGasUsed());
        block.setGasLimit(data.getBlock().getGasLimit());
        block.setExtraData(data.getBlock().getExtraData());
        block.setCreateTime(new Date());
        block.setBurnt("");
        block.setReward("");
        block.setValidator(data.getBlock().getMiner());
        block.setChainType(CHAIN_TYPE);
        block.setStatus(0);
        return block;
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
            tx.setTxTimestamp(convertTime(data.getBlock().getTimestamp().longValue()*1000));
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
                    }
                    //合约地址存储
                    tx.setContractAddress(receipt.getContractAddress());
                } else {
                    log.info("[save]cannot get gas used and tx fee. txHash={}", item.getHash());
                }
            } catch (IOException e) {
                log.error("[save]error occurred when query tx receipt. tx=" + item.getHash() + ",msg=" + e.getMessage(), e);
            }

            tx.setChainType(CHAIN_TYPE);
            inputParams(tx);
            txList.add(tx);
        }
        return txList;
    }

    private List<TransactionLog> buildTransactionLogList(EvmData data, int chainId) {
        List<TransactionLog> logList = Lists.newArrayList();
        if (data.getTransactionLogMap() == null || data.getTransactionLogMap().size() <= 0) {
            return logList;
        }

        for (String txHash : data.getTransactionLogMap().keySet()) {
            List<Log> itemList = data.getTransactionLogMap().get(txHash);
            if (CollectionUtils.isEmpty(itemList)) {
                continue;
            }

            for (Log item : itemList) {
                TransactionLog log = new TransactionLog();
                log.setTransactionHash(item.getTransactionHash());
                log.setLogIndex(item.getLogIndex().intValue());
                log.setAddress(item.getAddress());
                log.setType(item.getType());
                log.setData(item.getData());
                log.setTopics(toJsonString(item.getTopics()));
                log.setCreateTime(new Date());
                logList.add(log);
            }
        }

        return logList;
    }

    private Date convertTime(long mills) {
        mills = mills + 8 * 60 * 60 * 1000L;
        return new Date(mills);
    }

    private String toJsonString(Object obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }

    private void inputParams(Transaction tx) {
        String input = tx.getInput();
        if(input.length()> 10 && input.substring(0,2).equals("0x")){
            Object function = DecodUtils.getFunction(input.substring(0, 10));
            if(function != null){
                tx.setInputMethod(function.toString());
                tx.setInputParams(DecodUtils.getParams(input));
            }else{
                tx.setInputMethod(input);
                tx.setInputParams(input);
            }
        }else if(input.equals("0x")){
            tx.setInputMethod("Transfer");
        }else {
            tx.setInputParams(DecodUtils.getParams(input));
        }
    }


    @Override
    @Deprecated
    public void processUnconfirmedVMBlocks(int childBlockNum) {
        List<Block> blockList = blockDao.listUncomfirmedBlock();
        if (CollectionUtils.isEmpty(blockList)) {
            return;
        }
        if (blockList.size() <= childBlockNum) {
            return;
        }

        for (int i = 0; i < blockList.size(); i++) {
            int childNum = i + childBlockNum + 1;
            if (childNum > blockList.size()) {
                break;
            }

            boolean isLinked = isLinked(blockList.get(i), blockList.subList(i+1, childNum));
            if (isLinked) {
                log.info("[vm_confirm]confirmed.hash={}", blockList.get(i).getBlockHash());
                blockDao.updateBlockStatus(0, blockList.get(i).getBlockNumber());
            }
        }
    }

    /**
     * 能否成链
     */
    @Deprecated
    private boolean isLinked(Block parentBlock, List<Block> childBlockList) {
        Block firstChild = childBlockList.get(0);
        if (childBlockList.size() == 1) {
            return parentBlock.getBlockHash().equalsIgnoreCase(firstChild.getParentHash());
        }

        if (parentBlock.getBlockHash().equalsIgnoreCase(firstChild.getParentHash())) {
            return isLinked(firstChild, childBlockList.subList(1, childBlockList.size()));
        }

        return false;
    }
}

