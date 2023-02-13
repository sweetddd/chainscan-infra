package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.entity.NftAccount;
import ai.everylink.chainscan.watcher.entity.NftAuction;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.bean.CreateNewNftAuctionBean;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.List;

public interface ErcNftService {

    ErcTypeNftEnum type();

    boolean isScan();

    String getNftData(Web3j web3j, String contract, BigInteger tokenId);

    Long getNftId(List<String> params);

    void createNewNftAuction(CreateNewNftAuctionBean createBean, NftAuction nftAuction);

    void finishNftAuction(Transaction transaction, List<String> params, Web3j web3j);

    void cancelNftAuction(Transaction transaction, List<String> params, Web3j web3j);

    Long getAmount(Web3j web3j, String contractAddress, String address, Long tokenId);

    int updateNftAccountAmount(NftAccount nftAccount, Long amount);

    NftAuction getByTxHash(String txHash);

}
