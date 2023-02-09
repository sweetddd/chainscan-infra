package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.entity.NftAuction;
import ai.everylink.chainscan.watcher.entity.Transaction;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.List;

public interface ErcNftService {

    ErcTypeNftEnum type();

    boolean isScan();

    String getNftData(Web3j web3j, String contract, BigInteger tokenId);

    String getNftData2(Web3j web3j, String contract, BigInteger tokenId);

    Integer getNftId(List<String> params);

    void createNewNftAuction(String nftData, NftAuction nftAuction, List<String> params, Transaction transaction);

    void cancelNftAuction(Transaction transaction, List<String> params);

}
