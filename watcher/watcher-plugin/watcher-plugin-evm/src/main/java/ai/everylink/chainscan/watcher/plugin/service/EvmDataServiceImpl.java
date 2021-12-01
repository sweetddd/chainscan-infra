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

import ai.everylink.chainscan.watcher.plugin.EvmData;
import ai.everylink.chainscan.watcher.plugin.dao.*;
import ai.everylink.chainscan.watcher.plugin.entity.AccountContractBalance;
import ai.everylink.chainscan.watcher.plugin.entity.Block;
import ai.everylink.chainscan.watcher.plugin.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.entity.TransactionLog;
import ai.everylink.chainscan.watcher.plugin.util.DecodUtils;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.web3j.abi.TypeDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Log;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * EVM数据服务
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
@Slf4j
@Service
public class EvmDataServiceImpl implements EvmDataService {

    @Autowired
    private BlockDao blockDao;

    @Autowired
    private TransactionDao transactionDao;

    @Autowired
    private TransactionLogDao transactionLogDao;

    @Autowired
    private AccountDao accountDao;

    @Autowired
    private AccountContractBalanceDao accountContractBalanceDao;

    @Autowired
    private ContractDao contractDao;

    @Override
    public Long getMaxBlockNum(int chainId) {
        Long maxBlockNum = blockDao.getMaxBlockNum(chainId);
        return maxBlockNum == null ? 0L : maxBlockNum;
    }

    @Transactional
    @Override
    public void saveEvmData(EvmData data) {
        int chainId = data.getChainId();

        Block block = buildBlock(data, chainId);
        blockDao.save(block);

        List<Transaction> txList = buildTransactionList(data, chainId);
        if (!CollectionUtils.isEmpty(txList)) {
            transactionDao.saveAll(txList);
        }

        List<TransactionLog> logList = buildTransactionLogList(data, chainId);
        if (!CollectionUtils.isEmpty(logList)) {
            transactionLogDao.saveAll(logList);
        }

        if (!CollectionUtils.isEmpty(txList)) {
            updateContractBalance(txList);
        }
    }

    private Block buildBlock(EvmData data, int chainId) {
        Block block = new Block();
        block.setBlockNumber(data.getBlock().getNumber().longValue());
        block.setBlockHash(data.getBlock().getHash());
        block.setChainId(chainId);
        block.setBlockTimestamp(convertTime(data.getBlock().getTimestamp().longValue()*1000));
        block.setParentHash(data.getBlock().getParentHash());
        block.setMiner(data.getBlock().getMiner());
        try {
            block.setNonce(data.getBlock().getNonce().toString());
        } catch (Exception e) {
            block.setNonce(data.getBlock().getNonceRaw());
        }
        block.setTxSize(data.getBlock().getTransactions().size());
        block.setDifficulty(data.getBlock().getDifficulty().toString());
        block.setTotalDifficulty(data.getBlock().getTotalDifficulty().toString());
        block.setBlockSize(data.getBlock().getSize().intValue());
        block.setGasUsed(data.getBlock().getGasUsed().intValue());
        block.setGasLimit(data.getBlock().getGasLimit().intValue());
        block.setExtraData(data.getBlock().getExtraData());
        block.setCreateTime(new Date());
        block.setBurnt("");
        block.setReward("");
        block.setValidator(data.getBlock().getMiner());
        return block;
    }

