package ai.everylink.chainscan.watcher.plugin.service;


import ai.everylink.chainscan.watcher.entity.Block;
import ai.everylink.chainscan.watcher.entity.IncentiveBlock;
import ai.everylink.chainscan.watcher.entity.IncentiveTransaction;
import ai.everylink.chainscan.watcher.entity.Transaction;

import java.util.List;

public interface IncentiveService {

    List<IncentiveBlock> incentiveBlocksScan(Integer pagesize);

    IncentiveBlock incentiveLastBlockScan();

    Block selectBlockByBlockHash(String blockHash);

    Block incentiveBlockConvert(IncentiveBlock incentiveBlock);

    Block saveBlock(Block block);

    List<Block> findBlock(Block block);

    Transaction incentiveTransactionConvert(Block block, IncentiveTransaction incentiveTransaction, Integer index);

    void saveTransaction(Transaction transaction);

    Long selectMaxHeight();

}
