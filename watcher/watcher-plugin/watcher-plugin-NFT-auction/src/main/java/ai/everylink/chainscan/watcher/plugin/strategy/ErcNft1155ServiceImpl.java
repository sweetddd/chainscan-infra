package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.entity.NftAuction;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.constant.NFTAuctionConstant;
import cn.hutool.core.util.StrUtil;
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
    public String getNftData2(Web3j web3j, String contract, BigInteger tokenId) {
        return StrUtil.EMPTY;
    }

    @Override
    public Integer getNftId(List<String> params){
        return Integer.parseInt(params.get(2), 16);
    }

    /**
     * event NftAuctionCreated(
     *          method, 0
     *         address nftContractAddress, 1
     *         uint256 tokenId,  ->下标=2
     *         uint256 auctionId, // 拍卖ID 3
     *         uint256 amount, // 销售的数量 4
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
    public void createNewNftAuction(String nftData, NftAuction nftAuction, List<String> params, Transaction transaction) {
        Integer tokenId = this.getNftId(params);
        nftAuction.setNftId(tokenId.longValue());
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
    }

    @Override
    public void cancelNftAuction(Transaction transaction, List<String> params) {
        Long tokenId = Long.parseLong(params.get(2), 16) ;
        String nftContractAddress = "0x" + params.get(1).substring(params.get(1).length()-40);
        nftAuctionDao.cancel(nftContractAddress, tokenId);
        boolean result = nftAuctionService.updateNftAccount(transaction.getFromAddr(), nftContractAddress, true);
        log.info("erc1155卖家测试NFT拍卖 nftContractAddress:" + nftContractAddress + "tokenId =" + tokenId);
    }

}
