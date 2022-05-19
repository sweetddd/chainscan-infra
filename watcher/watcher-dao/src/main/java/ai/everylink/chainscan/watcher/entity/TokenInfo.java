package ai.everylink.chainscan.watcher.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigInteger;
import java.util.Date;

@Table(name = "token_info")
@Entity
@Data
public class TokenInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "address", nullable = false, length = 32)
    private String address;

    @Column(name = "token_type", nullable = false)
    private Integer tokenType;

    @Column(name = "decimals", nullable = false)
    private BigInteger decimals;

    @Column(name = "token_symbol", length = 32)
    private String tokenSymbol;

    @Column(name = "token_name", length = 100)
    private String tokenName;

    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @Column(name = "create_account_id", nullable = false)
    private Long createAccountId;

}
