package ai.everylink.chainscan.watcher.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;


@Data
@Entity
@Table(name="batch")
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "batch_number")
    private Long batch_number;

    //1 pending,2 submitted, 3 finalized
    @Column(name = "status")
    private Status status;

    @Column(name = "time")
    private Date time;

    @Column(name = "transactions")
    private Long transactions;

    @Column(name = "blocks")
    private Long blocks;

    @Column(name = "start_block_number")
    private Long start_block_number;

    @Column(name = "end_block_number")
    private Long end_block_number;

    @Column(name = "commit_tx_hash")
    private String commit_tx_hash;

    @Column(name = "commit_time")
    private Date commit_time;

    @Column(name = "finalized_tx_hash")
    private String finalized_tx_hash;

    @Column(name = "finalized_time")
    private Date finalized_time;
}
