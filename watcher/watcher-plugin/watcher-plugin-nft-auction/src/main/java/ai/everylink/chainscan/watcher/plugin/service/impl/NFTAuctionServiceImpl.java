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
import ai.everylink.chainscan.watcher.core.util.VM30Utils;
import ai.everylink.chainscan.watcher.core.util.WatcherUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.dao.*;
import ai.everylink.chainscan.watcher.entity.*;
import ai.everylink.chainscan.watcher.plugin.bean.CreateNewNftAuctionBean;
import ai.everylink.chainscan.watcher.plugin.service.NFTAuctionService;
import ai.everylink.chainscan.watcher.plugin.strategy.ErcNftFactory;
import ai.everylink.chainscan.watcher.plugin.strategy.ErcNftService;
import ai.everylink.chainscan.watcher.plugin.strategy.ErcTypeNftEnum;
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

    public static final String NFTAUCTION_FINISH_TOPIC ="0x8df77c988c9550e96e43a66277f716818a74ed2188cdacb49d790623e6f22571";
    public static final String NFTAUCTION_CANCEL_TOPIC ="0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

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
        String nftAuctionContracts = environment.getProperty("watcher.nft.auction.address");
        log.info("nftAuctionScan.nftAuctionContracts:{}", nftAuctionContracts);
       // String nftAuctionContracts = "0x9b38f6fa3943c24f4998d73d178fb1e2899a1365";
        int chainId = blockData.getChainId();
        List<Transaction> txList = buildTransactionList(blockData, chainId);
        log.info("nftAuctionScan.txList.size:{}", txList.size());
        // 事件监听 解析;
        for (Transaction transaction : txList) {
            String transactionHash = transaction.getTransactionHash();
            /*if(transactionHash.equals("0x6fda84fabede0cace5bd2cb4fccce175a211f143f48ad834fd3d29918490001e")){
                System.out.println(2);
            }*/
            String toAddr = transaction.getToAddr();
            if(nftAuctionContracts != null && !nftAuctionContracts.equalsIgnoreCase(toAddr)){
                continue;
            }
            //根据txHash查询交易明细（收据）
            vm30Utils.replayTxJudge(blockData, transactionHash, web3j);
            TransactionReceipt receipt = blockData.getTxList().get(transactionHash);
            this.parseTxStatus(transaction, receipt);

            String transactionStatus = transaction.getStatus();
            log.info("nftAuctionScan.transaction.toAddr:{}, status:{}, transactionHash:{}", toAddr, transactionStatus, transactionHash);
            if (StringUtils.isBlank(transactionStatus)) {
                continue;
            }
            int txSatte = Integer.parseInt(transactionStatus.replace("0x", ""), 16);
            log.info("nftAuctionScan.txSatte:{}", txSatte);

            if (StringUtils.isNotBlank(toAddr) && txSatte == 1) {
           // if (StringUtils.isNotBlank(toAddr) ) {
                String input = transaction.getInput();
                log.info("nftAuctionScan.开始处理拍卖，txHash:{}，toAddr:{}", transactionHash, toAddr);
                if (StringUtils.isNotBlank(input) && input.length() > 10) {
                    List<String> params = DecodUtils.getParams2List(input);
                    String       method = params.get(0);
                    log.info("nftAuctionScan.transaction.method:{}, params:{}", method, params);
                    //监控NFT 拍卖创建交易
                    ErcTypeNftEnum.Method nftMethod = ErcTypeNftEnum.getMethod(method);
                    log.info("nftAuctionScan.nftMethod:{}", nftMethod);
                    if(nftMethod == null){
                        continue;
                    }
                    ErcNftService ercNftService = ErcNftFactory.getInstance(nftMethod.isErc1155() ? ErcTypeNftEnum.ERC1155 : ErcTypeNftEnum.DEFAULT);
                    if(!ercNftService.isScan()){
                        log.info("监控NFT 拍卖hash:{}被过滤！", transactionHash);
                        continue;
                    }
                    if (params.size() > 2 && nftMethod.isCreate()) {
                        log.info("监控NFT 拍卖创建交易hash:{}", transactionHash);
                        createNewNftAuction(transaction, params, ercNftService, 0);
                        //监控NFT 拍卖成交交易
                    } else if (params.size() > 2 && nftMethod.isFinish()) {
                        log.info("监控NFT 拍卖成交交易hash:{}", transactionHash);
                        finishNftAuction(transaction, params, ercNftService);
                        //监控NFT 拍卖取消交易
                    } else if (params.size() > 2 && nftMethod.isCancel()) {
                        log.info("监控NFT 拍卖取消交易hash:{}", transactionHash);
                        cancelNftAuction(transaction, params, ercNftService);
                    }
                }
                log.info("nftAuctionScan.开始处理拍卖处理已完成，txHash:{}，toAddr:{}", transactionHash, toAddr);
             //}
            }
        }
    }

    //监控NFT 拍卖创建交易
    private void createNewNftAuction(Transaction transaction, List<String> params, ErcNftService ercNftService, int retryCount) {
        try {
            String transactionHash = transaction.getTransactionHash();
            log.info("createNewNftAuction.transactionHash:{}, params:{}", transactionHash, params);
            String fromAddr = transaction.getFromAddr();
            String nftContractAddress = "0x" + params.get(1).substring(params.get(1).length() - 40);
            //根据txHash查询拍卖表是否存在，存在则更新
            NftAuction nftAuction = ercNftService.getByTxHash(transactionHash);
            if (nftAuction == null) {
                nftAuction = new NftAuction();
                nftAuction.setTxHash(transactionHash);
            }
            nftAuction.setTxStatus(true);

            TokenInfo nftTokenContract = tokenInfoDao.findAllByAddress(nftContractAddress);
            if (Objects.isNull(nftTokenContract)) {
                nftTokenContract = addTokenInfo(nftContractAddress, fromAddr, ercNftService.type() == ErcTypeNftEnum.ERC1155);
            }
            Long nftId = ercNftService.getNftId(params);
            if (nftAuction.getTokenId() == null || nftAuction.getTokenId() == 0) {
                nftAuction.setTokenId(nftTokenContract.getId());
            }
            if (StringUtils.isBlank(nftAuction.getNftContractAddress())) {
                nftAuction.setNftContractAddress(nftContractAddress);
            }
            //获取NFT的data信息;
            NftAccount nftAccount;
            try {
                AccountInfo fromAccountInfo = accountInfoDao.findByAddress(fromAddr);
                nftAccount = nftAccountDao.selectByTokenIdContract(nftId, nftTokenContract.getId(), fromAccountInfo.getId());
                //nftAccount = nftAccountDao.findByTxHash(transactionHash);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            if (nftAccount != null) {
                if (StringUtils.isBlank(nftAuction.getNftData())) {
                    nftAuction.setNftData(nftAccount.getNftData());
                }
                if (nftAuction.getNftAccountId() == null) {
                    nftAuction.setNftAccountId(nftAccount.getAccountId());
                }
            }
            //更新并删除nft_account
        /*boolean result = updateNftAccount(fromAddr, nftContractAddress);
        //获取nftData（20、721使用）
        String nftData = ercNftService.getNftData2(web3j, nftContractAddress, new BigInteger(nftId.toString()));
        log.info("createNewNftAuction.tokenURL.nftData:{}", nftData);*/

            CreateNewNftAuctionBean createBean = new CreateNewNftAuctionBean();
            createBean.setFromAddr(fromAddr);
            createBean.setNftContractAddress(nftContractAddress);
            createBean.setNftId(nftId);
            createBean.setParams(params);
            createBean.setWeb3j(web3j);
            createBean.setTransaction(transaction);
            createBean.setErcNftService(ercNftService);
            //组装拍卖数据
            ercNftService.createNewNftAuction(createBean, nftAuction);
            log.info("createNewNftAuction.transactionHash:{}.nftAuction:{}", transactionHash, nftAuction);
            //更新nft_account
            Long amount = ercNftService.getAmount(web3j, nftContractAddress, fromAddr, nftId);
            //注意点：nftAccount如果没有前置步骤创建nft，则这里获取为空，会报空指针异常
            ercNftService.updateNftAccountAmount(nftAccount, amount);
            log.info("createNewNftAuction.transactionHash:{} end", transactionHash);
        } catch (Exception e){
            retryCount = retryCount + 1;
            e.printStackTrace();
            if(retryCount < 3){
                createNewNftAuction(transaction, params, ercNftService, retryCount);
            }
        }
    }

    //监控NFT 拍卖取消交易(721走db-log)
    private void cancelNftAuction(Transaction transaction, List<String> params, ErcNftService ercNftService) {
        log.info("cancelNftAuction.transactionHash:{}", transaction.getTransactionHash());

        String status = transaction.getStatus();
        log.info("cancelNftAuction.status:{}", status);
        if(status.equals("0x1")){
            //拍卖取消逻辑
            ercNftService.cancelNftAuction(transaction, params, web3j);
            log.info("ercNftService.cancelNftAuction.end!!!");
        }
    }

    //监控NFT 拍卖成交(走db-log)
    private void finishNftAuction(Transaction transaction, List<String> params, ErcNftService ercNftService) {
        log.info("finishNftAuction.transactionHash:{}", transaction.getTransactionHash());
        //拍卖成交逻辑
        ercNftService.finishNftAuction(transaction, params, web3j);
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
                this.parseTxStatus(tx, receipt);

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

    public void parseTxStatus(Transaction tx, TransactionReceipt receipt) {
        if (receipt != null && receipt.getStatus() != null &&
                (receipt.getStatus().equalsIgnoreCase("1") || receipt.getStatus().equalsIgnoreCase("0x1"))) {
            tx.setStatus("0x1");
        } else {
            tx.setStatus("0x0");
        }
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
    public TokenInfo addTokenInfo(String toAddr, String fromAddr, boolean erc1155) {
        TokenInfo tokenQuery = new TokenInfo();
        try {
            String symbol = StringUtils.EMPTY;
            String name = StringUtils.EMPTY;
            BigInteger decimals = BigInteger.ZERO;
            if(!erc1155) {
                symbol = vm30Utils.symbol(web3j, toAddr).toString();
                if (StringUtils.isBlank(symbol)) {
                    return tokenQuery;
                }
                name = vm30Utils.name(web3j, toAddr).toString();
                if (StringUtils.isBlank(name)) {
                    return tokenQuery;
                }
                decimals = vm30Utils.decimals(web3j, toAddr);
            }
            if (StringUtils.isNotBlank(symbol) && StringUtils.isNotBlank(name) || erc1155) {
                if(!erc1155) {
                    //判断合约类型
                    checkTokenType(toAddr, fromAddr, tokenQuery, decimals);
                }else{
                    tokenQuery.setTokenType(3);
                }
                tokenQuery.setTokenName(name);
                tokenQuery.setTokenSymbol(symbol);
                tokenQuery.setDecimals(decimals);
                tokenQuery.setAddress(toAddr);
                tokenQuery.setCreateTime(new Date());
                tokenQuery.setCreateTime(new Date());
                tokenInfoDao.save(tokenQuery);
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    @Override
    public AccountInfo getAccountNotExistSave(String fromAddr) {
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
        return accountInfo;
    }

    @Override
    @Transactional
    public boolean updateNftAccount(String fromAddr, String contract) {
        AccountInfo accountInfo = getAccountNotExistSave(fromAddr);
        TokenInfo tokenInfo = tokenInfoDao.findAllByAddress(contract);
        if( tokenInfo == null ){
            return false;
        }
        NftAccount nftAccountQuer = new NftAccount();
        nftAccountQuer.setAccountId(accountInfo.getId());
        nftAccountQuer.setTokenId(tokenInfo.getId());
        nftAccountDao.deleteNftAccount(accountInfo.getId(), tokenInfo.getId()); //清楚账户的NFT旧信息;
        //批量获取nft的数据信息;
        try {
            BigInteger            count = vm30Utils.balanceOf(web3j, contract, fromAddr); //有几个nft
            ArrayList<NftAccount> nfts = new ArrayList<>();
            for (int i = 0; i < count.intValue(); i++) {
                NftAccount nftAccount = new NftAccount();
                try {
                    nftAccount.setContractName(tokenInfo.getTokenName());
                    nftAccount.setAccountId(accountInfo.getId());
                    nftAccount.setTokenId(tokenInfo.getId());
                    //tokenOfOwnerByIndex
                    BigInteger tokenId = vm30Utils.tokenOfOwnerByIndex(web3j, contract, fromAddr, i);
                    log.info("updateNftAccount.accountInfo.getId():{}, tokens.getId():{}, tokenId:{}, fromAddr: {}", accountInfo.getId(), tokenInfo.getId(), tokenId, fromAddr);
                    if (tokenId.intValue() == -1 || (tokenId.intValue() == 0 && i > 0)) {
                        tokenInfo.setTokenType(1);
                        tokenInfoDao.updateTokenType(tokenInfo.getId(), 1);
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
                    nftAccount.setAmount(1L);
                    nftAccount.setWatcherUpdated(1);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("updateNftAccount.", e);
                }
                nfts.add(nftAccount);
            }
            nftAccountDao.saveAll(nfts);
        } catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    @Override
    @TargetDataSource(value = DataSourceEnum.marketplace)
    public void save(NftAuction nftAuction){
        nftAuctionDao.save(nftAuction);
    }

}

