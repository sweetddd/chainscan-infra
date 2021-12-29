package ai.everylink.chainscan.watcher.plugin.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.BigInteger;

@Component
@Slf4j
public class VM30Utils {

    @Value("${vm.secret.rpc-api:}")
    private String url;
    @Value("${vm.secret.rpc-secret:}")
    private String secret = "";
    @Value("${vm.secret.rpc-private-key:}")
    private String privateKey;

    private final static String contract_address = "";

    private Web3j web3j;
    private BigInteger gasLimit = BigInteger.valueOf(9000000);

    private Credentials credentials;

    private final static Integer decimals = 18;

    @PostConstruct
    public void init() {
        if (StringUtils.isNotEmpty(url)) {
            HttpService httpService = new HttpService(url);
            if (StringUtils.isNotEmpty(secret)) {
                String credential = okhttp3.Credentials.basic("", secret);
                httpService.addHeader("Authorization", credential);
            }
            this.web3j = Web3j.build(httpService);
            this.credentials = Credentials.create(privateKey);
        }
    }

    @SneakyThrows
    public Web3j web3j() {
        return this.web3j;
    }

    @SneakyThrows
    public BigDecimal burnt(String symbol, String network) {
        VM30 contract = getContranct(contract_address);
        BigInteger burnt    = new BigInteger("0");
        try {
            burnt = contract.burnt().send();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(symbol + "获取burnt失败:"+ contract_address);
        }
        BigDecimal weiFactor = BigDecimal.TEN.pow(decimals);
        return new BigDecimal(burnt).divide(weiFactor);
    }

    @SneakyThrows
    public BigDecimal price(String symbol, String network) {
        VM30 contract = getContranct(contract_address);
        BigInteger price = new BigInteger("0");
        try {
            price = contract.price().send();
        } catch (Exception ex) {
            log.error(symbol + "获取合约价格失败:" + contract_address);
        }
        BigDecimal weiFactor = BigDecimal.TEN.pow(decimals);
        return new BigDecimal(price).divide(weiFactor);
    }

    @SneakyThrows
    public VM30 getContranct(String contractAddress) {
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice().multiply(new BigInteger("105")).divide(new BigInteger("100"));
        //调用合约
        ContractGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);
        VM30 contract = VM30.load(contractAddress, web3j, credentials, gasProvider);
        return contract;
    }

}
