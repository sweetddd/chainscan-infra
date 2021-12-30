package ai.everylink.chainscan.watcher.plugin.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 区块交易数据记录对象 wallet_incentive_block_transaction
 *
 * @author brett
 * @date 2021-10-09
 */
@Data
public class IncentiveTransaction {

    private static final long serialVersionUID = 1L;

    /**
     * 交易对
     */
    private String coinSymbol;

    /**
     * 交易类型
     */
    private String transactionType;

    /**
     * 买方地址
     */
    private String buyerAddress;

    /**
     * 卖方地址
     */
    private String sellerAddress;

    /**
     * 成交数量
     */
    private BigDecimal amount;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 买方手续费
     */
    private BigDecimal buyerFee;

    /**
     * 卖方手续费
     */
    private BigDecimal sellerFee;

    /**
     * 交易hash
     */
    private String transactionHash;

}
