package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.core.util.VmChainUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.List;

@Slf4j
@Service
public class ErcTokenDefaultServiceImpl extends ErcTokenBaseService {

    @Override
    public ErcTypeTokenEnum type() {
        return ErcTypeTokenEnum.DEFAULT;
    }

    @Override
    public boolean isScan() {
        String erc20NotScanStr = environment.getProperty("watcher.token.not.scan.erc20");
        boolean erc20NotScan = Boolean.parseBoolean(erc20NotScanStr);
        log.info("erc20NotScanStr:{}, erc20NotScan:{}", erc20NotScanStr, erc20NotScan);
        return !erc20NotScan;
    }

    @Override
    public String getFrom(List<String> topics) {
        String topicFrom = topics.get(1);
        topicFrom = "0x"+topicFrom.substring(topicFrom.length()-40);
        return topicFrom;
    }

    @Override
    public String getTo(List<String> topics) {
        String topicTo = topics.get(2);
        topicTo = "0x"+topicTo.substring(topicTo.length()-40);
        return topicTo;
    }

    @Override
    public BigInteger getNftId(List<String> topics, String logData) {
        String nftIdHexadecimal = topics.size() > 3 ? topics.get(3): logData;
        return VmChainUtil.hexadecimal2Decimal(nftIdHexadecimal);
    }

    @Override
    public Long getAmount(Web3j web3j, String contractAddress, String address, Long tokenId) {
        return 1L;
    }

    @Override
    public String getNftData(Web3j web3j, String contract, BigInteger tokenId) {
        Utf8String tokenURL = vm30Utils.tokenURL(web3j, contract, tokenId);
        log.info("ErcTokenDefaultServiceImpl.getNftData:{}", tokenURL);
        return tokenURL.toString();
    }

    @Override
    public String getNftName(String contractName, String nftData) {
        return contractName;
    }

}
