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

import ai.everylink.chainscan.watcher.core.config.DataSourceEnum;
import ai.everylink.chainscan.watcher.core.config.TargetDataSource;
import ai.everylink.chainscan.watcher.core.util.DecodUtils;
import ai.everylink.chainscan.watcher.core.util.JDBCUtils;
import ai.everylink.chainscan.watcher.core.util.WatcherUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.dao.BlockDao;
import ai.everylink.chainscan.watcher.dao.TransactionDao;
import ai.everylink.chainscan.watcher.dao.TransactionLogDao;
import ai.everylink.chainscan.watcher.entity.Block;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.entity.TransactionLog;
import ai.everylink.chainscan.watcher.plugin.dto.CallTransaction;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
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

    private Web3j web3j;

    private HttpService httpService;

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

            String rpcUrl = WatcherUtils.getVmChainUrl();
            log.info("[rpc_url]url=" + rpcUrl);

            httpService = new HttpService(rpcUrl, httpClient, false);
            web3j = Web3j.build(httpService);
        } catch (Exception e) {
            log.error("初始化web3j异常", e);
        }
    }

    @TargetDataSource(value = DataSourceEnum.chainscan)
    @Override
    public Long getMaxBlockNum(int chainId) {
        Long maxBlockNum = blockDao.getMaxBlockNum(chainId);
        return maxBlockNum == null ? 0L : maxBlockNum;
    }

    @TargetDataSource(value = DataSourceEnum.chainscan)
    @Override
    public Date getMaxBlockCreationTime(int chainId) {
        return blockDao.getMaxBlockCreationTime(chainId);
    }

    @TargetDataSource(value = DataSourceEnum.chainscan)
    @Override
    public void updateBlockByHash(String finalizedHash) {
        blockDao.updateBlockByHash(finalizedHash);
    }

    private boolean isBlockExist(Long blockNum, int chainId) {
        Long dbBlockNum = blockDao.getBlockIdByNum(blockNum, chainId);
        return dbBlockNum != null && dbBlockNum > 0;
    }

