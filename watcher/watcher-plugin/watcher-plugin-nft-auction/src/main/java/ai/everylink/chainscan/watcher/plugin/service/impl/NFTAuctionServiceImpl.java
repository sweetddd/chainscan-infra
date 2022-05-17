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
import ai.everylink.chainscan.watcher.core.util.OkHttpUtil;
import ai.everylink.chainscan.watcher.core.util.VM30Utils;
import ai.everylink.chainscan.watcher.core.util.WatcherUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.dao.*;
import ai.everylink.chainscan.watcher.entity.*;
import ai.everylink.chainscan.watcher.plugin.constant.NFTAuctionConstant;
import ai.everylink.chainscan.watcher.plugin.service.NFTAuctionService;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.*;

/**
 * NFT 拍卖 扫描
 *
 * @author brett
 * @since 2022-05-05
 */
@Slf4j
@Service
public class NFTAuctionServiceImpl implements NFTAuctionService {

    private static final String NFTAUCTION_FINISH_TOPIC ="0x8df77c988c9550e96e43a66277f716818a74ed2188cdacb49d790623e6f22571";

    private Web3j web3j;

    @Autowired
    private VM30Utils vm30Utils;

    @Autowired
    Environment environment;

    @Autowired
    private TokenInfoDao tokenInfoDao;

    @Autowired
    private AccountInfoDao accountInfoDao;

    @Autowired
    private NftAccountDao  nftAccountDao;

    @Autowired
    private NftAuctionDao nftAuctionDao;

    @Autowired
    private TransactionLogDao transactionLogDao;

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
    public void nftAuctionScan(EvmData blockData) {
        String            nftAuctionContracts = environment.getProperty("watcher.nft.auction.address");
       // String            nftAuctionContracts = "0x9b38f6fa3943c24f4998d73d178fb1e2899a1365";
        int               chainId             = blockData.getChainId();
        List<Transaction> txList              = buildTransactionList(blockData, chainId);
        // 事件监听 解析;
        for (Transaction transaction : txList) {
            String toAddr = transaction.getToAddr();
            int txSatte = Integer.parseInt(transaction.getStatus().replace("0x", ""), 16);

            if (StringUtils.isNotBlank(toAddr) && nftAuctionContracts != null && nftAuctionContracts.equals(toAddr) && txSatte == 1) {
           // if (StringUtils.isNotBlank(toAddr) ) {
                String input = transaction.getInput();
                if (StringUtils.isNotBlank(input) && input.length() > 10) {
                    List<String> params = DecodUtils.getParams2List(input);
                    String       method = params.get(0);
                    //监控NFT 拍卖创建交易
                    if (params.size() > 2 && method.contains("0x8fa4a10f")) {
                        createNewNftAuction(transaction, params);
                        //监控NFT 拍卖成交交易
                    } else if (params.size() > 2 && method.contains("0x848e5c77")) {
                        finishNftAuction(transaction, params);
                        //监控NFT 拍卖取消交易
                    }
                    else if (params.size() > 2 && method.contains("0x848e5c77")) {
                        cancelNftAuction(transaction, params);
                    }
                }
             //}
            }
        }
    }

    //监控NFT 拍卖创建交易
    private void createNewNftAuction(Transaction transaction, List<String> params) {
        NftAuction nftAuction         = new NftAuction();
        String nftContractAddress = "0x" + params.get(1).substring(params.get(1).length() - 40);
        TokenInfo  nftContract        = tokenInfoDao.findAllByAddress(nftContractAddress);
        if (Objects.isNull(nftContract)) {
            nftContract = addTokenInfo(nftContractAddress, transaction.getFromAddr());
        }
        nftAuction.setTokenId(nftContract.getId());
        Integer tokenId = Integer.parseInt(params.get(2), 16);
        nftAuction.setNftId(tokenId.longValue());
        //获取NFT的data信息;
        NftAccount  nftAccount = nftAccountDao.selectByTokenIdContract(tokenId.longValue(), nftContract.getId());
        if(nftAccount != null){
            nftAuction.setNftData(nftAccount.getNftData());
        }else {
            boolean result = updateNftAccount(transaction.getFromAddr(), nftContractAddress);
//            if(result){
//                createNewNftAuction(transaction, params);
//            }
        }
        String erc20Token = "0x" + params.get(3).substring(params.get(3).length() - 40);
        nftAuction.setPayToken(erc20Token);
        Long minPrice = Long.parseLong(params.get(4), 16);
        nftAuction.setMinPrice(minPrice);
        Long buyNowPrice = Long.parseLong(params.get(5), 16);
        nftAuction.setBuyNowPrice(buyNowPrice);
        Long auctionBidPeriod = Long.parseLong(params.get(6), 16);
        nftAuction.setAuctionBidPeriod(auctionBidPeriod);
        long time = transaction.getTxTimestamp().getTime()/1000;
        nftAuction.setAuctionEnd(time + auctionBidPeriod);
        Long bidIncreasePercentage = Long.parseLong(params.get(7), 16);
        nftAuction.setBidIncreasePercentage(bidIncreasePercentage);
        Long feePercentages = Long.parseLong(params.get(14), 16);
        nftAuction.setFeePercentages(feePercentages);
        String feeRecipients = "0x" + params.get(12).substring(params.get(3).length() - 40, params.get(3).length());
        nftAuction.setFeeRecipients(feeRecipients);
        nftAuction.setState(NFTAuctionConstant.STATE_CREAT);
        nftAuction.setCreateTime(new Date().toInstant());
        nftAuction.setAccountAddress(transaction.getFromAddr());
        nftAuction.setNftContractAddress(nftContractAddress);
        nftAuction.setDeleted(false);
        nftAuctionDao.save(nftAuction);
    }

