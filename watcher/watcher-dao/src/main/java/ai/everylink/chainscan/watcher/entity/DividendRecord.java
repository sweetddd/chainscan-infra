package ai.everylink.chainscan.watcher.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * 挖矿详情数据记录对象 wallet_mining_details
 *  dividend_record
 * @author brett
 * @date 2021-10-15
 */
@Data
@Entity
@Table(name="dividend_record")
public class DividendRecord {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mining_details")
    private String miningDetails;

    @Column(name = "earnings")
    private BigDecimal earnings;

    @Column(name = "volume")
    private BigDecimal volume;

    @Column(name = "transactions")
    private BigInteger transactions;

    @Column(name = "mining_earnings")
    private BigDecimal miningEarnings;

    @Column(name = "rigid_price")
    private BigDecimal rigidPrice;

    @Column(name = "time")
    private Date time;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @Column(name = "remark")
    private String remark;

}
