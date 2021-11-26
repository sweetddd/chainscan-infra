
package ai.everylink.openapi.plugin.chainscan.util;

import io.zksync.domain.token.Token;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class Convert {
    private Convert() {
    }

    public static BigInteger toWei(BigDecimal number, Token token) {
        BigDecimal weiFactor = BigDecimal.TEN.pow(token.getDecimals());
        return number.multiply(weiFactor).toBigInteger();
    }
    public static BigInteger toWei(String number, Token token) {
        BigDecimal weiFactor = BigDecimal.TEN.pow(token.getDecimals());
        return new BigDecimal(number).multiply(weiFactor).toBigInteger();
    }

    public static BigDecimal fromWei(BigDecimal number, Token token) {
        BigDecimal weiFactor = BigDecimal.TEN.pow(token.getDecimals());
        return number.divide(weiFactor);
    }
    public static BigDecimal fromWei(String number, Token token) {
        BigDecimal weiFactor = BigDecimal.TEN.pow(token.getDecimals());
        return new BigDecimal(number).divide(weiFactor);
    }

    public static BigDecimal fromWei(String number, Unit unit) {
        return fromWei(new BigDecimal(number), unit);
    }

    public static BigDecimal fromWei(BigDecimal number, Unit unit) {
        return number.divide(unit.getWeiFactor());
    }

    public static BigDecimal toWei(String number, Unit unit) {
        return toWei(new BigDecimal(number), unit);
    }

    public static BigDecimal toWei(BigDecimal number, Unit unit) {
        return number.multiply(unit.getWeiFactor());
    }

    public static enum Unit {
        WEI("wei", 0),
        KWEI("kwei", 3),
        MWEI("mwei", 6),
        GWEI("gwei", 9),
        SZABO("szabo", 12),
        FINNEY("finney", 15),
        ETHER("ether", 18),
        KETHER("kether", 21),
        METHER("mether", 24),
        GETHER("gether", 27);

        private String     name;
        private BigDecimal weiFactor;

        private Unit(String name, int factor) {
            this.name = name;
            this.weiFactor = BigDecimal.TEN.pow(factor);
        }

        public BigDecimal getWeiFactor() {
            return this.weiFactor;
        }

        public String toString() {
            return this.name;
        }

        public static Unit fromString(String name) {
            if (name != null) {
                Unit[] var1 = values();
                int var2 = var1.length;

                for (int var3 = 0; var3 < var2; ++var3) {
                    Unit unit = var1[var3];
                    if (name.equalsIgnoreCase(unit.name)) {
                        return unit;
                    }
                }
            }

            return valueOf(name);
        }
    }
}