    //监控NFT 拍卖取消交易
    private void cancelNftAuction(Transaction transaction, List<String> params) {

    }

    //监控NFT 拍卖成交
    private void finishNftAuction(Transaction transaction, List<String> params) {
        List<TransactionLog> txLog = transactionLogDao.findByTxHash(transaction.getTransactionHash());
        for (TransactionLog transactionLog : txLog) {
            String topicsStr = transactionLog.getTopics();
            //topics转为数组
            JSONArray topics = JSONArray.parseArray(topicsStr);
            if (topics.size() > 0) {
                String topic = topics.get(0).toString();
                if (topic.equals(NFTAUCTION_FINISH_TOPIC)) {
                    String data = transactionLog.getData();
                    String substring = data.substring(2, 66);
                    String nftContractAddress ="0x" +  substring.substring(substring.length() - 40);
                    Long tokenId = Long.parseLong(data.substring(66, 130), 16) ;
                    nftAuctionDao.finish(nftContractAddress, tokenId);
                    boolean result = updateNftAccount(transaction.getFromAddr(), nftContractAddress);
                  //  System.out.println(result);
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

    /**
     * 2
     * 触发是否 增加token信息;
     *
     * @param toAddr
     */
    private TokenInfo addTokenInfo(String toAddr, String fromAddr) {
        TokenInfo tokenQuery = new TokenInfo();
        try {
            String symbol = vm30Utils.symbol(web3j, toAddr).toString();
            if (StringUtils.isBlank(symbol)) {
                return tokenQuery;
            }
            String name = vm30Utils.name(web3j, toAddr).toString();
            if (StringUtils.isBlank(name)) {
                return tokenQuery;
            }
            BigInteger decimals = vm30Utils.decimals(web3j, toAddr);
            if (StringUtils.isNotBlank(symbol) && StringUtils.isNotBlank(name)) {

                //判断合约类型
                checkTokenType(toAddr, fromAddr, tokenQuery, decimals);
                tokenQuery.setTokenName(name);
                tokenQuery.setTokenSymbol(symbol);
                tokenQuery.setDecimals(decimals);
                tokenQuery.setAddress(toAddr);
                tokenQuery.setCreateTime(new Date());
                tokenQuery.setCreateTime(new Date());
                tokenInfoDao.save(tokenQuery);
            }
        } catch (Exception e) {
            log.error("addToken error", e);
        }
        return tokenQuery;
    }

    /**
     * 校验tokenType
     *
     * @param contract
     */
    private void checkTokenType(String contract, String fromAddr, TokenInfo tokenInfo, BigInteger decimals) {
        boolean erc20  = false;
        boolean erc721 = false;

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

            erc20 = transfer && allowance && totalSupply && balanceOf && transferFrom;
            erc721 = balanceOf && ownerOf && safeTransferFrom && transferFrom && approve && setApprovalForAll && getApproved && isApprovedForAll;

            if (erc721) {
                tokenInfo.setTokenType(2);
            } else if (erc20) {
                tokenInfo.setTokenType(1);
            } else {
                tokenInfo.setTokenType(0);
            }
            if (erc20 && decimals.intValue() != 0) {
                tokenInfo.setTokenType(1);
            }
        } catch (Exception e) {
            log.error("识别合约类型异常:" + e.getMessage());
            // e.printStackTrace();
            tokenInfo.setTokenType(0);
            if (erc20 && decimals.intValue() != 0) {
                tokenInfo.setTokenType(1);
            }
        }
    }


    @Transactional
    public boolean updateNftAccount(String fromAddr, String contract) {
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
        if( tokens == null ){
            return false;
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
                    return false;
                }
                Utf8String tokenURL = vm30Utils.tokenURL(web3j, contract, tokenId);
//                if(StringUtils.isEmpty(tokenURL.toString())){
//                    tokens.setTokenType(1);
//                    tokenInfoDao.updateTokenType(tokens.getId(), 1);
//                    return false;
//                }
                nftAccount.setNftData(tokenURL.toString());
                nftAccount.setNftId(tokenId.longValue());
            }   catch (Exception e) {
                log.info("updateNftAccount.");
            }
            nfts.add(nftAccount);
        }
        nftAccountDao.saveAll(nfts);
        return true;
    }

    public static void main(String[] args) {
        String       input            = "0x00000000000000000000000055da635ee54370941e6d7abacf6cf53d80ea4e0700000000000000000000000000000000000000000000000000000000000000090000000000000000000000000a079069bd5995a8c65f9b9bacb53ff326f7a2c50000000000000000000000000000000000000000000000000de0b6b3a764000000000000000000000000000010b77a65becc87657f7497e99ffc6e25f50b197900000000000000000000000010b77a65becc87657f7497e99ffc6e25f50b1979";
//        List<String> params           = DecodUtils.getParams2List(input);
//        Long         auctionBidPeriod = Long.parseLong(params.get(6), 16);
//        // System.out.println(buyNowPrice);
//        System.out.println(params.size());
//        System.out.println(params);
        System.out.println(input.substring(2,66));
        System.out.println(input.substring(66,130));


    }


}

