package ai.everylink.chainscan.watcher.core.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @Author apple
 * @Description
 * @Date 2021/12/28 10:19 上午
 **/
public class DecimalUtil {

    public static String toDecimal(int decimal, BigInteger integer) {
        StringBuffer sbf = new StringBuffer("1");
        for (int i = 0; i < decimal; i++) {
            sbf.append("0");
        }
        String balance = new BigDecimal(integer).divide(new BigDecimal(sbf.toString()), 18, BigDecimal.ROUND_DOWN).toPlainString();
        return balance;
    }

}