    private List<Transaction> buildTransactionList(EvmData data, int chainId) {
        List<Transaction> txList = Lists.newArrayList();

        if (CollectionUtils.isEmpty(data.getBlock().getTransactions())) {
            return txList;
        }

        for (EthBlock.TransactionResult result : data.getBlock().getTransactions()) {
            org.web3j.protocol.core.methods.response.Transaction item = ((EthBlock.TransactionObject) result).get();
            Transaction tx = new Transaction();
            tx.setTransactionHash(item.getHash());
            tx.setBlockHash(item.getBlockHash());
            tx.setBlockNumber(item.getBlockNumber().longValue());
            tx.setChainId(chainId);
            tx.setTransactionIndex(item.getTransactionIndex().intValue());
            tx.setStatus("pending");// TODO
            tx.setFailMsg(""); // TODO
            tx.setTxTimestamp(convertTime(data.getBlock().getTimestamp().longValue()*1000));
            tx.setFromAddr(item.getFrom());
            if (Objects.nonNull(item.getTo())) {
                tx.setToAddr(item.getTo());
            }
            tx.setValue(item.getValue().toString());
            tx.setTxFee("0"); // TODO
            tx.setGasLimit(21000); // TODO
            tx.setGasUsed(item.getGas().intValue());
            tx.setGasPrice(item.getGasPrice().toString());
            tx.setNonce(item.getNonce().toString());
            tx.setInput(item.getInput());
            tx.setInputMethod(""); // TODO
            tx.setInputParams(""); // TODO
            if (StringUtils.equalsIgnoreCase("0x", item.getInput())) {
                tx.setTxType(0); // 1-合约交易 0-非合约交易
            } else {
                tx.setTxType(1);
            }

            tx.setCreateTime(new Date());
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
        return new Date(mills);
    }

    private String toJsonString(Object obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }


    private void updateContractBalance(List<Transaction> txList) {
        for (Transaction tx : txList) {
            if (StringUtils.equalsIgnoreCase("0x", tx.getInput())) {
                log.info("not contract.blockNum={},tx_hash={}", tx.getBlockNumber(), tx.getTransactionHash());
                continue;
            }

            String method = tx.getInputMethod();
            String params = tx.getInputParams();
            if (StringUtils.isEmpty(method) || StringUtils.isEmpty(params)) {
                log.info("method or params is null.block={},tx_hash={}", tx.getBlockNumber(), tx.getTransactionHash());
                continue;
            }

            InputParam inputParam = parseInputParam(tx.getInput());

            if (StringUtils.containsIgnoreCase(method, "transfer")) {
                String fromAddr = tx.getFromAddr();
                String contractAddr = tx.getToAddr();
                String toAddr = inputParam.addr;
                Long amount = inputParam.amount;
                boolean decFlag = decreaseAccountAmount(fromAddr, contractAddr, amount);
                boolean incFlag = increaseAccountAmount(toAddr, contractAddr, amount);
                log.info("transfer called.block={},tx={},dec={},inc={}",
                        tx.getBlockNumber(), tx.getTransactionHash(), decFlag, incFlag);
            } else if (StringUtils.containsIgnoreCase(method, "mint")) {
                String contractAddr = tx.getToAddr();
                String addr = inputParam.addr;
                Long amount = inputParam.amount;
                boolean incFlag = increaseAccountAmount(addr, contractAddr, amount);
                log.info("mint called.block={},tx={},inc={}",
                        tx.getBlockNumber(), tx.getTransactionHash(), incFlag);
            } else if (StringUtils.containsIgnoreCase(method, "burn")) {
                String contractAddr = tx.getToAddr();
                String addr = inputParam.addr;
                Long amount = inputParam.amount;
                boolean decFlag = decreaseAccountAmount(addr, contractAddr, amount);
                log.info("burn called.block={},tx={},dec={}",
                        tx.getBlockNumber(), tx.getTransactionHash(), decFlag);
            }
        }
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
        }
    }

    private boolean increaseAccountAmount(String addr, String contractAddr, Long amount) {
        AccountContractBalance old = accountContractBalanceDao.getByAccountAddrAndContractAddr(addr, contractAddr);
        if (old == null) {
            old = new AccountContractBalance();
            old.setAccountAddr(addr);
            old.setContractAddr(contractAddr);
            old.setBalance(amount);
            old.setCreateTime(new Date());
            accountContractBalanceDao.saveAndFlush(old);

            return true;
        }

        int rows = accountContractBalanceDao.increaseBalance(amount, addr, contractAddr);
        return rows > 0;
    }

    private boolean decreaseAccountAmount(String addr, String contractAddr, Long amount) {
        AccountContractBalance old = accountContractBalanceDao.getByAccountAddrAndContractAddr(addr, contractAddr);
        if (old == null) {
            log.error("decrease failure,no record found.addr={},contractAddr={}", addr, contractAddr);
            return false;
        }

        int rows = accountContractBalanceDao.decreaseBalance(amount, addr, contractAddr);
        return rows > 0;
    }


    private InputParam parseInputParam(String inputData) {
        // String method = inputData.substring(0, 10);
        String to    = inputData.substring(10, 74);
        String value = inputData.substring(74);
        Method refMethod;
        try {
            refMethod = TypeDecoder.class.getDeclaredMethod("decode", String.class, int.class, Class.class);
            refMethod.setAccessible(true);
            Address address = (Address) refMethod.invoke(null, to, 0, Address.class);
            Uint256 amount = (Uint256) refMethod.invoke(null, value, 0, Uint256.class);

            return new InputParam(address.toString(), amount.getValue().longValue());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }
}

