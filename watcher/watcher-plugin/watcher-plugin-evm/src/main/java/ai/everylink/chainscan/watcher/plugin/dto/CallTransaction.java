package ai.everylink.chainscan.watcher.plugin.dto;

import org.web3j.utils.Numeric;

import java.math.BigInteger;

public class CallTransaction {

    private String from;
    private String to;
    private BigInteger gas;
    private BigInteger gasPrice;
    private BigInteger value;
    private String data;


    public CallTransaction(
            String from,
            BigInteger gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data) {
        this.from = from;
        this.to = to;
        this.gas = gasLimit;
        this.gasPrice = gasPrice;
        this.value = value;

        if (data != null) {
            this.data = Numeric.prependHexPrefix(data);
        }

    }


    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getGas() {
        return convert(gas);
    }

    public String getGasPrice() {
        return convert(gasPrice);
    }

    public String getValue() {
        return convert(value);
    }

    public String getData() {
        return data;
    }


    private static String convert(BigInteger value) {
        if (value != null) {
            return Numeric.encodeQuantity(value);
        } else {
            return null; // we don't want the field to be encoded if not present
        }
    }
}
