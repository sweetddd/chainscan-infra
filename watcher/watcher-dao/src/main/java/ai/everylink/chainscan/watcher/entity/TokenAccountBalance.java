package ai.everylink.chainscan.watcher.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigInteger;
import java.util.Date;
import java.time.Instant;

@Table(name = "token_account_balance")
@Entity
@Data
public class TokenAccountBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "account", nullable = false, length = 32)
    private String account;

    @Column(name = "token", length = 32)
    private String token;

    @Column(name = "contract", length = 80)
    private String contract;

    @Column(name = "balance", nullable = false)
    private String balance;

    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @Column(name = "update_time", nullable = false)
    private Date updateTime;
}
