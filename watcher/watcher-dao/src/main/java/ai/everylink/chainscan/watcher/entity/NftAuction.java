package ai.everylink.chainscan.watcher.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.Instant;
import java.util.Date;

@Table(name = "nft_auction")
@Entity
@Data
public class NftAuction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "account_address", nullable = false, length = 80)
    private String accountAddress;

    @Column(name = "nft_contract_name", length = 80)
    private String nftContractName;

    @Column(name = "nft_name", length = 80)
    private String nftName;

    @Column(name = "nft_contract_address", length = 80)
    private String nftContractAddress;

    @Column(name = "nft_auction_id")
    private String nftAuctionId;

    @Column(name = "nft_id", nullable = false)
    private Long nftId;

    @Column(name = "token_id", nullable = false)
    private Long tokenId;

    @Lob
    @Column(name = "nft_data")
    private String nftData;

    @Column(name = "pay_token", nullable = false, length = 80)
    private String payToken;

    @Column(name = "min_price", nullable = false)
    private String minPrice;

    @Column(name = "buy_now_price", nullable = false)
    private String buyNowPrice;

    @Column(name = "auction_bid_period", nullable = false)
    private Long auctionBidPeriod;

    @Column(name = "auction_end", nullable = false)
    private Long auctionEnd;

    @Column(name = "bid_increase_percentage", nullable = false)
    private Long bidIncreasePercentage;

    @Column(name = "fee_recipients", length = 80)
    private String feeRecipients;

    @Column(name = "fee_percentages")
    private Long feePercentages;

    @Column(name = "nft_highest_bid")
    private Long nftHighestBid;

    @Column(name = "nft_highest_bidder", length = 80)
    private String nftHighestBidder;

    @Lob
    @Column(name = "nft_bid_history")
    private String nftBidHistory;

    //1拍卖中,2 成交, 3 撤销
    @Column(name = "state")
    private Integer state;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "create_time", nullable = false)
    private Instant createTime  = new Date().toInstant();

    @Column(name = "update_time", nullable = false)
    private Instant updateTime = new Date().toInstant();

    @Column(name = "nft_description", length = 256)
    private String nftDescription;

    @Column(name = "nft_external_link", length = 256)
    private String nftExternalLink;

    @Column(name = "nft_attributes", length = 256)
    private String nftAttributes;

    @Column(name = "nft_levels", length = 256)
    private String nftLevels;

    @Column(name = "nft_stats", length = 80)
    private String nftStats;

    @Column(name = "nft_unlockable_content", length = 256)
    private String nftUnlockableContent;

    @Column(name = "nft_explicit", length = 256)
    private Boolean nftExplicit;

    @Column(name = "chain_id")
    private Long chainId;

    @Column(name = "layer", length = 80)
    private String layer = "L1";

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "tx_status")
    private Boolean txStatus = true;

    @Column(name = "nft_account_id")
    private Long nftAccountId;

}
