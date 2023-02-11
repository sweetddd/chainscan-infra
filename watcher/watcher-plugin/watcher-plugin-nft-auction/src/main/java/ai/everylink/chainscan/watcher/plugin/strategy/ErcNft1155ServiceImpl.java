package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.entity.NftAccount;
import ai.everylink.chainscan.watcher.entity.NftAuction;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.bean.CreateNewNftAuctionBean;
import ai.everylink.chainscan.watcher.plugin.constant.NFTAuctionConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

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
        Long tokenId = this.getNftId(params);
        nftAuction.setNftId(tokenId);
        nftAuction.setNftAuctionId(new BigInteger(params.get(4), 16).toString());
        String erc20Token = "0x" + params.get(7).substring(params.get(7).length() - 40);
        nftAuction.setPayToken(erc20Token);
        BigInteger minPrice = new BigInteger(params.get(8), 16);
        nftAuction.setMinPrice(minPrice.toString());
        BigInteger buyNowPrice = new BigInteger(params.get(9), 16);
        nftAuction.setBuyNowPrice(buyNowPrice.toString());
        Long auctionBidPeriod = Long.parseLong(params.get(10), 16);
        nftAuction.setAuctionBidPeriod(auctionBidPeriod);
        long time = transaction.getTxTimestamp().getTime()/1000;
        nftAuction.setAuctionEnd(time + auctionBidPeriod);
        Long bidIncreasePercentage = Long.parseLong(params.get(11), 16);
        nftAuction.setBidIncreasePercentage(bidIncreasePercentage);
        Long feePercentages = Long.parseLong(params.get(13), 16);
        nftAuction.setFeePercentages(feePercentages);
        String feeRecipients = "0x" + params.get(12);
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
        nftAuctionDao.save(nftAuction);
    }

    @Override
    public void finishNftAuction(Transaction transaction, List<String> params) {
        //合约地址+nft的tokenId+事件id(auctionId)确定唯一性
        String nftContractAddress = "0x" + params.get(1).substring(params.get(1).length()-40);
        Long tokenId = Long.parseLong(params.get(2), 16) ;
        Long auctionId = Long.parseLong(params.get(3), 16) ;
        nftAuctionDao.finish1155(nftContractAddress, tokenId, auctionId);
        log.info("finishNftAuction.updateNftAccount transactionHash:{}", transaction.getTransactionHash());
        //boolean result = nftAuctionService.updateNftAccount(transaction.getFromAddr(), nftContractAddress, true);
    }

    @Override
    public void cancelNftAuction(Transaction transaction, List<String> params) {
        //合约地址+nft的tokenId+事件id(auctionId)确定唯一性
        String nftContractAddress = "0x" + params.get(1).substring(params.get(1).length()-40);
        Long tokenId = Long.parseLong(params.get(2), 16) ;
        Long auctionId = Long.parseLong(params.get(3), 16) ;
        int count = nftAuctionDao.cancel1155(nftContractAddress, tokenId, auctionId);
        //boolean result = nftAuctionService.updateNftAccount(transaction.getFromAddr(), nftContractAddress, true);
        log.info("finishNftAuction.cancelNftAuction end transactionHash:{}, count:{}", transaction.getTransactionHash(), count);
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
