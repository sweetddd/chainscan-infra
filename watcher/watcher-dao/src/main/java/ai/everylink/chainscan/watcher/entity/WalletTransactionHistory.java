package ai.everylink.chainscan.watcher.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
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
@DynamicInsert
@DynamicUpdate
public class WalletTransactionHistory implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "type", nullable = false, length = 80)
    private String type;

    @JSONField(name = "token_type")
    @Column(name = "token_type", nullable = false, length = 80)
    private String tokenType;

    @JSONField(name = "symbol")
    @Column(name = "symbol", nullable = false, length = 80)
    private String symbol;

    @JSONField(name = "action_symbol")
    @Column(name = "action_symbol", nullable = false, length = 80)
    private String actionSymbol;

    @JSONField(name = "token_address")
    @Column(name = "token_address", nullable = false)
    private String tokenAddress;

    @JSONField(name = "amount")
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @JSONField(name = "contract_name")
    @Column(name = "contract_name", nullable = false)
    private String contractName;

    @JSONField(name = "token_id")
    @Column(name = "token_id", nullable = false, length = 80)
    private String tokenId;

    @Column(name = "layer", nullable = false, length = 80)
    private String layer;

    @JSONField(name = "from_address")
    @Column(name = "from_address", nullable = false, length = 80)
    private String fromAddress;

    @JSONField(name = "from_network")
    @Column(name = "from_network", length = 80)
    private String fromNetwork;

    @JSONField(name = "from_chain_id")
    @Column(name = "from_chain_id")
    private Integer fromChainId;

    @JSONField(name = "from_deposit_nonce")
    @Column(name = "from_deposit_nonce")
    private Integer fromDepositNonce;

    @JSONField(name = "from_tx_hash")
    @Column(name = "from_tx_hash")
    private String fromTxHash;

    @JSONField(name = "from_tx_state")
    @Column(name = "from_tx_state")
    private Integer fromTxState;

    @JSONField(name = "date")
    @JsonProperty("date")
    @Column(name = "from_tx_time")
    private Timestamp fromTxTime;

    @JSONField(name = "to_address")
    @Column(name = "to_address", nullable = false, length = 80)
    private String toAddress;

    @JSONField(name = "to_network")
    @Column(name = "to_network", length = 80)
    private String toNetwork;

    @JSONField(name = "to_chain_id")
    @Column(name = "to_chain_id")
    private Integer toChainId;

    @JSONField(name = "to_deposit_nonce")
    @Column(name = "to_deposit_nonce")
    private Integer toDepositNonce;

    @JSONField(name = "to_tx_hash")
    @Column(name = "to_tx_hash")
    private String toTxHash;


    @JSONField(name = "to_tx_state")
    @Column(name = "to_tx_state")
    private Integer toTxState;

    @JSONField(name = "to_tx_time")
    @Column(name = "to_tx_time")
    private Timestamp toTxTime;

    @JSONField(name = "confirm_block")
    @Column(name = "confirm_block")
    private BigInteger confirmBlock;

    @JSONField(name = "submit_block")
    @Column(name = "submit_block")
    private BigInteger submitBlock;

    @JSONField(name = "status")
    @JsonProperty("status")
    @Column(name = "tx_state", nullable = false)
    private String txState;

    @JSONField(name = "create_time")
    @JsonProperty("create_time")
    @Column(name = "create_time")
    private Timestamp createTime;

    @JSONField(name = "update_time")
    @JsonProperty("update_time")
    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "l2_executed")
    private Integer l2Executed;
}
