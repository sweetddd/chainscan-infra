package ai.everylink.chainscan.watcher.plugin.constant;

/**
 * @author by watson
 * @Description the coin token enum
 * @Date 2020/12/3 14:59
 */
public enum CoinChainEnum {
    ETHEREUM("Ethereum"),
    BIT("Bticoin"),
    LITECOIN("Litecoin"),
    EOS("EOS");


    private String chain;

    CoinChainEnum(String chain) {
        this.chain = chain;
    }
}
