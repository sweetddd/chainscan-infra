package ai.everylink.chainscan.watcher.plugin.service.impl;

import ai.everylink.chainscan.watcher.plugin.dao.BlockDao;
import ai.everylink.chainscan.watcher.plugin.dao.TransactionDao;
import ai.everylink.chainscan.watcher.plugin.entity.Block;
import ai.everylink.chainscan.watcher.plugin.entity.IncentiveBlock;
import ai.everylink.chainscan.watcher.plugin.entity.IncentiveTransaction;
import ai.everylink.chainscan.watcher.plugin.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.service.IncentiveService;
import ai.everylink.chainscan.watcher.plugin.util.BlockScanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class IncentiveServiceImpl implements IncentiveService {



    private static final String resource = "INCENTIVE";

    @Autowired
    private BlockScanUtil blockScanUtil;

    @Autowired
    private BlockDao blockDao;

    @Autowired
    private TransactionDao transactionDao;

    @Override
    public List<IncentiveBlock> incentiveBlocksScan(Integer pageSize) {
        return blockScanUtil.incentiveBlocksScan(pageSize);
    }

    @Override
    public IncentiveBlock incentiveLastBlockScan() {
        String storage = blockScanUtil.getLastBlocStorage();
        return blockScanUtil.getBlock(storage);
    }

    @Override
    public Block selectBlockByBlockHash(String blockHash) {
        return blockDao.getBlockByHash(blockHash, 97);
    }

    @Override
    public Long selectMaxHeight() {
        return blockDao.getMaxBlockNum(97);
    }

    @Override
    public Block saveBlock(Block block) {
        blockDao.save(block);
        return selectBlockByBlockHash(block.getBlockHash());
    }

    @Override
    public void saveTransaction(Transaction transaction) { transactionDao.save(transaction); }

    @Override
    public Block incentiveBlockConvert(IncentiveBlock incentiveBlock) {
        Block block = new Block();
        block.setBlockNumber(incentiveBlock.getBlockHeight().longValue());
        block.setBlockHash(incentiveBlock.getBlockHash());
        block.setChainId(97);
        block.setBlockTimestamp(new Date(incentiveBlock.getStartTime().longValue()));
        block.setParentHash(incentiveBlock.getParentHash());
        block.setTxSize(incentiveBlock.getTransactionCount().intValue());
        block.setDifficulty(incentiveBlock.getDifficulty().toString());
        block.setBlockFee(incentiveBlock.getBlockedFee());
        block.setSource(resource);
        block.setCreateTime(new Date());
        return block;
    }

    @Override
    public Transaction incentiveTransactionConvert(Block block, IncentiveTransaction incentiveTransaction, Integer index) {
        Transaction transaction = new Transaction();
        transaction.setTransactionHash(incentiveTransaction.getTransactionHash());
        transaction.setTransactionIndex(index);
        transaction.setBlockHash(block.getBlockHash());
        transaction.setBlockNumber(block.getBlockNumber());
        transaction.setChainId(block.getChainId());
        transaction.setStatus("0");
        transaction.setFromAddr(incentiveTransaction.getSellerAddress());
        transaction.setToAddr(incentiveTransaction.getBuyerAddress());
        transaction.setCoinSymbol(incentiveTransaction.getCoinSymbol());
        transaction.setPrice(incentiveTransaction.getPrice());
        transaction.setAmount(incentiveTransaction.getAmount());
        transaction.setSellerFee(incentiveTransaction.getSellerFee());
        transaction.setBuyerFee(incentiveTransaction.getBuyerFee());
        transaction.setSource(resource);
        transaction.setCreateTime(new Date());
        return transaction;
    }
}
