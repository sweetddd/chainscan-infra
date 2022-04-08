package ai.everylink.chainscan.watcher.plugin.service;

import ai.everylink.chainscan.watcher.core.config.DataSourceEnum;
import ai.everylink.chainscan.watcher.core.config.TargetDataSource;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.dao.BlockDataDao;
import ai.everylink.chainscan.watcher.entity.*;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * EVM数据服务
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
@Slf4j
@Service
public class EvmScanDataServiceImpl implements EvmScanDataService {

    @Autowired
    private BlockDataDao blockDataDao;

    @TargetDataSource(value = DataSourceEnum.chainscan)
    @Override
    public Long queryMaxBlockNumber() {
        Long maxBlockNumber = blockDataDao.queryMaxBlockNumber();
        return maxBlockNumber == null ? 0L : maxBlockNumber;
    }

    @TargetDataSource(value = DataSourceEnum.chainscan)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean insert(List<EvmData> dataList) {
        Gson gson = new Gson();
        for (EvmData data : dataList) {
            BlockData block = new BlockData();
            block.setBlockNumber(data.getBlock().getNumber());
            block.setBlockHash(data.getBlock().getHash());
            block.setProcessed((byte)0);
            block.setCreateTime(new Date());
            block.setChainId(data.getChainId());
            block.setBlock(gson.toJson(data.getBlock()));
            if (data.getTxList() != null && data.getTxList().size() > 0) {
                block.setTransaction(gson.toJson(data.getTxList()));
            }
            if (data.getTransactionLogMap() != null && data.getTransactionLogMap().size() > 0) {
                block.setLog(gson.toJson(data.getTransactionLogMap()));
            }
            blockDataDao.save(block);
        }

        return true;
    }

    @Override
    public void deleteBlockData(Long blockNumber) {
        blockDataDao.deleteBlockByBlockNumber(blockNumber);
    }

    @Override
    public List<EvmData> queryBlockList(Long startBlock, Integer limit) {
        if (startBlock == null) {
            log.warn("startBlock is null");
            return Lists.newArrayList();
        }

        if (limit == null) {
            limit = 100;
        }

        List<BlockData> blockList = blockDataDao.listBlock(startBlock, limit);
        if (CollectionUtils.isEmpty(blockList)) {
            log.info("not found blocks.start={},limit={}", startBlock, limit);
            return Lists.newArrayList();
        }

        List<EvmData> retlist = Lists.newArrayList();

        for (BlockData block : blockList) {
            retlist.add(parse(block));
        }
        return retlist;
    }

    private static Gson gson = new GsonBuilder().registerTypeAdapter(
            EthBlock.TransactionResult.class, new TypeAdapter<EthBlock.TransactionResult>() {

                @Override
                public void write(JsonWriter out, EthBlock.TransactionResult value) throws IOException {
                }

                @Override
                public EthBlock.TransactionResult read(JsonReader in) throws IOException {
                    EthBlock.TransactionObject ret = new EthBlock.TransactionObject();
                    in.beginObject();

                    while (in.hasNext()) {
                        String field = in.nextName();
                        if ("hash".equals(field)) {
                            ret.setHash(in.nextString());
                        } else if ("nonce".equals(field)) {
                            ret.setNonce(in.nextString());
                        } else if ("blockHash".equals(field)) {
                            ret.setBlockHash(in.nextString());
                        } else if ("blockNumber".equals(field)) {
                            ret.setBlockNumber(in.nextString());
                        } else if ("transactionIndex".equals(field)) {
                            ret.setTransactionIndex(in.nextString());
                        } else if ("from".equals(field)) {
                            ret.setFrom(in.nextString());
                        } else if ("to".equals(field)) {
                            ret.setTo(in.nextString());
                        } else if ("value".equals(field)) {
                            ret.setValue(in.nextString());
                        } else if ("gasPrice".equals(field)) {
                            ret.setGasPrice(in.nextString());
                        } else if ("gas".equals(field)) {
                            ret.setGas(in.nextString());
                        } else if ("input".equals(field)) {
                            ret.setInput(in.nextString());
                        } else if ("creates".equals(field)) {
                            ret.setCreates(in.nextString());
                        } else if ("publicKey".equals(field)) {
                            ret.setPublicKey(in.nextString());
                        } else if ("raw".equals(field)) {
                            ret.setRaw(in.nextString());
                        } else if ("r".equals(field)) {
                            ret.setR(in.nextString());
                        } else if ("s".equals(field)) {
                            ret.setS(in.nextString());
                        } else if ("v".equals(field)) {
                            ret.setV(in.nextInt());
                        } else{
                            in.skipValue();
                        }
                    }

                    in.endObject();
                    return ret;
                }
            }).create();

    private EvmData parse(BlockData block) {
        try {
            EvmData ret = new EvmData();
            ret.setChainId(block.getChainId());
            ret.setBlock(gson.fromJson(block.getBlock(), EthBlock.Block.class));

            if (!StringUtils.isEmpty(block.getTransaction())) {
                List<TransactionReceipt> list = Lists.newArrayList();
                ret.setTxList(gson.fromJson(block.getTransaction(),
                        TypeToken.getParameterized(Map.class, String.class, TransactionReceipt.class).getType()));
            }
            if (!StringUtils.isEmpty(block.getLog())) {
                ret.setTransactionLogMap(gson.fromJson(block.getLog(),
                        TypeToken.getParameterized(Map.class, String.class,
                            TypeToken.getParameterized(List.class, Log.class).getType()).getType()));
            }

            return ret;
        } catch (Exception e) {
            log.error("error when parse block. num=" + block.getBlockNumber(), e);
        }

        return null;
    }

}
