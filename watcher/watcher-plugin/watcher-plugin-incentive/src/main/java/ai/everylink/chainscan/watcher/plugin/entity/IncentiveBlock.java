package ai.everylink.chainscan.watcher.plugin.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 区块数据记录对象 wallet_incentive_block
 *
 * @author brett
 * @date 2021-10-09
 */
@Data
public class IncentiveBlock extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 当前记录起始索引
     */
    private Integer pageNum;

    /**
     * 每页显示记录数
     */
    private Integer pageSize;


    /**
     * id
     */
    private Long id;

    /**
     * 区块高度
     */
    private Long blockHeight;

    /**
     * 区块高度
     */
    private Long blockNumber;


    /**
     * 最高区块高度；
     */
    private Long max_number;

    /**
     * 难度
     */
    private Long difficulty;

    /**
     * 手续费
     */
    private BigDecimal blockedFee;

    /**
     * 区块时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startTime;

    /**
     * hash
     */
    private String blockHash;

    /**
     * parentHash
     */
    private String parentHash;

    /**
     * finalized
     */
    private boolean finalized;

    /**
     * 交易数量
     */
    private Integer transactionCount;

    /**
     * 本次分红
     */
    private BigDecimal reward;


    /**
     * 本次分红
     */
    private BigDecimal burnt;


    private List<IncentiveTransaction> extrinsics;
}
