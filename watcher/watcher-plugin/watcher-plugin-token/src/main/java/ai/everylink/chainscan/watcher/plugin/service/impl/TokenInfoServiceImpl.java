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
import ai.everylink.chainscan.watcher.core.util.VmChainUtil;
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
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
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

    private static final String TRANSFER_TOPIC = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    private Web3j web3j;

    @Value("${watcher.vmChainUrl:}")
    private String vmChainUrl;

    @Autowired
    private VM30Utils vm30Utils;

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

    /**
     * tx扫描
     * @param transaction
     */
    @Override
    public void tokenTxScan(Transaction transaction) {
//        BigInteger tokenId  = vm30Utils.tokenOfOwnerByIndex(web3j, "0x9533e4b3c982383544fe9f81fac4762c6c87e298", "0xc5989f90f41c2f4df71076680a597432136bc10d", 1);
//        Utf8String tokenURL = vm30Utils.tokenURL(web3j, "0x9533e4b3c982383544fe9f81fac4762c6c87e298", tokenId);

        String value    = transaction.getValue();
        String toAddr   = transaction.getToAddr();
        String fromAddr = transaction.getFromAddr();
        //交易value为0则为 合约方法调用;
//        if (value.equals("0") && StringUtils.isNotBlank(toAddr) && StringUtils.isNotBlank(fromAddr)) {
//            addToken(transaction); //增加合约信息;
//            addAccountInfo(fromAddr); //增加用户信息;
//        }
        //转账事件
        transactionLogDao.findByTxHash(transaction.getTransactionHash()).forEach(transactionLog -> {
            String topicsStr = transactionLog.getTopics();
            //topics转为数组
            JSONArray topics = JSONArray.parseArray(topicsStr);
            if (topics.size() > 0) {
                String topic = topics.get(0).toString();
                if (topic.equals(TRANSFER_TOPIC)) {
                    String topicFrom = topics.get(1).toString();
                    String topicTo = topics.get(2).toString();
                    String hexadecimal = topics.size() > 3 ? topics.get(3).toString(): transactionLog.getData();
                    BigInteger txAmt = VmChainUtil.hexadecimal2Decimal(hexadecimal);
                    addToken(transaction);
                    saveOrUpdateBalance(topicFrom, toAddr, txAmt, false);
                    saveOrUpdateBalance(topicTo, toAddr, txAmt, true);
                    updateNftAccount(fromAddr, toAddr);
                }
            }
        } );
    }


    @Override
    public void tokenScan(EvmData data) {
        int chainId = data.getChainId();
        List<Transaction> txList  = buildTransactionList(data, chainId);
        for (Transaction transaction : txList) {
            String value = transaction.getValue();
            String toAddr = transaction.getToAddr();
            String fromAddr = transaction.getFromAddr();
            if(StringUtils.isNotBlank(fromAddr)){
                addAccountInfo(fromAddr); //增加用户信息;
            }
//            //交易value为0则为 合约方法调用;
//            if (value.equals("0") && StringUtils.isNotBlank(toAddr)) {
//                addToken(transaction); //增加合约信息;
//            }

            String method = transaction.getInputMethod();
//            if (method.contains("mint(") ){
//                String input = transaction.getInput();
//                List<String> params2List = DecodUtils.getParams2List(input);
//                String accountAdd = "0x" + params2List.get(1).substring(params2List.get(1).length() - 40);
//                addAccountInfo(accountAdd); //增加用户信息;
//                saveOrUpdateBalance(accountAdd, toAddr);
//                updateNftAccount(accountAdd, toAddr);
//                return;
//            }


            // 转账事件监听;
            if(null != data.getTransactionLogMap() && data.getTransactionLogMap().size() > 0){
                List<Log> logs = data.getTransactionLogMap().get(transaction.getTransactionHash());
                if(!CollectionUtils.isEmpty(logs)){
                    logs.forEach(log -> {
                        if (log.getTopics().size() > 0) {
                            String topic = log.getTopics().get(0);
                            if (topic.equals(TRANSFER_TOPIC)) {
                                String topicFrom = log.getTopics().get(1);
                                String topicTo = log.getTopics().get(2);
                                String hexadecimal = log.getTopics().size() > 3 ? log.getTopics().get(3): log.getData();
                                BigInteger txAmt = VmChainUtil.hexadecimal2Decimal(hexadecimal);
                                addToken(transaction);
                                saveOrUpdateBalance(topicFrom, toAddr, txAmt, false);
                                saveOrUpdateBalance(topicTo, toAddr, txAmt, true);
                                updateNftAccount(fromAddr, toAddr);
                            }
                        }
                    } );
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
            AccountInfo account = accountInfoDao.findByAddress(fromAddr);
            if (account == null) {
                String result = web3j.ethGetCode(fromAddr, DefaultBlockParameterName.LATEST).send().getResult();
                if (result.equals("0x")) {
                   //添加账户信息
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
     * @param transaction
     */
    private void addToken(Transaction transaction) {
        String toAddr = transaction.getToAddr();
        String fromAddr = transaction.getFromAddr();
        try {
            TokenInfo  tokenInfo = tokenInfoDao.findAllByAddress(toAddr);
            if( tokenInfo == null && StringUtils.isNotBlank(toAddr)){
                String     symbol   = vm30Utils.symbol(web3j, toAddr).toString();
                if(StringUtils.isBlank(symbol)){
                    return;
                }
                String     name     = vm30Utils.name(web3j, toAddr).toString();
                if(StringUtils.isBlank(name)){
                    return;
                }
                BigInteger decimals = vm30Utils.decimals(web3j, toAddr);
                if (StringUtils.isNotBlank(symbol) && StringUtils.isNotBlank(name)) {
                    TokenInfo tokenQuery = new TokenInfo();
                    //判断合约类型
                    checkTokenType(toAddr, fromAddr, tokenQuery,decimals);

                    //增加部署合约者;
                    String input = transaction.getInput();
                    List<String> params2List = DecodUtils.getParams2List(input);
                    if(params2List.size()>1 && params2List.get(0).equals("0x60806040")){
                        AccountInfo byAddress   = accountInfoDao.findByAddress(fromAddr);
                        if (byAddress == null) {
                            String result = web3j.ethGetCode(fromAddr, DefaultBlockParameterName.LATEST).send().getResult();
                            if (result.equals("0x")) {
                                //添加账户信息
                                AccountInfo accountInfo = new AccountInfo();
                                accountInfo.setAddress(fromAddr);
                                accountInfo.setCreateTime(new Date());
                                accountInfo.setUpdateTime(new Date());
                                accountInfo.setDeleted(false);
                                accountInfoDao.save(accountInfo);
                                tokenQuery.setCreateAccountId(accountInfo.getId());
                            }
                        }else {
                            tokenQuery.setCreateAccountId(byAddress.getId());
                        }
                    }
                    tokenQuery.setTokenName(name);
                    tokenQuery.setTokenSymbol(symbol);
                    tokenQuery.setDecimals(decimals);
                    tokenQuery.setAddress(toAddr);
                    tokenQuery.setCreateTime(new Date());
                    tokenQuery.setCreateTime(new Date());
                    tokenInfoDao.save(tokenQuery);
                    //增加账户与token关系数据;
//                    saveOrUpdateBalance(fromAddr, toAddr);
//                    updateNftAccount(fromAddr, toAddr);
                }
            }

        }    catch (Exception e) {
            log.error("addToken error", e);
        }

    }

    /**
     * 增加账户与token关联关系;
     *
     * @param fromAddr
     * @param contract
     * 0地址不操作，另外的地址还是会操作
     *
     */
    private void saveOrUpdateBalance(String fromAddr, String contract, BigInteger txAmount, Boolean add) {
        if(Objects.requireNonNull(VmChainUtil.hexadecimal2Decimal(fromAddr)).compareTo(BigInteger.ZERO) != 0){
            String symbol = "";
            TokenInfo token = tokenInfoDao.findAllByAddress(contract);
            if (token != null) {
                symbol = token.getTokenSymbol();
            }else {
                return;
            }
            //查询账户信息; 如果没有数据就新增
            AccountInfo query = accountInfoDao.findByAddress(fromAddr);
            if (query == null) {
                AccountInfo accountInfo = new AccountInfo();
                accountInfo.setAddress(fromAddr);
                accountInfo.setCreateTime(new Date());
                accountInfo.setUpdateTime(new Date());
                accountInfo.setDeleted(false);
                query = accountInfoDao.save(accountInfo);
            }

            //查询账户余额
            TokenAccountBalance balance = new TokenAccountBalance();
            balance.setAccountId(query.getId());
            balance.setTokenId(token.getId());
            Example<TokenAccountBalance> exp = Example.of(balance);
            List<TokenAccountBalance> balances = tokenAccountBalanceDao.findAll(exp);
            BigInteger asset = CollectionUtils.isEmpty(balances)
                    ? BigInteger.ZERO
                    : new BigInteger(balances.get(0).getBalance());
            asset = Objects.equals(add, true) ? asset.add(txAmount): asset.subtract(txAmount);
            asset = asset.compareTo(BigInteger.ZERO) < 0 ? vm30Utils.balanceOf(web3j, contract, fromAddr): asset;

            if (balances.size() < 1 && asset.compareTo(BigInteger.ZERO) > 0) {
                balance.setBalance(asset.toString());
                balance.setCreateTime(new Date());
                balance.setUpdateTime(new Date());
                tokenAccountBalanceDao.save(balance);
            } else if (balances.size() == 1) {
                TokenAccountBalance tokenAccountBalance = balances.get(0);
                tokenAccountBalance.setBalance(asset.toString());
                //更新账户余额
                tokenAccountBalanceDao.updateBalance(tokenAccountBalance.getId(), asset);
            }
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
        nftAccountDao.deleteNftAccount(accountInfo.getId(), tokens.getId()); //清楚账户的NFT旧信息;
        //批量获取nft的数据信息;
        BigInteger            count = vm30Utils.balanceOf(web3j, contract, fromAddr);
        ArrayList<NftAccount> nfts  = new ArrayList<>();
        for (int i = 0; i < count.intValue(); i++) {
            NftAccount nftAccount = new NftAccount();
            try {
                nftAccount.setContractName(tokens.getTokenName());
                nftAccount.setAccountId(accountInfo.getId());
                nftAccount.setTokenId(tokens.getId());
                //tokenOfOwnerByIndex
                BigInteger tokenId  = vm30Utils.tokenOfOwnerByIndex(web3j, contract, fromAddr, i);
                if(tokenId.intValue() == -1 || ( tokenId.intValue() == 0 && i >0)) {
                    tokens.setTokenType(1);
                    tokenInfoDao.updateTokenType(tokens.getId(), 1);
                    return;
                }
                Utf8String tokenURL = vm30Utils.tokenURL(web3j, contract, tokenId);
                if(StringUtils.isEmpty(tokenURL.toString())){
                    tokens.setTokenType(1);
                    tokenInfoDao.updateTokenType(tokens.getId(), 1);
                    return;
                }
                nftAccount.setNftData(tokenURL.toString());
                nftAccount.setNftId(tokenId.longValue());
                nftAccount.setCreateTime(new Date().toInstant());
                nftAccount.setUpdateTime(new Date().toInstant());
            }   catch (Exception e) {
                log.info("updateNftAccount.");
            }
            nfts.add(nftAccount);
        }
        nftAccountDao.saveAll(nfts);
    }


    /**
     * 校验tokenType
     *
     * @param contract
     */
    private void checkTokenType(String contract, String fromAddr, TokenInfo tokenInfo,BigInteger decimals) {
        boolean erc20  = false;
        boolean erc721  = false;

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

           erc20  = transfer && allowance && totalSupply && balanceOf && transferFrom;
           erc721 = balanceOf && ownerOf && safeTransferFrom && transferFrom && approve && setApprovalForAll && getApproved && isApprovedForAll;

            if (erc721) {
                tokenInfo.setTokenType(2);
            } else if (erc20) {
                tokenInfo.setTokenType(1);
            } else {
                tokenInfo.setTokenType(0);
            }
            if(erc20 && decimals.intValue() != 0  ){
                tokenInfo.setTokenType(1);
            }
        } catch (Exception e) {
            log.error("识别合约类型异常:" + e.getMessage());
           // e.printStackTrace();
            tokenInfo.setTokenType(0);
            if(erc20 && decimals.intValue() != 0  ){
                tokenInfo.setTokenType(1);
            }
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
            //创建合约交易
            if(item.getInput().length() > 10){
                String function = item.getInput().substring(0, 10);
                if(function.equals("0x60806040") && ((EthBlock.TransactionObject) result).getCreates() != null){
                    //设置to地址为合约地址
                    tx.setToAddr(((EthBlock.TransactionObject) result).getCreates());
                }
                if(function.equals("0x60e06040") && ((EthBlock.TransactionObject) result).getCreates() != null){
                    //设置to地址为合约地址
                    tx.setToAddr(((EthBlock.TransactionObject) result).getCreates());
                }
            }
            //合约地址存储
            tx.setContractAddress(((EthBlock.TransactionObject) result).getCreates());

            tx.setCreateTime(new Date());
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


    public static void main(String[] args) {
//        String str ="[\"0x2f8788117e7eff1d82e926ec794901d17c78024a50270940304540a733656f0d\",\"0x0000000000000000000000000000000000000000000000000000000000000000\",\"0x0000000000000000000000006da573eec80f63c98b88ced15d32ca270787fb5a\",\"0x0000000000000000000000006da573eec80f63c98b88ced15d32ca270787fb5a\"]";
//        JSONArray jsonArray = JSONArray.parseArray();
//        System.out.println(jsonArray);
    }


}

