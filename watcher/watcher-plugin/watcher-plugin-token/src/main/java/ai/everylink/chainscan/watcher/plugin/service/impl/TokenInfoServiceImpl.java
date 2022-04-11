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

import ai.everylink.chainscan.watcher.core.util.DecodUtils;
import ai.everylink.chainscan.watcher.core.util.VM30Utils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.dao.*;
import ai.everylink.chainscan.watcher.entity.*;
import ai.everylink.chainscan.watcher.plugin.service.TokenInfoService;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
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
public class TokenInfoServiceImpl implements TokenInfoService {

    private static final String BRIDGE_TOPIC = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    private Web3j web3j;

    @Value("${watcher.vmChainUrl:}")
    private String vmChainUrl;

    @Autowired
    private VM30Utils vm30Utils;

    @Autowired
    private TransactionDao transactionDao;

    @Autowired
    private TransactionLogDao transactionLogDao;

    @Autowired
    private TokenInfoDao tokenInfoDao;

    @Autowired
    private AccountInfoDao accountInfoDao;

    @Autowired
    private NftAccountDao nftAccountDao;

    @Autowired
    private TokenAccountBalanceDao tokenAccountBalanceDao;

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
    public void tokenScan(EvmData data) {
        int               chainId = data.getChainId();
        List<Transaction> txList  = buildTransactionList(data, chainId);
        for (Transaction transaction : txList) {
            String method   = transaction.getInputMethod();
            String value    = transaction.getValue();
            String toAddr   = transaction.getToAddr();
            String fromAddr = transaction.getFromAddr();
            if(StringUtils.isNotBlank(fromAddr)){
                addAccountInfo(fromAddr); //增加用户信息;
            }
            //交易value为0则为 合约方法调用;
            if (value.equals("0") && StringUtils.isNotBlank(toAddr)) {
                addToken(toAddr, fromAddr); //增加合约信息;
            }
            //账户信息余额更新;
            if (method != null) {
                if (method.contains("mint(") || method.contains("transfer(") || method.contains("transferFrom(")
                        || method.contains("burn(") || method.contains("burnFrom(") || method.contains("deposit")
                        || method.contains("ithdraw") ) {
                    //监控此方法更新用户余额信息;
                    addToken(toAddr, fromAddr);
                    saveOrUpdateBalance(fromAddr, toAddr);
                    updateNftAccount(fromAddr, toAddr);
                }
            }
            transactionDao.updateTokenTag(transaction.getId());
        }

        // 事件监听;
        for (Transaction transaction : txList) {
            List<TransactionLog> logs = transactionLogDao.findByTxHash(transaction.getTransactionHash());
            for (TransactionLog transactionLog : logs) {
                String    contract  = transactionLog.getAddress();
                TokenInfo tokens    = tokenInfoDao.findAllByAddress(contract);
                String    topics    = transactionLog.getTopics();
                JSONArray topicList = JSONArray.parseArray(topics);

                //断言: 为跨链;
                if (topicList.get(0).equals(BRIDGE_TOPIC)) {
                    // ERC20 跨链:
                    if (tokens != null && tokens.getTokenType() == 1) {
                        String address = topicList.get(2).toString().replace("000000000000000000000000","");
                        saveOrUpdateBalance(address, contract);
                    } else if (tokens != null && tokens.getTokenType() == 2) {
                        // ERC721 跨链:
                        String address = topicList.get(2).toString().replace("000000000000000000000000","");
                        saveOrUpdateBalance(address, contract);
                        updateNftAccount(address, contract);
                    }
                }
            }
        }

    }

