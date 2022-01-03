package ai.everylink.chainscan.watcher.plugin.service;

import ai.everylink.chainscan.watcher.plugin.entity.IncentiveBlock;
import ai.everylink.chainscan.watcher.plugin.entity.Block;
import ai.everylink.chainscan.watcher.plugin.entity.IncentiveTransaction;
import ai.everylink.chainscan.watcher.plugin.entity.Transaction;

import java.util.List;

public interface IncentiveService {

    List<IncentiveBlock> incentiveBlocksScan(Integer pagesize);

    IncentiveBlock incentiveLastBlockScan();

    Block selectBlockByBlockHash(String blockHash);

    Block incentiveBlockConvert(IncentiveBlock incentiveBlock);

    Block saveBlock(Block block);

    Transaction incentiveTransactionConvert(Block block, IncentiveTransaction incentiveTransaction, Integer index);

    void saveTransaction(Transaction transaction);

    Long selectMaxHeight();

}
