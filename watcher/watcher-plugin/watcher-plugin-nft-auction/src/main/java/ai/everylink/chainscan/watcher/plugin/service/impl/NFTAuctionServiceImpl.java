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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.nio.charset.StandardCharsets;
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
                    } else if (params.size() > 2 && (method.contains("0x848e5c77")  || method.contains("0xc24d5a5c"))) {
                        finishNftAuction(transaction, params);
                        //监控NFT 拍卖取消交易
                    }else if (params.size() > 2 && method.contains("0x848e5c77")) {
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
        }
        boolean result = updateNftAccount(transaction.getFromAddr(), nftContractAddress);

        Utf8String tokenURL = vm30Utils.tokenURL(web3j, nftContractAddress,new BigInteger(tokenId.toString()));
        if(tokenURL != null && StringUtils.isNotBlank(tokenURL.toString())){
            //拍卖需求增加字段;
            String nftData = tokenURL.toString();
            String[] split = nftData.split(",");
            if(split.length >1){
                try {
                    Base64.Decoder decoder = Base64.getDecoder();
                    String  decodeNftData = new String(decoder.decode(split[1]), StandardCharsets.UTF_8);
                    JSONObject  data = JSON.parseObject(decodeNftData);
                    if(data.getBoolean("explicit")!=null){
                        nftAuction.setNftExplicit(data.getBoolean("explicit"));
                    }
                    if(data.getString("name")!=null){
                        nftAuction.setNftName(data.getString("name"));
                    }
                    if(data.getString("stats")!=null){
                        nftAuction.setNftStats(data.getString("stats"));
                    }
                    if(data.getString("external_link")!=null){
                        nftAuction.setNftExternalLink(data.getString("external_link"));
                    }
                    if(data.getString("levels")!=null){
                        nftAuction.setNftLevels(data.getString("levels"));
                    }
                    if(data.getString("unlockable_content")!=null){
                        nftAuction.setNftUnlockableContent(data.getString("unlockable_content"));
                    }
                    if(data.getString("description")!=null){
                        nftAuction.setNftDescription(data.getString("description"));
                    }
                    if(data.getString("attributes")!=null){
                        nftAuction.setNftAttributes(data.getString("attributes"));
                    }
                } catch (Exception e) {
                    log.error("解析 nftData 异常 hash:" + transaction.getTransactionHash());
                }
            }
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
        //String   nftData = "data:application/json;base64,eyJuYW1lIjoieWhuX3dvcmRfdGVzdDUiLCJleHRlcm5hbF9saW5rIjoiIiwibGV2ZWxzIjpbXSwic3RhdHMiOltdLCJ1bmxvY2thYmxlX2NvbnRlbnQiOiIiLCJleHBsaWNpdCI6ZmFsc2UsImRlc2NyaXB0aW9uIjoiIiwiYXR0cmlidXRlcyI6W10sImltYWdlIjoiZGF0YTppbWFnZS9wbmc7YmFzZTY0LGlWQk9SdzBLR2dvQUFBQU5TVWhFVWdBQUFFQUFBQUJBQ0FZQUFBQ3FhWEhlQUFBSlJFbEVRVlI0WHUxYkMyeFQ1eFgrenIxK1FXTEhUa2g0RnNKamdSVW9pQVFLSlFXeXRadTZWbnUybXNURUJoWHJxbTVyTjlLT3FhVkFCNW5XRmxBZjZqcXRVbUdqM1VQYlZFM3JPblhUeGl2bFVjSzZBdWtLNWJVMkpRMkpFOXZYUVB5NDkwei9kUXlPWTEvYjF6Yk41UDFTcEVqKy8vUDR6ajNuUCtmOC8wOG84bEFVcFRxc1lna0lNd21vWSticFJLZ0N5QWxBL0ltaEFLd3d3MHRFSnhnNENVYTdUY1plcDlQWlhVd1JxUmpFZmI2TDlTcXB5OEc0RmNBc0FHYjVNSURqSVB4Tlp2bFhibmZaa1VMTGExYXdJWEowZDNjN0pmdUllNGg1RlRQUExMU2dnaDRSdFRQUmRpMTArZWZWMWRWS0lYamtEVUFmczFzTEJCOEE4LzBBS2dzaFZCWTBla0gwak9RcWY5cEQ1TXRpZnRvcHBnRmdadklwRjcraGF0b1RCRlRuSTRUWnRReDB5NUwwQTdlejdCZEVKTndsNTJFS2dDNmZiNnFWNUIzTTNKZ3p4eUlzSUtMV0NLc3JSN3ZkcDNNbG56TUFYcDl5RjhBdkFLaklsVm1SNS90bG1WYTduYzdmNThJbmF3RDBUOTZ2UE1sRWE4VC91VEM1Vm5PRkd4RHpObmVGODZGc1hTSXJSWmpaMmhjSWJtZm1yMTByWmZMaFEwUXZlMXpscTRnb2tvbE9SZ0JpeWl1dk1PUDJUTVNHMCs5RStMUEg1ZnhTSmhBTUFSQ2ZlbDhndVBOL3hmTEpCaGo0RWxZWXVZTWhBSDIrd0JZTmFCNU9sczFWRmduWTZuRzdIa3kzTGkwQUl0b1Q0YmZETmVCbEM0U3dQak8rV3VWMi9pN1ZtcFFBaUgzZUFrbmszY050cTh0VzcrUjUvaWkwK2xSNXdoQUFCdngrNzNCSmNzeHFuQ0lldEhwYzVVdVM0OEVRQUx4OWdWVWd2RmdveHNPS0R1UHVLbzlyZTZKTWd3QVFoWTNxVjA1K1hMbDlzY0hTYTRjS1oxMWlBVFVJQUs5ZjJRRG1qY1VVNUZJRTZGUWsrRUtFL2lnZ0VURFN5cWdwWTlTTVpNaFNNYm5yTmZYR3Fncm5ZM0V1VndEUTYzbXIvVnloUzlxb0J2eXJTOEtCRGd2YUwwZzRIMHl2b1ZWaTFGVnBtRHRheGVLSktpWTRUUlY0bVJEczFTS2gybmcvNFFvQTNrQ3dHWnEySmRQcWJIOFBob0hYVGxudzZrbXJibTB6WTJhMWlpL1BpR0QrT00zTTh2UnJKT25CS2xmNVZqSGhLZ0I5Z1hZUXJzK1hFelB3K2hrTFhqcG1SY0NrNHNreXpLcFc4YTE1WVV4eUYrYUxFSjJseWdxbmFOWEZBTkI3ZUZEYjhsWGUxdzg4ZGNpT2YzNGs1MHRxeUhyaEhpdm5SSEJIWGRSMGd6R1JxQXk1UWZRWWRRQzgvc0JXTU5ia0kzVkhnTEJ4angwWExoVTNpdDB5T1lKdk4wVHlENWFFYlZVVnJ1WVlBTDdBVVFDenpRSndxcGV3Zm84RHdiQ3hyMXNreHZRcURYV1ZHa2FYTThxc0RGV0RIaU02QWhMYXV5VjBHZ1RKdUh6eng2bjQ0VTBoV1BQNzBJNVZ1VjAzVUt4dnoxMW1XOWRDZ2EvL2NRUVVBK1hIbEduNDRvd29sa3lNb3R4bURQTlpuNFRYM3JQZ0grZGtSTFQwZ0s2NElZeTdQaGsxYXpPeGptMHlqU2F2VC9rS3dEbTFrWks1dHJUYWNPaER5eEJoaE1XWHo0emlDek1pc09ib0daMEs0YWRIYkhpN2E2aVp4WmZUMHRTUEtaNThneUxkU1Y2L3NoN01WeElETTVDS3ZmNkovVFljVEFEQjQyQThlbk0vcGxXYUYxTHNLTDlwdCtMWDdkWXJZcFhiR0Q5YW1oL2RLOFNJTmxDUFQzbVp3TXZOS0o2NFJvQ3c5YUFOYjN4Z1FVMlpocFpsSWQzUEN6SCtjc3FDbngyeHdTbVVYMVlJeThla0VnMFQ4dm9DaHdFMEZFSlFFUS8rOEs0RlRiVXFxa2NXUnZtNFhJYytsREcyWE1QRWlvTFNiUk1BbkFWUW13MEFSem9sN0R4cXcvSlpFU3dZcjJhekpPMmNua3VFYlFkdG1ERkt3NHJaRVpDNVpGR25MNEIvL29nTnZaY0pheGFHTWdiYUJLSE9VYTlmNldibVVabTBFUlo0Zkw4TlVZMGdFK09oUldIY2RKMDVFQzVjSkR5eXk0NnVpN0hJZU51MENPNmRadzZFNVBnejFhUHBNY0pwejZTUjdnSTlBb0IrWmphY2ZxQkQxb09jbW5BY0lLcTQ1b1VoM0R3eE54QStDc2FVNzA1S21ENHpKWUw3R2lKNmRaanRpS2pBVDk2dzQzRG40SjJpdGtMRHBtVWhWRGlNM1lXSVFoa0I4RjRpckg3Vk1VajV1SUJDMkFjV2hORlVtOTErZkY0Unlqdmd2WnhheTAvWFJ2SGRCZUdzUUFpcndJOWIwNmZkTjQ2UDRwSEdzQ0dXY1FBTVhVQ2cvUEF1QjA1NFUyL2tRaFVoOUMyVGpVRVFxZks2WFE3MDlodWJlT21rS0w2M0lHeVk2b2Fpd09aV2U4b2NJYTd4dmZWaGZHNmFzVXdETGhBNHc0ekpSbENKSnNaamV4MzRkMDk2RU81ckNPT3pVMU16Zk45UFdMZmJBVjhHNWVNeU5GNFhSZlBDMUNDSUpzcW1mWFljdTVBK0Q3NW5YaGgzZkNMelYwbUVzMWx2ZzVjak1jYkh1OU16RmlYcjdVbU16L2tJais1MndKOWphYnhvUWxRUHRKWUV6SVVNd2hEdkdCaENXUDYyREpaUE1IYWJpQUV2Wlh2eUk5RGZ2TStPb3dib3I1NGJ4dWVueDlBLzNVZFl2OXVSdGs0UUphNVJ2cjlnbklxMUEwWFB4UWl3Y1kreEszNW5maGkzVHNscytUZ0FzVVFveDFSWUJKK1dWanZlTXFqNVY4NEpZM2FOaWcwR0ZhTGJ3ZGk4ckI5aWU5MTVMSDJGVkQ5V3hmM3p3N3JQdjllYjJnVkZNQlp6UHBVaERnMXhjNUVLbXltR0JBaGkrMmxMMm40U0djZ0VxR2wyb2NvUnNXSm0vRURQNzVWM0xkaitkbm9RakdnSjViOS9Zd2hMSitXMkhROGt3M2VhTG9mRjdpQnlnMFBuaDFhQlJnRjExRWhHeTdKK2pFMXFlUDdwcEFVdnZKV2hWazRpTElCcFhoUkNvN21FTEZZT0M1cG1HeUlpQzl0eXdJYjlIZG1CVUROU3crYW1FTWFrS1pKRUUxVVVQZGtNVVdxTElMbG9naG5MNnh4aURSRWRnRHhhWWlJUEZ6bjl2ZytNUVJCTkVhRzg2UDhiamIrZXR1QzVOaHVNWm9uZ3VYWnhHQ0pJbWg2SkxiRjhtNklDaEtmZnRHSDNmMUtESUtxNGxxWVF4T2VmemZqN1dRdWVlVE0xQ0ZhWjhmRGlFT3JINXRjcUg5UVVGVUwxK3BYaitWeHcxQmg0OXJBTlF2akVNZDRaVTE0RXZseUdBUE9wUXpZSXV2RmhreG5yR2tPWU95WS81WWUweFhVM0tNREJpQkQyK1RZclhqOFQ2K0JNZEduWTFOUVBqeU1YMWEvTzNmZStqRzBIN2ZwdVlwY1o2NWVFTUxzbVArVjE2cWtPUmdwMU5DYmFXTDg4YXNVNXY2VG45Smtxc2t6UUhPeVE5U2JMcWprUlhGOWRBT1dCMUVkanNXQlkvTVBSVEFvWC9mZDBoNk9DY2NrZmorc2dCSUlyTlUwYmRJbWc2RmE1Umd3a1NWcmxjWlh2U0dUMy95c3lxY0F2NlV0U2NVQksrcHBjSElTU3ZpZ3BRQ2o1cTdJRElKVHVaZW00SzVUMGRma0VFRXIzd1VUaVZqbWNuOHdBOU0xMGw2TFQ1Vm81SEVSZEpWSFNqNllTWGFMWHA2eGt3dU1mMTlWYWNmV1ZHR3NyM2M0ZDJiNFJTdjRTVEgwQmlVUks5dUZrTXBMeHA3TlF0YnNMY2VFeXBjOHkzb0VzdlRpc25zNm1FclFrSDArbmk3S0p6K2VaTVozQWRRQkdFVkY1NHZONVpnNEM2R0hRU1NLY3VGYlA1LzhMWjdkV3lHQ3NrOGNBQUFBQVNVVk9SSzVDWUlJPSIsIm5mdF9pZCI6MX0=";
        String   nftData = "data:application/json;base64,ewogICJuYW1lIjogIkFscGhhYmV0IiwKICAiZGVzY3JpcHRpb24iOiAiQWxwaGFiZXQgTkZULi4uIiwKICAiYXR0cmlidXRlcyI6IFsKICAgIHsKICAgICAgInRyYWl0X3R5cGUiOiAidG9rZW5Mb2NrVGltZSIsCiAgICAgICJ2YWx1ZSI6ICIyMDI1MDMyOCIKICAgIH0sCiAgICB7CiAgICAgICJ0cmFpdF90eXBlIjogInRva2VuQ29pbkFtb3VudCIsCiAgICAgICJ2YWx1ZSI6ICI1MDAwIgogICAgfSwKICAgIHsKICAgICAgInRyYWl0X3R5cGUiOiAiY29sb3IiLAogICAgICAidmFsdWUiOiAiZ29sZGVuIgogICAgfSwKICAgIHsKICAgICAgInRyYWl0X3R5cGUiOiAibGV2ZWwiLAogICAgICAidmFsdWUiOiAiNiIKICAgIH0sCiAgICB7CiAgICAgICJ0cmFpdF90eXBlIjogInRva2VuV29yZCIsCiAgICAgICJ2YWx1ZSI6ICJCSVRDT0lOIgogICAgfQogIF0sCiAgImltYWdlIjogImRhdGE6aW1hZ2Uvc3ZnK3htbDtiYXNlNjQsUEhOMlp5QjNhV1IwYUQwaU5UQTRJaUJvWldsbmFIUTlJalV3T0NJZ2RtbGxkMEp2ZUQwaU1DQXdJRE13TUNBek1EQWlJR1pwYkd3OUltNXZibVVpSUhodGJHNXpQU0pvZEhSd09pOHZkM2QzTG5jekxtOXlaeTh5TURBd0wzTjJaeUkrQ2lBZ0lDQThjR0YwYUNCa1BTSk5NQ0F3U0RJNU9WWXlPREZJTUZZd1dpSWdabWxzYkQwaWRYSnNLQ053WVdsdWREQmZjbUZrYVdGc1h6RTNNRjh4TkRVNE16Y3BJaTgrQ2lBZ0lDQThiV0Z6YXlCcFpEMGliV0Z6YXpCZk1UY3dYekUwTlRnek55SWdjM1I1YkdVOUltMWhjMnN0ZEhsd1pUcGhiSEJvWVNJZ2JXRnphMVZ1YVhSelBTSjFjMlZ5VTNCaFkyVlBibFZ6WlNJZ2VEMGlNQ0lnZVQwaU1DSWdkMmxrZEdnOUlqSTVPU0lnYUdWcFoyaDBQU0l5T0RFaVBnb2dJQ0FnSUNBZ0lEeHdZWFJvSUdROUlrMHdJREJJTWprNVZqSTRNVWd3VmpCYUlpQm1hV3hzUFNKMWNtd29JM0JoYVc1ME1WOXlZV1JwWVd4Zk1UY3dYekUwTlRnek55a2lMejRLSUNBZ0lEd3ZiV0Z6YXo0S0lDQWdJRHhuSUcxaGMyczlJblZ5YkNnamJXRnphekJmTVRjd1h6RTBOVGd6TnlraVBnb2dJQ0FnSUNBZ0lEeDBaWGgwSUdadmJuUXRjMmw2WlQwaU5UQWlJR1p2Ym5RdFptRnRhV3g1UFNKTWRXTnBaR0VnUTJGc2JHbG5jbUZ3YUhraUlHWnZiblF0YzNSNWJHVTlJbTV2Y20xaGJDSWdkR1Y0ZEMxaGJtTm9iM0k5SW0xcFpHUnNaU0lnWkc5dGFXNWhiblF0WW1GelpXeHBibVU5SW0xcFpHUnNaU0lLSUNBZ0lDQWdJQ0FnSUNBZ0lDQjVQU0kxTUNVaUlIZzlJalV3SlNJZ1ptbHNiRDBpZFhKc0tDTndZV2x1ZERCZmJHbHVaV0Z5WHpFM01GOHhOVE01TkRJcElqNEtJQ0FnSUNBZ0lDQWdJQ0FnUWtsVVEwOUpUZ29nSUNBZ0lDQWdJRHd2ZEdWNGRENEtJQ0FnSUR3dlp6NEtJQ0FnSUR4emRIbHNaU0IwZVhCbFBTSjBaWGgwTDJOemN5SStDaUFnSUNBZ0lDQWdRR1p2Ym5RdFptRmpaU0I3Q2lBZ0lDQWdJQ0FnSUNBZ0lHWnZiblF0Wm1GdGFXeDVPaUFpVEhWamFXUmhJRU5oYkd4cFozSmhjR2g1SWpzS0lDQWdJQ0FnSUNBZ0lDQWdjM0pqT2lCMWNtd29JbWgwZEhCek9pOHZaR3BuY3podGRXWm5PVFl3Tmk1amJHOTFaR1p5YjI1MExtNWxkQzkwZEdZdlRIVmphV1JoSUVOaGJHeHBaM0poY0doNUxuUjBaaUlwT3dvZ0lDQWdJQ0FnSUgwS0lDQWdJRHd2YzNSNWJHVStDaUFnSUNBOFpHVm1jejRLSUNBZ0lDQWdJQ0E4Y21Ga2FXRnNSM0poWkdsbGJuUWdhV1E5SW5CaGFXNTBNRjl5WVdScFlXeGZNVGN3WHpFME5UZ3pOeUlnWTNnOUlqQWlJR041UFNJd0lpQnlQU0l4SWlCbmNtRmthV1Z1ZEZWdWFYUnpQU0oxYzJWeVUzQmhZMlZQYmxWelpTSWdaM0poWkdsbGJuUlVjbUZ1YzJadmNtMDlJblJ5WVc1emJHRjBaU2d4TXpFdU5EUTRJREUwTkM0ek1qTXBJSEp2ZEdGMFpTZzJNeTQxTURjNEtTQnpZMkZzWlNneE9UUXVNell4SURJd01pNHdNRFlwSWo0S0lDQWdJQ0FnSUNBZ0lDQWdQSE4wYjNBZ2MzUnZjQzFqYjJ4dmNqMGlJekpGTkRrMk1DSXZQZ29nSUNBZ0lDQWdJQ0FnSUNBOGMzUnZjQ0J2Wm1aelpYUTlJakF1TkRneU5UUXhJaUJ6ZEc5d0xXTnZiRzl5UFNJak1qZ3pSalUwSWk4K0NpQWdJQ0FnSUNBZ0lDQWdJRHh6ZEc5d0lHOW1abk5sZEQwaU1TSWdjM1J2Y0MxamIyeHZjajBpSXpGRU16QTBNU0l2UGdvZ0lDQWdJQ0FnSUR3dmNtRmthV0ZzUjNKaFpHbGxiblErQ2lBZ0lDQWdJQ0FnUEhKaFpHbGhiRWR5WVdScFpXNTBJR2xrUFNKd1lXbHVkREZmY21Ga2FXRnNYekUzTUY4eE5EVTRNemNpSUdONFBTSXdJaUJqZVQwaU1DSWdjajBpTVNJZ1ozSmhaR2xsYm5SVmJtbDBjejBpZFhObGNsTndZV05sVDI1VmMyVWlJR2R5WVdScFpXNTBWSEpoYm5ObWIzSnRQU0owY21GdWMyeGhkR1VvTVRNNExqQTFPU0F4T0RZdU9EVTFLU0J5YjNSaGRHVW9Oakl1TWpJNU15a2djMk5oYkdVb01qRXhMakUzT1NBek5EVXVNRFkxS1NJK0NpQWdJQ0FnSUNBZ0lDQWdJRHh6ZEc5d0lITjBiM0F0WTI5c2IzSTlJaU5HUmtVd056UWlMejRLSUNBZ0lDQWdJQ0FnSUNBZ1BITjBiM0FnYjJabWMyVjBQU0l3TGpJeU9EWXlPQ0lnYzNSdmNDMWpiMnh2Y2owaUkwWkdSRGMwT1NJdlBnb2dJQ0FnSUNBZ0lDQWdJQ0E4YzNSdmNDQnZabVp6WlhROUlqQXVNemd3TnpnMUlpQnpkRzl3TFdOdmJHOXlQU0ozYUdsMFpTSXZQZ29nSUNBZ0lDQWdJQ0FnSUNBOGMzUnZjQ0J2Wm1aelpYUTlJakF1TlRFMk1ESTBJaUJ6ZEc5d0xXTnZiRzl5UFNJak56azJRekkySWk4K0NpQWdJQ0FnSUNBZ0lDQWdJRHh6ZEc5d0lHOW1abk5sZEQwaU1DNDFPRGM1TmpFaUlITjBiM0F0WTI5c2IzSTlJaU5HTjBSQ00wSWlMejRLSUNBZ0lDQWdJQ0FnSUNBZ1BITjBiM0FnYjJabWMyVjBQU0l3TGpZNE1EWXdOeUlnYzNSdmNDMWpiMnh2Y2owaUkwWkdSakpCT0NJdlBnb2dJQ0FnSUNBZ0lDQWdJQ0E4YzNSdmNDQnZabVp6WlhROUlqQXVOemd6TkRBM0lpQnpkRzl3TFdOdmJHOXlQU0lqUWtaQlJUUTFJaTgrQ2lBZ0lDQWdJQ0FnSUNBZ0lEeHpkRzl3SUc5bVpuTmxkRDBpTVNJZ2MzUnZjQzFqYjJ4dmNqMGlJMFpHUmpsRE1DSXZQZ29nSUNBZ0lDQWdJRHd2Y21Ga2FXRnNSM0poWkdsbGJuUStDaUFnSUNBZ0lDQWdQR3hwYm1WaGNrZHlZV1JwWlc1MElHbGtQU0p3WVdsdWREQmZiR2x1WldGeVh6RTNNRjh4TlRNNU5ESWlJSGd4UFNJdE16WXVNVEF6TnlJZ2VURTlJakl4TGpBeU56SWlJSGd5UFNJeU16Y3VOVFk0SWlCNU1qMGlNek01TGprNE55SWdaM0poWkdsbGJuUlZibWwwY3owaWRYTmxjbE53WVdObFQyNVZjMlVpUGdvZ0lDQWdJQ0FnSUNBZ0lDQThjM1J2Y0NCemRHOXdMV052Ykc5eVBTSWpSVFZFUVVJMklpOCtDaUFnSUNBZ0lDQWdJQ0FnSUR4emRHOXdJRzltWm5ObGREMGlNQzR4T1RVME16a2lJSE4wYjNBdFkyOXNiM0k5SWlOR1JrWTBSRU1pTHo0S0lDQWdJQ0FnSUNBZ0lDQWdQSE4wYjNBZ2IyWm1jMlYwUFNJd0xqUTNORGcxT0NJZ2MzUnZjQzFqYjJ4dmNqMGlJME5DUWpFNE5DSXZQZ29nSUNBZ0lDQWdJQ0FnSUNBOGMzUnZjQ0J2Wm1aelpYUTlJakF1TmpnM01qVTVJaUJ6ZEc5d0xXTnZiRzl5UFNJalEwTkNNemcySWk4K0NpQWdJQ0FnSUNBZ0lDQWdJRHh6ZEc5d0lHOW1abk5sZEQwaU1TSWdjM1J2Y0MxamIyeHZjajBpSTBWRFJFTkNReUl2UGdvZ0lDQWdJQ0FnSUR3dmJHbHVaV0Z5UjNKaFpHbGxiblErQ2lBZ0lDQThMMlJsWm5NK0Nqd3ZjM1puUGdvPSIKfQo=";

        String[] split = nftData.split(",");
        //System.out.println(split[1]);

        try {
            Base64.Decoder decoder = Base64.getDecoder();
            String  decodeNftData = new String(decoder.decode(split[1]), StandardCharsets.UTF_8);
            JSONObject  data = JSON.parseObject(decodeNftData);
            System.out.println("explicit = " + data.getBoolean("explicit"));
            System.out.println("name = " + data.getString("name"));
            System.out.println("stats = " + data.getString("stats"));
            System.out.println("external_link = " + data.getString("external_link"));
            System.out.println("levels = " + data.getString("levels"));
            System.out.println("unlockable_content = " + data.getString("unlockable_content"));
            System.out.println("description = " + data.getString("description"));
            System.out.println("attributes = " + data.getString("attributes"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

