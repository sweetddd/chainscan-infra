package ai.everylink.chainscan.watcher.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.Instant;
import java.util.Date;

@Data
@Entity
@Table(name = "pending_rewards")
public class PendingReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mobi_distribution_reserve", nullable = false)
    private Long mobiDistributionReserve;

    @Column(name = "staking_reserve", nullable = false)
    private Long stakingReserve;

    @Column(name = "buffer_rewards", nullable = false)
    private Long bufferRewards;

    @Column(name = "distribution_reserve_unit", nullable = false, length = 80)
    private String distributionReserveUnit;

    @Column(name = "staking_reserve_unit", nullable = false, length = 80)
    private String stakingReserveUnit;

    @Column(name = "buffer_rewards_unit", nullable = false, length = 80)
    private String bufferRewardsUnit;

    @Column(name = "create_time", nullable = false)
    private Date createTime;

}