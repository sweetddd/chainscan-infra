package ai.everylink.chainscan.watcher.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Table(name = "nft_account")
@Entity
@Data
public class NftAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "account_id", nullable = false, length = 80)
    private Long accountId;

    @Column(name = "nft_id", nullable = false)
    private Long nftId;

    @Lob
    @Column(name = "nft_data")
    private String nftData;

    @Column(name = "nft_name", length = 80)
    private String nftName;

    @Column(name = "contract_name", nullable = false, length = 80)
    private String contractName;

    @Column(name = "token_id", nullable = false, length = 80)
    private Long tokenId;

    @Column(name = "create_time", nullable = false)
    private Instant createTime;

    @Column(name = "update_time", nullable = false)
    private Instant updateTime;
}
