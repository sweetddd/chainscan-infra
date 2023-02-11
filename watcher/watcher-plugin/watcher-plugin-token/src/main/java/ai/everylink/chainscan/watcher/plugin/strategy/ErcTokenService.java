package ai.everylink.chainscan.watcher.plugin.strategy;

import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.List;

public interface ErcTokenService {

    ErcTypeTokenEnum type();

    boolean isScan();

    String getFrom(List<String> topics);

    String getTo(List<String> topics);

    BigInteger getNftId(List<String> topics, String logData);

    Long getAmount(Web3j web3j, String contractAddress, String address, Long tokenId);

    String getNftData(Web3j web3j, String contract, BigInteger tokenId);

    String getNftName(String contractName, String nftData);

}
