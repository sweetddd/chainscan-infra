package ai.everylink.chainscan.watcher.entity;


import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.math.BigInteger;
import java.util.Date;


@Data
@Entity
@Accessors(chain = true)
@Table(name = "plugin_processing")
public class PluginProcessing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "block_number")
    private BigInteger blockNumber;

    @Column(name = "deleted")
    private Integer deleted;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

}