    /**
     * 增加用户信息
     * @param fromAddr
     */
    private void addAccountInfo(String fromAddr) {
        try {
            String result = web3j.ethGetCode(fromAddr, DefaultBlockParameterName.LATEST).send().getResult();
            if (result.equals("0x")) {
               //添加账户信息
                AccountInfo byAddress   = accountInfoDao.findByAddress(fromAddr);
                if (byAddress == null) {
                    AccountInfo accountInfo = new AccountInfo();
                    accountInfo.setAddress(fromAddr);
                    accountInfo.setCreateTime(new Date());
                    accountInfo.setUpdateTime(new Date());
                    accountInfo.setDeleted(false);
                    accountInfoDao.save(accountInfo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 2
     * 触发是否 增加token信息;
     *
     * @param toAddr
     */
    private void addToken(String toAddr, String fromAddr) {
        String     symbol   = vm30Utils.symbol(web3j, toAddr).toString();
        String     name     = vm30Utils.name(web3j, toAddr).toString();
        BigInteger decimals = vm30Utils.decimals(web3j, toAddr);
        if (StringUtils.isNotBlank(symbol) && StringUtils.isNotBlank(name)) {
            TokenInfo tokenQuery = new TokenInfo();
            tokenQuery.setAddress(toAddr);
            Example<TokenInfo> exp    = Example.of(tokenQuery);
            List<TokenInfo>    tokens = tokenInfoDao.findAll(exp);
            tokenQuery.setTokenName(name);
            tokenQuery.setTokenSymbol(symbol);
            tokenQuery.setDecimals(decimals);
            checkTokenType(toAddr, fromAddr, tokenQuery);
            if (tokens.size() < 1) {
                //判断合约类型
                checkTokenType(toAddr, fromAddr, tokenQuery);
                tokenQuery.setAddress(toAddr);
                tokenQuery.setCreateTime(new Date());
                tokenQuery.setCreateTime(new Date());
                tokenInfoDao.save(tokenQuery);
            }
            //增加账户与token关系数据;
            saveOrUpdateBalance(fromAddr, toAddr);
            updateNftAccount(fromAddr, toAddr);
        }
    }

    /**
     * 增加账户与token关联关系;
     *
     * @param fromAddr
     * @param contract
     */
    private void saveOrUpdateBalance(String fromAddr, String contract) {
        assert !fromAddr.equals(contract);

        String    symbol = "";
        TokenInfo tokens = tokenInfoDao.findAllByAddress(contract);
        if (tokens != null) {
            symbol = tokens.getTokenSymbol();
        }else {
            return;
        }
        //查询账户信息; 如果没有数据就新增
        AccountInfo accountInfo = accountInfoDao.findByAddress(fromAddr);
        if (accountInfo == null) {
            new AccountInfo();
            accountInfo.setAddress(fromAddr);
            accountInfo.setCreateTime(new Date());
            accountInfo.setUpdateTime(new Date());
            accountInfo.setDeleted(false);
            accountInfoDao.save(accountInfo);
        }

        //查询账户余额
        BigInteger          amount  = vm30Utils.balanceOf(web3j, contract, fromAddr);
        TokenAccountBalance balance = new TokenAccountBalance();
        balance.setAccountId(accountInfo.getId());
        balance.setContract(contract);
        balance.setTokenId(tokens.getId());
        Example<TokenAccountBalance> exp      = Example.of(balance);
        List<TokenAccountBalance>    balances = tokenAccountBalanceDao.findAll(exp);
        if (balances.size() < 1 && amount.compareTo(BigInteger.ZERO) > 0) {
            balance.setContract(contract);
            balance.setBalance(amount.toString());
            tokenAccountBalanceDao.save(balance);
        } else if (balances.size() == 1) {
            TokenAccountBalance tokenAccountBalance = balances.get(0);
            tokenAccountBalance.setBalance(amount.toString());
            //更新账户余额
            tokenAccountBalanceDao.updateBalance(tokenAccountBalance.getId(), amount);
        }

    }


    /**
     * 更新用户 NFT账户信息;
     *
     * @param fromAddr
     * @param contract
     */
    @Transactional
    public void updateNftAccount(String fromAddr, String contract) {
        AccountInfo accountInfo = accountInfoDao.findByAddress(fromAddr);
        if (accountInfo == null) {
            //新增用户数据;
            accountInfo = new AccountInfo();
            accountInfo.setAddress(fromAddr);
            accountInfo.setCreateTime(new Date());
            accountInfo.setUpdateTime(new Date());
            accountInfo.setDeleted(false);
            accountInfoDao.save(accountInfo);
        }
        TokenInfo tokens = tokenInfoDao.findAllByAddress(contract);
        if( tokens == null || tokens.getTokenType() != 2){
            return;
        }
        NftAccount nftAccountQuer = new NftAccount();
        nftAccountQuer.setAccountId(accountInfo.getId());
        nftAccountQuer.setTokenId(tokens.getId());
        nftAccountDao.deleteNftAccount(fromAddr, contract); //清楚账户的NFT旧信息;
        //批量获取nft的数据信息;
        BigInteger            count = vm30Utils.balanceOf(web3j, contract, fromAddr);
        ArrayList<NftAccount> nfts  = new ArrayList<>();
        for (int i = 0; i < count.intValue(); i++) {
            NftAccount nftAccount = new NftAccount();
            nftAccount.setContractName(tokens.getTokenName());
            nftAccount.setAccountId(accountInfo.getId());
            nftAccount.setTokenId(tokens.getId());
            //tokenOfOwnerByIndex
            BigInteger tokenId  = vm30Utils.tokenOfOwnerByIndex(web3j, contract, fromAddr, i);
            Utf8String tokenURL = vm30Utils.tokenURL(web3j, contract, tokenId);
            nftAccount.setNftData(tokenURL.toString());
            nftAccount.setNftId(tokenId.longValue());
            nfts.add(nftAccount);
        }
        nftAccountDao.saveAll(nfts);
    }


    /**
     * 校验tokenType
     *
     * @param contract
     */
    private void checkTokenType(String contract, String fromAddr, TokenInfo tokenInfo) {
        try {
            List<Type> parames = new ArrayList<>();
            //ERC20:
            //function totalSupply() constant returns (uint totalSupply);
            //function balanceOf(address _owner) constant returns (uint balance);
            //function transfer(address _to, uint _value) returns (bool success);
            //function transferFrom(address _from, address _to, uint _value) returns (bool success);
            //function approve(address _spender, uint _value) returns (bool success);
            //function allowance(address _owner, address _spender) constant returns (uint remaining);
            parames.add(new Address(fromAddr));
            parames.add(new Uint256(1));
            boolean transfer = vm30Utils.querryFunction(web3j, parames, "transfer", fromAddr, contract);
            parames.clear();
            parames.add(new Address(fromAddr));
            parames.add(new Address(fromAddr));
            boolean allowance = vm30Utils.querryFunction(web3j, parames, "allowance", fromAddr, contract);
            parames.clear();
            boolean totalSupply = vm30Utils.querryFunction(web3j, parames, "totalSupply", fromAddr, contract);
            parames.clear();
            parames.add(new Address(fromAddr));
            boolean balanceOf = vm30Utils.querryFunction(web3j, parames, "balanceOf", fromAddr, contract);
            parames.clear();
            parames.add(new Address(fromAddr));
            parames.add(new Address(fromAddr));
            parames.add(new Uint256(1));
            boolean transferFrom = vm30Utils.querryFunction(web3j, parames, "transferFrom", fromAddr, contract);

            //ERC721
            //function balanceOf(address _owner) external view returns (uint256);
            //function ownerOf(uint256 _tokenId) external view returns (address);
            //function safeTransferFrom(address _from, address _to, uint256 _tokenId, bytes data) external payable;
            //function transferFrom(address _from, address _to, uint256 _tokenId) external payable;
            //function approve(address _approved, uint256 _tokenId) external payable;
            //function setApprovalForAll(address _operator, bool _approved) external;
            //function getApproved(uint256 _tokenId) external view returns (address);
            //function isApprovedForAll(address _owner, address _operator) external view returns (bool);
            parames.clear();
            parames.add(new Uint256(1));
            boolean ownerOf = vm30Utils.querryFunction(web3j, parames, "ownerOf", fromAddr, contract);
            parames.clear();
            parames.add(new Address(fromAddr));
            parames.add(new Address(fromAddr));
            parames.add(new Uint256(1));
            //parames.add(new Bytes31("".getBytes()));
            boolean safeTransferFrom = vm30Utils.querryFunction(web3j, parames, "safeTransferFrom", fromAddr, contract);
            parames.clear();
            parames.add(new Address(fromAddr));
            parames.add(new Address(fromAddr));
            parames.add(new Uint256(1));
            boolean transferFrom721 = vm30Utils.querryFunction(web3j, parames, "transferFrom", fromAddr, contract);
            parames.clear();
            parames.add(new Address(fromAddr));
            parames.add(new Uint256(1));
            boolean approve = vm30Utils.querryFunction(web3j, parames, "approve", fromAddr, contract);
            parames.clear();
            parames.add(new Address(fromAddr));
            parames.add(new Bool(true));
            boolean setApprovalForAll = vm30Utils.querryFunction(web3j, parames, "setApprovalForAll", fromAddr, contract);
            parames.clear();
            parames.add(new Uint256(1));
            boolean getApproved = vm30Utils.querryFunction(web3j, parames, "getApproved", fromAddr, contract);
            parames.clear();
            parames.add(new Address(fromAddr));
            parames.add(new Address(fromAddr));
            boolean isApprovedForAll = vm30Utils.querryFunction(web3j, parames, "isApprovedForAll", fromAddr, contract);

            boolean erc20  = transfer && allowance && totalSupply && balanceOf && transferFrom;
            boolean erc721 = balanceOf && ownerOf && safeTransferFrom && transferFrom && approve && setApprovalForAll && getApproved && isApprovedForAll;

            if (erc721) {
                tokenInfo.setTokenType(2);
            } else if (erc20) {
                tokenInfo.setTokenType(1);
            } else {
                tokenInfo.setTokenType(0);
            }
        } catch (Exception e) {
            log.error("识别合约类型异常:" + e.getMessage());
            e.printStackTrace();
            tokenInfo.setTokenType(0);
        }
    }

    private List<Transaction> buildTransactionList(EvmData data, int chainId) {
        List<Transaction> txList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(data.getBlock().getTransactions())) {
            return txList;
        }
        for (EthBlock.TransactionResult result : data.getBlock().getTransactions()) {
            Transaction   tx   = new Transaction();
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

            try {
                TransactionReceipt receipt = web3j.ethGetTransactionReceipt(item.getHash()).send().getResult();
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
            } catch (IOException e) {
                log.error("[save]error occurred when query tx receipt. tx=" + item.getHash() + ",msg=" + e.getMessage(), e);
            }
            inputParams(tx);
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

    private void inputParams(Transaction tx) {
        try {
            String input = tx.getInput();
            if (input.length() > 10 && input.startsWith("0x")) {
                Object function = DecodUtils.getFunction(input);
                if (function != null) {
                    tx.setInputMethod(function.toString());
                    tx.setInputParams(DecodUtils.getParams(input));
                } else {
                    tx.setInputMethod(input);
                    tx.setInputParams(input);
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

}

