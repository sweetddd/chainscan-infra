package ai.everylink.chainscan.watcher.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

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

    @Column(name = "nft_contract_address", length = 80)
    private String nftContractAddress;

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
    private Long minPrice;

    @Column(name = "buy_now_price", nullable = false)
    private Long buyNowPrice;

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

    @Column(name = "state")
    private Integer state;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "create_time", nullable = false)
    private Instant createTime;

    @Column(name = "update_time", nullable = false)
    private Instant updateTime;

}