//    @Transactional(rollbackFor = Exception.class)
    @TargetDataSource(value = DataSourceEnum.chainscan)
    @Override
    public void saveEvmData(EvmData data) {
        long t1 = System.currentTimeMillis();
        if (isBlockExist(data.getBlock().getNumber().longValue(), data.getChainId())) {
            log.info("[save]block exist.");
            return;
        }
        log.info("[isBlockExist]consume: {}ms", (System.currentTimeMillis() - t1));

        int   chainId = data.getChainId();
        int   gasUsed = 0;
        Block block = buildBlock(data, chainId);

        List<Transaction> txList = buildTransactionList(data, chainId);

        // block gas used
        for (Transaction transaction : txList) {
            if (transaction.getGasUsed() != null) {
                gasUsed += transaction.getGasUsed().intValue();
            }
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

        List<TransactionLog> logList = buildTransactionLogList(data);

        insertDB(block, txList, logList);
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
        block.setChainType(WatcherUtils.getChainType());
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
                        ( receipt.getStatus().equalsIgnoreCase("1")
                                || receipt.getStatus().equalsIgnoreCase("0x1"))) {
                    tx.setStatus("0x1");
                } else {
                    tx.setStatus("0x0");
                    //fail
                    String failMsg = getFailMsg(item);

                    tx.setFailMsg(failMsg);

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
            }

            tx.setTokenTag(0);
            tx.setChainType(WatcherUtils.getChainType());
            inputParams(tx);
            txList.add(tx);
        }
        return txList;
    }

    private String getFailMsg(org.web3j.protocol.core.methods.response.Transaction transaction){

        try {
            CallTransaction tr = new CallTransaction(transaction.getFrom()
                    , transaction.getGasPrice(), transaction.getGas()
                    , transaction.getTo()
                    , BigInteger.ZERO
                    , transaction.getInput());
            DefaultBlockParameter defaultBlockParameter = DefaultBlockParameter.valueOf(transaction.getBlockNumber());


            EthCall send = new Request<>(
                    "eth_call",
                    Arrays.asList(tr, defaultBlockParameter),
                    httpService,
                    org.web3j.protocol.core.methods.response.EthCall.class).send();

            if (null != send.getError() && !StringUtils.isEmpty(send.getError().getMessage())){

                return send.getError().getMessage();
            }

        }catch (Exception e){
            log.error("[getFailMsg]error occurred when query tx receipt. tx=" + transaction.getHash() + ",msg=" + e.getMessage(), e);

        }
        return "";
    }

    private List<TransactionLog> buildTransactionLogList(EvmData data) {
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
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mills);
        return cal.getTime();
    }

    private String toJsonString(Object obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }

    private void inputParams(Transaction tx) {
        try {
            String input = tx.getInput();
            Object function = "";
            if (input.length() > 10 && input.startsWith("0x")) {
                function = DecodUtils.getFunction(input);
                if (function != null) {
                    tx.setInputMethod(function.toString());
                    tx.setInputParams(DecodUtils.getParams(input));
                } else {
                   // tx.setInputMethod(input);
                  //  tx.setInputParams(input);
                }
            } else if (input.equals("0x")) {
                tx.setInputMethod("Transfer");
            } else {
                tx.setInputParams(input);
            }

        } catch (Exception e) {
            log.error("[Save]inputParams call error.txHash=" + tx.getTransactionHash(), e);
        }
    }


    private void insertDB(Block block, List<Transaction> txList, List<TransactionLog> logList) {
        long t1 = System.currentTimeMillis();
        if (WatcherUtils.getProcessStep() <= 200) {
            blockDao.save(block);
        } else {
            // use origin jdbc
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            try {
                connection = JDBCUtils.getConnection();
                String sql = "INSERT INTO block (block_number, block_hash, chain_id, block_timestamp, parent_hash, miner, nonce, validator, burnt, tx_size, reward, difficulty, total_difficulty, block_size, gas_used, gas_limit, extra_data, create_time, status, block_fee, chain_type, finalized) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                preparedStatement = connection.prepareStatement(sql);
                Block b = block;
                preparedStatement.setObject(1, b.getBlockNumber());
                preparedStatement.setObject(2, b.getBlockHash());
                preparedStatement.setObject(3, b.getChainId());
                preparedStatement.setObject(4, b.getBlockTimestamp());
                preparedStatement.setObject(5, b.getParentHash());
                preparedStatement.setObject(6, "");
                preparedStatement.setObject(7, b.getNonce());
                preparedStatement.setObject(8, b.getValidator());
                preparedStatement.setObject(9, b.getBurnt());
                preparedStatement.setObject(10, b.getTxSize());
                preparedStatement.setObject(11, b.getReward());
                preparedStatement.setObject(12, b.getDifficulty());
                preparedStatement.setObject(13, b.getTotalDifficulty());
                preparedStatement.setObject(14, b.getBlockSize());
                preparedStatement.setObject(15, b.getGasUsed());
                preparedStatement.setObject(16, b.getGasLimit());
                preparedStatement.setObject(17, b.getExtraData());
                preparedStatement.setObject(18, b.getCreateTime());
                preparedStatement.setObject(19, b.getStatus());
                preparedStatement.setObject(20, b.getBlockFee());
                preparedStatement.setObject(21, b.getChainType());
                preparedStatement.setObject(22, 0);

                int rows = preparedStatement.executeUpdate();
                if (rows <= 0) {
                    log.info("[saveBlock]fail.rows={}", rows);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                // 6. 释放资源
                JDBCUtils.close(preparedStatement,connection);
            }
        }
        log.info("[save]block={},block saved", block.getBlockNumber());
        log.info("[saveBlock]consume: {}ms", (System.currentTimeMillis() - t1));

        if (!CollectionUtils.isEmpty(txList)) {
            long t2 = System.currentTimeMillis();
            if (WatcherUtils.getProcessStep() <= 400) {
                transactionDao.saveAll(txList);
            } else {
                // use origin jdbc
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                try {
                    connection = JDBCUtils.getConnection();
                    String sql = "INSERT INTO transaction (transaction_hash, transaction_index, block_hash, block_number, chain_id, status, fail_msg, tx_timestamp, " +
                            "from_addr, to_addr, contract_address, value, tx_fee, gas_limit, gas_used, gas_price, nonce, input, input_method, input_params, tx_type, " +
                            "create_time, chain_type, token_tag) " +
                            " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    preparedStatement = connection.prepareStatement(sql);
                    for (Transaction b : txList) {
                        preparedStatement.setObject(1, b.getTransactionHash());
                        preparedStatement.setObject(2, b.getTransactionIndex());
                        preparedStatement.setObject(3, b.getBlockHash());
                        preparedStatement.setObject(4, b.getBlockNumber());
                        preparedStatement.setObject(5, b.getChainId());
                        preparedStatement.setObject(6, b.getStatus());
                        preparedStatement.setObject(7, b.getFailMsg());
                        preparedStatement.setObject(8, b.getTxTimestamp());
                        preparedStatement.setObject(9, b.getFromAddr());
                        preparedStatement.setObject(10, b.getToAddr());
                        preparedStatement.setObject(11, b.getContractAddress());
                        preparedStatement.setObject(12, b.getValue());
                        preparedStatement.setObject(13, b.getTxFee());
                        preparedStatement.setObject(14, b.getGasLimit());
                        preparedStatement.setObject(15, b.getGasUsed());
                        preparedStatement.setObject(16, b.getGasPrice());
                        preparedStatement.setObject(17, b.getNonce());
                        preparedStatement.setObject(18, b.getInput());
                        preparedStatement.setObject(19, b.getInputMethod());
                        preparedStatement.setObject(20, b.getInputParams());
                        preparedStatement.setObject(21, b.getTxType());
                        preparedStatement.setObject(22, b.getCreateTime());
                        preparedStatement.setObject(23, b.getChainType());
                        preparedStatement.setObject(24, b.getTokenTag());

                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                }finally {
                    // 6. 释放资源
                    JDBCUtils.close(preparedStatement,connection);
                }
            }
            log.info("[save]block={},txs saved.size={}", block.getBlockNumber(), txList.size());
            log.info("[saveTransaction]consume: {}ms", (System.currentTimeMillis() - t2));
        }


        if (!CollectionUtils.isEmpty(logList)) {
            transactionLogDao.saveAll(logList);
            log.info("[save]block={},logs saved,size={}", block.getBlockNumber(), logList.size());
        }
    }
}

