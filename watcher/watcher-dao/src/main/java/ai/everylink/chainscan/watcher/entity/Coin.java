package ai.everylink.chainscan.watcher.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "token")
@Entity
@Data
public class Coin {
    @Id
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_platform_coin")
    private Integer isPlatformCoin;

    @Column(name = "name_cn")
    private String nameCn;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "wallet_scale", nullable = false)
    private Integer walletScale;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "circulation_supply")
    private Long circulationSupply;

    @Column(name = "explorer")
    private String explorer;

    @Column(name = "introduction")
    private String introduction;

    @Column(name = "issue_date")
    private String issueDate;

    @Column(name = "issue_price", length = 19)
    private String issuePrice;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "max_supply")
    private Long maxSupply;

    @Column(name = "rank")
    private Long rank;

    @Column(name = "total_supply")
    private Long totalSupply;

    @Column(name = "online")
    private Integer online;

    @Column(name = "is_dividend_coin")
    private Integer isDividendCoin;

    @Column(name = "auto_audit")
    private Integer autoAudit;

    @Column(name = "show_scale")
    private Integer showScale;

    @Column(name = "show_scale_rounding_mode")
    private Integer showScaleRoundingMode;

    @Column(name = "icon_cache_id")
    private Long iconCacheId;

    @Column(name = "total_lock_amount")
    private String totalLockAmount;

    @Column(name = "total_burnt_amount")
    private String totalBurntAmount;

    @Column(name = "l2_lock_amount")
    private String l2LockAmount;

    @Column(name = "l1_lock_amount")
    private String l1LockAmount;


}
