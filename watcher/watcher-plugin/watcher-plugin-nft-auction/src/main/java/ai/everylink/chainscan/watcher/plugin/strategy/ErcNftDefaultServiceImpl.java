package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.dao.TransactionLogDao;
import ai.everylink.chainscan.watcher.entity.NftAccount;
import ai.everylink.chainscan.watcher.entity.NftAuction;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.entity.TransactionLog;
import ai.everylink.chainscan.watcher.plugin.bean.CreateNewNftAuctionBean;
import ai.everylink.chainscan.watcher.plugin.constant.NFTAuctionConstant;
import ai.everylink.chainscan.watcher.plugin.service.impl.NFTAuctionServiceImpl;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ErcNftDefaultServiceImpl extends ErcNftBaseService {

    @Autowired
    private TransactionLogDao transactionLogDao;

    @Override
    public ErcTypeNftEnum type() {
        return ErcTypeNftEnum.DEFAULT;
    }

    @Override
    public boolean isScan() {
        String erc20NotScanStr = environment.getProperty("watcher.token.not.scan.erc20");
        boolean erc20NotScan = Boolean.parseBoolean(erc20NotScanStr);
        log.info("erc20NotScanStr:{}, erc20NotScan:{}", erc20NotScanStr, erc20NotScan);
        return !erc20NotScan;
    }

    @Override
    public String getNftData(Web3j web3j, String contract, BigInteger tokenId) {
        Utf8String tokenURL = vm30Utils.tokenURL(web3j, contract, tokenId);
        log.info("ErcTokenDefaultServiceImpl.getNftData:{}", tokenURL);
        return tokenURL.toString();
    }

    @Override
    public Long getNftId(List<String> params){
        return Long.parseLong(params.get(2), 16);
    }

    @Override
    public void createNewNftAuction(CreateNewNftAuctionBean createBean, NftAuction nftAuction){
        String fromAddr = createBean.getFromAddr();
        String nftContractAddress = createBean.getNftContractAddress();
        Long nftId = createBean.getNftId();
        List<String> params = createBean.getParams();
        Transaction transaction = createBean.getTransaction();
        log.info("createNewNftAuction.createBean:{}", createBean);
        //更新并删除nft_account
        boolean result = nftAuctionService.updateNftAccount(fromAddr, nftContractAddress);
        //获取nftData（20、721使用）
        String nftData = this.getNftData(createBean.getWeb3j(), nftContractAddress, new BigInteger(nftId.toString()));
        log.info("createNewNftAuction.tokenURL.nftData:{}", nftData);
        nftAuction.setNftId(nftId);
        if(StringUtils.isNotBlank(nftData)){
            //拍卖需求增加字段;
            String[] split = nftData.split(",");
            log.info("createNewNftAuction.nftData:{}", nftData);
            if(split.length >1){
                try {
                    Base64.Decoder decoder = Base64.getDecoder();
                    String  decodeNftData = new String(decoder.decode(split[1]), StandardCharsets.UTF_8);
                    JSONObject data = JSON.parseObject(decodeNftData);
                    log.info("createNewNftAuction.data:{}", data);
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
        BigInteger minPrice = new BigInteger(params.get(4), 16);
        nftAuction.setMinPrice(minPrice.toString());
        BigInteger buyNowPrice = new BigInteger(params.get(5), 16);
        nftAuction.setBuyNowPrice(buyNowPrice.toString());
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
        nftAuction.setDeleted(false);
        nftAuction.setLayer("L1");
        String  chainId = environment.getProperty("watcher.chain.chainId");
        if(chainId != null){
            nftAuction.setChainId(Long.parseLong(chainId));
        }
        nftAuctionService.save(nftAuction);
    }

    @Override
    public void finishNftAuction(Transaction transaction, List<String> params, Web3j web3j) {
        List<TransactionLog> txLog = transactionLogDao.findByTxHash(transaction.getTransactionHash());
        log.info("finishNftAuction.txLog:{}, transaction:{}", txLog, transaction);
        for (TransactionLog transactionLog : txLog) {
            log.info("finishNftAuction.transactionLog:{}", transactionLog);
            String topicsStr = transactionLog.getTopics();
            //topics转为数组
            JSONArray topics = JSONArray.parseArray(topicsStr);
            log.info("finishNftAuction.transactionLog.topics():{}", topics);
            if (topics.size() > 0) {
                String topic = topics.get(0).toString();
                if (topic.equals(NFTAuctionServiceImpl.NFTAUCTION_FINISH_TOPIC)) {
                    String data = transactionLog.getData();
                    log.info("finishNftAuction.data:{}", data);
                    String substring = data.substring(2, 66);
                    String nftContractAddress ="0x" +  substring.substring(substring.length() - 40);
                    Long tokenId = Long.parseLong(data.substring(66, 130), 16) ;
                    nftAuctionDao.finish(nftContractAddress, tokenId);
                    log.info("finishNftAuction.updateNftAccount start");
                    boolean result = nftAuctionService.updateNftAccount(transaction.getFromAddr(), nftContractAddress);
                    log.info("finishNftAuction.updateNftAccount end");
                    //  System.out.println(result);
                }
            }
        }
    }

    @Override
    public void cancelNftAuction(Transaction transaction, List<String> params, Web3j web3j) {
        String transactionHash = transaction.getTransactionHash();
        List<TransactionLog> txLog = transactionLogDao.findByTxHash(transactionHash);
        log.info("cancelNftAuction.txLog:{}", txLog);
        for (TransactionLog transactionLog : txLog) {
            log.info("cancelNftAuction.transactionLog:{}", transactionLog);
            String topicsStr = transactionLog.getTopics();
            //topics转为数组
            JSONArray topics = JSONArray.parseArray(topicsStr);
            log.info("cancelNftAuction.transactionLog.topics:{}", topics);
            if (topics.size() > 0) {
                String topic = topics.get(0).toString();
                log.info("cancelNftAuction.topic:{}", topic);
                if (topic.equals(NFTAuctionServiceImpl.NFTAUCTION_CANCEL_TOPIC)) {
                    String nftContractAddress = transactionLog.getAddress();
                    Long tokenId = Long.parseLong(topics.get(3).toString().substring(2, 66), 16) ;
                    log.info("cancelNftAuction.cancel start. nftContractAddress:{}, tokenId:{}, topics:{}", nftContractAddress, tokenId, topics);
                    nftAuctionDao.cancel(nftContractAddress, tokenId);
                    log.info("cancelNftAuction.updateNftAccount start.transaction:{}", transaction);
                    boolean result = nftAuctionService.updateNftAccount(transaction.getFromAddr(), nftContractAddress);
                    log.info("erc721卖家测试NFT拍卖 nftContractAddress:" + nftContractAddress + "tokenId =" + tokenId);
                }
            }
        }
        //["0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef","0x0000000000000000000000004e1e1f8b4fdf2e452942026fa8cc36cc6a651a81","0x00000000000000000000000010b77a65becc87657f7497e99ffc6e25f50b1979","0x0000000000000000000000000000000000000000000000000000000000000001"]
    }

    @Override
    public Long getAmount(Web3j web3j, String contractAddress, String address, Long tokenId) {
        return 1L;
    }

    @Override
    public int updateNftAccountAmount(NftAccount nftAccount, Long amount) {
        return 1;
    }

}
