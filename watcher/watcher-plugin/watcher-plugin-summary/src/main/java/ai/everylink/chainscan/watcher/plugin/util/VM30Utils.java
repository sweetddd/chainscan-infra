package ai.everylink.chainscan.watcher.plugin.util;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigDecimal;
import java.math.BigInteger;

@Slf4j
@Component
public class VM30Utils {

    private BigInteger gasLimit = BigInteger.valueOf(9000000);

    private Credentials credentials;

    @SneakyThrows
    public VM30 getContranct(Web3j web3j,String contractAddress) {
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice().multiply(new BigInteger("105")).divide(new BigInteger("100"));
        //调用合约
        credentials = Credentials.create("0x992ae9bb2319a1c67274f1f2dcdefdeb2e0e62533b591dfc2a49c58104d70a98");
        ContractGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);
        VM30 contract = VM30.load(contractAddress, web3j, credentials, gasProvider);
        return contract;
    }

    @SneakyThrows
    public BigInteger totalSupply(Web3j web3j,String contractAddress) {
        VM30       contract = getContranct(web3j,contractAddress);
        BigInteger totalSupply    = new BigInteger("0");
        try {
            totalSupply = contract.totalSupply().send();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("获取totalSupply失败:"+contractAddress);
        }
        return totalSupply;
    }

    @SneakyThrows
    public BigInteger totalLockAmount(Web3j web3j,String contractAddress) {
        VM30       contract = getContranct(web3j,contractAddress);
        BigInteger totalSupply    = new BigInteger("0");
        try {
            totalSupply = contract.totalLockAmount().send();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("获取totalLockAmount失败:"+contractAddress);
        }
        return totalSupply;
    }
    @SneakyThrows
    public BigInteger burnt(Web3j web3j,String contractAddress) {
        VM30       contract = getContranct(web3j,contractAddress);
        BigInteger totalSupply    = new BigInteger("0");
        try {
            totalSupply = contract.burnt().send();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("获取burnt失败:"+contractAddress);
        }
        return totalSupply;
    }
}
