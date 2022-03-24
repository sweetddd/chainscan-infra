package ai.everylink.chainscan.watcher.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Table(name = "bridge_history", indexes = {
        @Index(name = "src_tx_hash", columnList = "src_tx_hash"),
        @Index(name = "dst_tx_hash", columnList = "dst_tx_hash"),
        @Index(name = "user_address", columnList = "address")
})
@Entity
@Data
public class BridgeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "address", nullable = false, length = 80)
    private String address;

    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @Column(name = "token_address")
    private String tokenAddress;

    @Column(name = "token_symbol", length = 80)
    private String tokenSymbol;

    @Column(name = "bridge_fee")
    private BigDecimal bridgeFee;

    @Column(name = "src_network", nullable = false, length = 80)
    private String srcNetwork;

    @Column(name = "src_chain_id", nullable = false)
    private Integer srcChainId;

    @Column(name = "src_deposit_nonce", nullable = false)
    private Integer srcDepositNonce;

    @Column(name = "src_tx_hash", nullable = false)
    private String srcTxHash;

    @Column(name = "src_tx_state")
    private Integer srcTxState;

    @Column(name = "src_tx_time", nullable = false)
    private Date srcTxTime;

    @Column(name = "dst_network", length = 80)
    private String dstNetwork;

    @Column(name = "dst_chain_id")
    private Integer dstChainId;

    @Column(name = "dst_deposit_nonce")
    private Integer dstDepositNonce;

    @Column(name = "dst_tx_hash")
    private String dstTxHash;

    @Column(name = "dst_tx_state")
    private Integer dstTxState;

    @Column(name = "dst_tx_time", nullable = false)
    private Date dstTxTime;

    @Column(name = "bridge_state", nullable = false)
    private Integer bridgeState;
}
