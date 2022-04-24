package ai.everylink.chainscan.watcher.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Table(name = "token_account_balance")
@Entity
@Data
public class TokenAccountBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "account_id", nullable = false, length = 32)
    private Long accountId;

    @Column(name = "token_id", length = 32)
    private Long tokenId;

    @Column(name = "balance", nullable = false)
    private String balance;

    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @Column(name = "update_time", nullable = false)
    private Date updateTime;
}
