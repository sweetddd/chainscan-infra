package ai.everylink.chainscan.watcher.plugin.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * 区块数据记录对象 wallet_incentive_block
 *
 * @author brett
 * @date 2021-10-09
 */
@Data
public class IncentiveBlock {

    private static final long serialVersionUID = 1L;

    /**
     * 区块高度
     */
    private Long blockHeight;

    /**
     * 难度
     */
    private BigInteger difficulty;

    /**
     * 手续费
     */
    private BigDecimal blockedFee;

    /**
     * 区块时间
     */
    private Long startTime;

    /**
     * hash
     */
    private String blockHash;

    /**
     * parentHash
     */
    private String parentHash;

    /**
     * 交易数量
     */
    private Long transactionCount;

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
