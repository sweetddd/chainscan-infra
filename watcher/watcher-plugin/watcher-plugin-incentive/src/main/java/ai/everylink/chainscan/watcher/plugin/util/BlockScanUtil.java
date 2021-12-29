package ai.everylink.chainscan.watcher.plugin.util;

import ai.everylink.chainscan.watcher.plugin.entity.IncentiveBlock;
import ai.everylink.chainscan.watcher.plugin.entity.IncentiveTransaction;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @Author
 * @Description 区块扫描工具
 * @Date 2021/10/14 15:27
 **/
@Slf4j
@Component
public class BlockScanUtil {

    // @Value("${evm.chain.vmUrl:}")
     private String vmUrl = "http://vmchain-dev-node-0-sandbox.chain-sandbox.svc.cluster.local:9934";
//    private String vmUrl = "http://10.233.65.33:9934";


    public static byte[] readMessage(String input) {
        byte[] msg = new byte[0];
        try {
            msg = Hex.decodeHex(input);
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return msg;
    }

    public IncentiveBlock getIncentiveBlocks(String blockHash) {
        byte[] keyHash = UtilsCrypto.xxhashAsU8a(("Incentive").getBytes(), 128);
        String s = Hex.encodeHexString(keyHash);
        byte[] lastBlock = UtilsCrypto.xxhashAsU8a(("IncentiveBlocks").getBytes(), 128);
        String hexString = Hex.encodeHexString(lastBlock);
        String params = "7c2f95f1641ebe08" + blockHash.substring(2);
        String storage = getStorage("0x" + s + hexString + params, "state_subscribeStorage");
        if (StringUtils.isEmpty(storage)) {
            return null;
        }
        IncentiveBlock block = new IncentiveBlock();
        block.setBlockNumber((long) BlockAnalysisUtil.getBlockHeight(storage));
        block.setDifficulty(BlockAnalysisUtil.getDifficulty(storage));
        block.setBlockedFee(new BigDecimal(BlockAnalysisUtil.getBlockedFee(storage)));
        block.setStartTime(new Date(BlockAnalysisUtil.getStartTime(storage)));
        block.setBlockHash(BlockAnalysisUtil.getBlockHash(storage));
        block.setTransactionCount(BlockAnalysisUtil.getTransactionCount(storage));
        IncentiveTransaction transaction = new IncentiveTransaction();
        transaction.setTransactionHash(BlockAnalysisUtil.getTransactionHash(storage));
        transaction.setSellerFee(new BigDecimal(BlockAnalysisUtil.getSellerFee(storage)));
        transaction.setBuyerFee(new BigDecimal(BlockAnalysisUtil.getBuyerFee(storage)));
        transaction.setPrice(new BigDecimal(BlockAnalysisUtil.getPrice(storage)));
        transaction.setAmount(new BigDecimal(BlockAnalysisUtil.getAmount(storage)));
        transaction.setSellerAddress(BlockAnalysisUtil.getSellerAddress(storage));
        transaction.setBuyerAddress(BlockAnalysisUtil.getBuyerAddress(storage));
        transaction.setCoinSymbol(BlockAnalysisUtil.getCoinSymbol(storage));
        transaction.setTransactionType(BlockAnalysisUtil.getTransactionType(storage));
        ArrayList<IncentiveTransaction> list = new ArrayList<>();
        list.add(transaction);
        block.setExtrinsics(list);
        return block;
    }

    public  String getLastBlocStorage() {
        byte[] keyHash = UtilsCrypto.xxhashAsU8a(("Incentive").getBytes(), 128);
        String s = Hex.encodeHexString(keyHash);
        byte[] lastBlock = UtilsCrypto.xxhashAsU8a(("LastIncentiveBlock").getBytes(), 128);
        String hexString = Hex.encodeHexString(lastBlock);
        String storage = getStorage("0x" + s + hexString, "state_getStorage");
        if (StringUtils.isEmpty(storage)) {
            return null;
        }
        return storage;
    }

    public  String test() {
        byte[] keyHash = UtilsCrypto.xxhashAsU8a(("StakingToken").getBytes(), 128);
        String s = Hex.encodeHexString(keyHash);
        byte[] lastBlock = UtilsCrypto.xxhashAsU8a(("TotalRewards").getBytes(), 128);
        String hexString = Hex.encodeHexString(lastBlock);
        String storage = getStorage("0x" + s + hexString, "state_getStorage");
        if (StringUtils.isEmpty(storage)) {
            return null;
        }
        return storage;
    }

    public  List<String> getBlocksStorage(Integer pageSize) {
        List<String> result = new ArrayList<>();
        byte[] keyHash = UtilsCrypto.xxhashAsU8a(("Incentive").getBytes(), 128);
        String stateStr = Hex.encodeHexString(keyHash);
        byte[] queryStr = UtilsCrypto.xxhashAsU8a(("IncentiveBlocks").getBytes(), 128);
        String hexString = Hex.encodeHexString(queryStr);
        String paramStr = "0x" + stateStr + hexString;
        List<Object> paramList = Arrays.asList(paramStr, pageSize, paramStr);
        List<String> storageKeys = (List<String>) getStorage("state_getKeysPaged", paramList);
        for (String storageKey : storageKeys) {
            try {
                String storage = (String) getStorage("c", List.of(storageKey));
                if (!StringUtils.isEmpty(storage) && !result.contains(storage)){
                    result.add(storage);
                }
            } catch (Exception err){
                log.warn("获取Storage异常：{}", err.getMessage());
            }
        }
        return result;
    }

    public  List<IncentiveBlock> incentiveBlocksScan(Integer pageSize){
        List<IncentiveBlock> result = new ArrayList<>();
        List<String> storages = getBlocksStorage(pageSize);
        for (String storage : storages) {
            IncentiveBlock block = getBlock(storage);
            ArrayList<IncentiveTransaction> transactions = getBlockTxs(storage);
            for (IncentiveTransaction transaction : transactions) {
                transaction.setBlockNumber(block.getBlockHeight());
            }
            block.setExtrinsics(transactions);
            result.add(block);
        }
        return result;
    }

    public static void main(String[] args) {

    }

    /**
     * 查询最后一个区块信息
     *
     * @return
     */
    public IncentiveBlock getBlock(String storage) {
        IncentiveBlock block = new IncentiveBlock();
        block.setBlockHeight((long) BlockAnalysisUtil.getBlockHeight(storage));
        block.setDifficulty(BlockAnalysisUtil.getDifficulty(storage));
        block.setBlockedFee(new BigDecimal(BlockAnalysisUtil.getBlockedFee(storage)));
        block.setStartTime(new Date(BlockAnalysisUtil.getStartTime(storage)*1000));
        block.setBlockHash(BlockAnalysisUtil.getBlockHash(storage));
        block.setTransactionCount(BlockAnalysisUtil.getTransactionCount(storage));
        return block;
    }

    /**
     * 查询最后一个区块信息
     *
     * @return
     */
    public static ArrayList<IncentiveTransaction> getBlockTxs(String storage) {
        //解析交易明细
        int count = BlockAnalysisUtil.getTransactionCount(storage);
        ArrayList<IncentiveTransaction> list  = new ArrayList<>();
        int index = 150;
        for (int i = 0; i < count; i++) {
            String str = storage.substring(index, index + 248);
            IncentiveTransaction transaction = getTransaction(str);
            list.add(transaction);
            index = index + 248 + 2;
        }
        return list;
    }

    /**
     * 解析交易明细
     *
     * @param storage
     * @return
     */
    public static IncentiveTransaction getTransaction(String storage) {
        IncentiveTransaction transaction = new IncentiveTransaction();
        transaction.setCoinSymbol(BlockAnalysisUtil.getCoinSymbol(storage));
        transaction.setTransactionType(BlockAnalysisUtil.getTransactionType(storage));
        transaction.setBuyerAddress(BlockAnalysisUtil.getBuyerAddress(storage));
        transaction.setSellerAddress(BlockAnalysisUtil.getSellerAddress(storage));
        transaction.setAmount(new BigDecimal(BlockAnalysisUtil.getAmount(storage)));
        transaction.setPrice(new BigDecimal(BlockAnalysisUtil.getPrice(storage)));
        transaction.setBuyerFee(new BigDecimal(BlockAnalysisUtil.getBuyerFee(storage)));
        transaction.setSellerFee(new BigDecimal(BlockAnalysisUtil.getSellerFee(storage)));
        transaction.setTransactionHash(BlockAnalysisUtil.getTransactionHash(storage));
        return transaction;
    }

    /**
     * 请求rpc接口
     *
     * @param input
     * @param method
     * @return
     */
    private String getStorage(String input, String method) {
        log.info("getStorage.method:" + method);
        log.info("getStorage.input:" + input);
        HttpService httpService = new HttpService(vmUrl, new OkHttpClient(), false);
        Request state_getStorage = new Request<>(
                method,
                Arrays.asList(input),
                httpService,
                Response.class);
        try {
            Object result = state_getStorage.send().getResult();
            if (null == result) {
                return "";
            }
            return result.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 请求rpc接口
     *
     * @param method
     * @param params
     * @return
     */
    private Object getStorage(String method, List<Object> params) {
        log.info("getStorage.method:" + method);
        log.info("getStorage.params:" + params);
        Object result = null;
        HttpService httpService = new HttpService(vmUrl, new OkHttpClient(), false);
        Request state_getStorage = new Request<>(method, params, httpService, Response.class);
        try {
            result = state_getStorage.send().getResult();
        } catch (Exception err){
            err.printStackTrace();
        }
        return result;
    }

}
