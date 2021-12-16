package ai.everylink.chainscan.watcher.plugin.entity;

import lombok.Data;

import javax.persistence.*;

@Table(name = "coin_contract")
@Entity
@Data
public class CoinContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "chain_id")
    private Long chainId;

    @Column(name = "contract_address")
    private String contractAddress;

    @Column(name = "name")
    private String name;

    @Column(name = "contract_decimals")
    private Integer contractDecimals;

    @Column(name = "coin_type")
    private Integer coinType;

}
