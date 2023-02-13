package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.entity.*;
import ai.everylink.chainscan.watcher.plugin.bean.CreateNewNftAuctionBean;
import ai.everylink.chainscan.watcher.plugin.constant.NFTAuctionConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ErcNft1155ServiceImpl extends ErcNftBaseService {

    @Override
    public ErcTypeNftEnum type() {
        return ErcTypeNftEnum.ERC1155;
    }

    @Override
    public boolean isScan() {
        String erc1155NotScanStr = environment.getProperty("watcher.token.not.scan.erc1155");
        boolean erc1155NotScan = Boolean.parseBoolean(erc1155NotScanStr);
        log.info("erc1155NotScanStr:{}, erc1155NotScan:{}", erc1155NotScanStr, erc1155NotScan);
        return !erc1155NotScan;
    }

    @Override
    public String getNftData(Web3j web3j, String contract, BigInteger tokenId) {
        log.info("ErcToken1155ServiceImpl.contract:{}，tokenId:{}", contract, tokenId);
        String tokenURI = vm30Utils.URI(web3j, contract, tokenId);
        log.info("ErcToken1155ServiceImpl.getNftData:{}", tokenURI);
        return tokenURI;
    }

    @Override
    public Long getNftId(List<String> params){
        return Long.parseLong(params.get(2), 16);
    }

    /**
     * event NftAuctionCreated(
     *          method, 0
     *         address nftContractAddress, 1
     *         uint256 tokenId,  ->下标=2
     *         uint256 auctionId, // 拍卖ID 3（3-4）
     *         uint256 amount, // 销售的数量 4（3-4）
     *         bool isRetail, // 是否零售 5
     *         address nftSeller, 6
     *         address erc20Token, // 支付的币的合约地址。7
     *         uint128 minPrice, 8
     *         uint128 buyNowPrice, // 是零售，则是单张票的价格；否则是整体打包价格。 9
     *         uint32 auctionBidPeriod, 10
     *         uint32 bidIncreasePercentage, 11
     *         address[] feeRecipients, 12
     *         uint32[] feePercentages 13
     *     );
     */
    @Override
    public void createNewNftAuction(CreateNewNftAuctionBean createBean, NftAuction nftAuction) {
        String fromAddr = createBean.getFromAddr();
        List<String> params = createBean.getParams();
        Transaction transaction = createBean.getTransaction();
        Web3j web3j = createBean.getWeb3j();
        String nftContractAddress = createBean.getNftContractAddress();
        if(nftAuction.getNftId() == null) {
            Long tokenId = this.getNftId(params);
            nftAuction.setNftId(tokenId);
        }
        if(nftAuction.getNftAuctionId() == null) {
            nftAuction.setNftAuctionId(new BigInteger(params.get(4), 16).toString());
        }
        if(StringUtils.isBlank(nftAuction.getPayToken())) {
            String erc20Token = "0x" + params.get(7).substring(params.get(7).length() - 40);
            nftAuction.setPayToken(erc20Token);
        }
        if(StringUtils.isBlank(nftAuction.getMinPrice())) {
            BigInteger minPrice = new BigInteger(params.get(8), 16);
            nftAuction.setMinPrice(minPrice.toString());
        }
        if(StringUtils.isBlank(nftAuction.getBuyNowPrice())) {
            BigInteger buyNowPrice = new BigInteger(params.get(9), 16);
            nftAuction.setBuyNowPrice(buyNowPrice.toString());
        }
        if(nftAuction.getAuctionBidPeriod() == null) {
            Long auctionBidPeriod = Long.parseLong(params.get(10), 16);
            nftAuction.setAuctionBidPeriod(auctionBidPeriod);
            if(nftAuction.getAuctionEnd() == null) {
                long time = transaction.getTxTimestamp().getTime() / 1000;
                nftAuction.setAuctionEnd(time + auctionBidPeriod);
            }
        }

        if(nftAuction.getBidIncreasePercentage() == null) {
            Long bidIncreasePercentage = Long.parseLong(params.get(11), 16);
            nftAuction.setBidIncreasePercentage(bidIncreasePercentage);
        }
        /*Long feePercentages = Long.parseLong(params.get(13), 16);
        nftAuction.setFeePercentages(feePercentages);
        String feeRecipients = "0x" + params.get(12);
        nftAuction.setFeeRecipients(feeRecipients);*/
        if(nftAuction.getState() == null) {
            nftAuction.setState(NFTAuctionConstant.STATE_CREAT);
        }
        if(nftAuction.getCreateTime() == null) {
            nftAuction.setCreateTime(new Date().toInstant());
        }
        if(StringUtils.isBlank(nftAuction.getNftContractAddress())) {
            nftAuction.setAccountAddress(transaction.getFromAddr());
        }
        if(nftAuction.getDeleted() == null) {
            nftAuction.setDeleted(false);
        }
        if(StringUtils.isBlank(nftAuction.getLayer())) {
            nftAuction.setLayer("L1");
        }
        if(nftAuction.getChainId() == null) {
            String chainId = environment.getProperty("watcher.chain.chainId");
            if (chainId != null) {
                nftAuction.setChainId(Long.parseLong(chainId));
            }
        }
        nftAuctionService.save(nftAuction);
    }

    @Override
    public void finishNftAuction(Transaction transaction, List<String> params, Web3j web3j) {
        //合约地址+nft的tokenId+事件id(auctionId)确定唯一性
        String nftContractAddress = "0x" + params.get(1).substring(params.get(1).length()-40);
        Long nftId = Long.parseLong(params.get(2), 16);
        //Long auctionId = Long.parseLong(params.get(3), 16); //拍卖id（链上）
        //nftAuctionDao.finish1155(nftContractAddress, tokenId, auctionId);
        //根据account_id + nft_id + 合约地址（token_id）查询唯一
        String fromAddr = transaction.getFromAddr();
        //AccountInfo fromAccountInfo = nftAuctionService.getAccountNotExistSave(fromAddr);
        AccountInfo fromAccountInfo = accountInfoDao.findByAddress(fromAddr);
        if(fromAccountInfo == null){
            log.error("数据错误！finishNftAuction.出价未查询到address:[{}]的数据！", fromAddr);
            return;
        }
        TokenInfo nftTokenContract = tokenInfoDao.findAllByAddress(nftContractAddress);
        log.info("finishNftAuction.fromAddr:{}, fromAccountInfo:{}:, nftTokenContract:{}", fromAddr, fromAccountInfo, nftTokenContract);
        if (Objects.isNull(nftTokenContract)) {
            nftTokenContract = nftAuctionService.addTokenInfo(nftContractAddress, fromAddr, true);
        }
        //更新资产
        Long amount = this.getAmount(web3j, nftContractAddress, fromAddr, nftId);
        //更新nft_account amount、状态
        int count = nftAccountDao.updateAmountAndWatcherStatus(nftId, nftTokenContract.getId(), fromAccountInfo.getId(), 1, amount);
        log.info("finishNftAuction.updateNftAccount transactionHash:{}, count:{}", transaction.getTransactionHash(), count);
    }

    @Override
    public void cancelNftAuction(Transaction transaction, List<String> params, Web3j web3j) {
        //合约地址+nft的tokenId+事件id(auctionId)确定唯一性
        String nftContractAddress = "0x" + params.get(1).substring(params.get(1).length() - 40);
        Long nftId = Long.parseLong(params.get(2), 16);
        //Long auctionId = Long.parseLong(params.get(3), 16);
        //int count = nftAuctionDao.cancel1155(nftContractAddress, tokenId, auctionId);
        //boolean result = nftAuctionService.updateNftAccount(transaction.getFromAddr(), nftContractAddress, true);
        //更新资产，为0的删除
        String fromAddr = transaction.getFromAddr();
        //AccountInfo fromAccountInfo = nftAuctionService.getAccountNotExistSave(fromAddr);
        AccountInfo fromAccountInfo = accountInfoDao.findByAddress(fromAddr);
        if(fromAccountInfo == null){
            log.error("数据错误！cancelNftAuction.出价未查询到address:[{}]的数据！", fromAddr);
            return;
        }
        TokenInfo nftTokenContract = tokenInfoDao.findAllByAddress(nftContractAddress);
        log.info("finishNftAuction.fromAddr:{}, fromAccountInfo:{}:, nftTokenContract:{}", fromAddr, fromAccountInfo, nftTokenContract);
        if (Objects.isNull(nftTokenContract)) {
            nftTokenContract = nftAuctionService.addTokenInfo(nftContractAddress, fromAddr, true);
        }
        //更新资产
        synchronized (this) {
            Long amount = this.getAmount(web3j, nftContractAddress, fromAddr, nftId);
            //更新nft_account amount、状态
            int count = nftAccountDao.updateAmountAndWatcherStatus(nftId, nftTokenContract.getId(), fromAccountInfo.getId(), 1, amount);
            log.info("finishNftAuction.cancelNftAuction end transactionHash:{}, count:{}", transaction.getTransactionHash(), count);
        }
    }

    @Override
    public Long getAmount(Web3j web3j, String contractAddress, String address, Long tokenId) {
        return vm30Utils.balanceOfErc1155(web3j, contractAddress, address, tokenId, 0).longValue();
    }

    @Override
    public int updateNftAccountAmount(NftAccount nftAccount, Long amount) {
        return nftAccountDao.updateAmount(nftAccount.getId(), amount);
    }

}
