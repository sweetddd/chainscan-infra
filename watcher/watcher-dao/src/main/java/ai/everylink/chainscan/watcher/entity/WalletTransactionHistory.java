package ai.everylink.chainscan.watcher.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigInteger;
import java.sql.Timestamp;

@Table(name = "wallet_transaction_history", indexes = {
        @Index(name = "tx_time", columnList = "from_tx_time"),
        @Index(name = "to_tx_hash", columnList = "to_tx_hash"),
        @Index(name = "from_tx_hash", columnList = "from_tx_hash"),
        @Index(name = "to_address", columnList = "to_address"),
        @Index(name = "from_address", columnList = "from_address")
})
@Data
@Entity
public class WalletTransactionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "type", nullable = false, length = 80)
    private String type;

    @Column(name = "token_type", nullable = false, length = 80)
    private String tokenType;

    @Column(name = "token_address", nullable = false)
    private String tokenAddress;

    @Column(name = "token_id", nullable = false, length = 80)
    private String tokenId;

    @Column(name = "from_address", nullable = false, length = 80)
    private String fromAddress;

    @Column(name = "from_network", length = 80)
    private String fromNetwork;

    @Column(name = "from_chain_id")
    private Integer fromChainId;

    @Column(name = "from_deposit_nonce")
    private Integer fromDepositNonce;

    @Column(name = "from_tx_hash")
    private String fromTxHash;

    @Column(name = "from_tx_state")
    private Integer fromTxState;

    @Column(name = "from_tx_time")
    private Timestamp fromTxTime;

    @Column(name = "to_address", nullable = false, length = 80)
    private String toAddress;

    @Column(name = "to_network", length = 80)
    private String toNetwork;

    @Column(name = "to_chain_id")
    private Integer toChainId;

    @Column(name = "to_deposit_nonce")
    private Integer toDepositNonce;

    @Column(name = "to_tx_hash")
    private String toTxHash;

    @Column(name = "to_tx_state")
    private Integer toTxState;

    @Column(name = "to_tx_time")
    private Timestamp toTxTime;

    @Column(name = "confirm_block")
    private BigInteger confirmBlock;

    @Column(name = "tx_state", nullable = false)
    private String txState;

}
