package ai.everylink.chainscan.watcher.plugin.bean;

import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.strategy.ErcNftService;
import lombok.Data;
import org.web3j.protocol.Web3j;

import java.util.List;

@Data
public class CreateNewNftAuctionBean {

    private String fromAddr;
    private String nftContractAddress;
    private ErcNftService ercNftService;
    private Long nftId;
    private List<String> params;
    private Transaction transaction;
    private Web3j web3j;


}
