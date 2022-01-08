package ai.everylink.chainscan.watcher.plugin.util;

import ai.everylink.chainscan.watcher.core.util.UtilsCrypto;
import ai.everylink.chainscan.watcher.entity.IncentiveBlock;
import ai.everylink.chainscan.watcher.entity.IncentiveTransaction;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
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
import java.math.BigInteger;
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
     private static final String vmUrl = "http://vmchain-dev-node-0-sandbox.chain-sandbox.svc.cluster.local:9934";
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
        block.setBlockHeight(BlockAnalysisUtil.getBlockHeight(storage));
        block.setDifficulty(BlockAnalysisUtil.getDifficulty(storage));
        block.setBlockedFee(new BigDecimal(BlockAnalysisUtil.getBlockedFee(storage)));
        block.setStartTime(BlockAnalysisUtil.getStartTime(storage));
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

    public static String getLastBlocStorage() {
        byte[] keyHash = UtilsCrypto.xxhashAsU8a(("CposContribution").getBytes(), 128);
        String s = Hex.encodeHexString(keyHash);
        byte[] lastBlock = UtilsCrypto.xxhashAsU8a(("LastContributionBlock").getBytes(), 128);
        String hexString = Hex.encodeHexString(lastBlock);
        String storage = getStorage("0x" + s + hexString, "state_getStorage");
        if (StringUtils.isEmpty(storage)) {
            return null;
        }
        return storage;
    }

    public static List<String> getBlocksStorage(Integer pageSize) {
        List<String> result = new ArrayList<>();
        byte[] keyHash = UtilsCrypto.xxhashAsU8a(("CposContribution").getBytes(), 128);
        String stateStr = Hex.encodeHexString(keyHash);
        byte[] queryStr = UtilsCrypto.xxhashAsU8a(("ContributionBlocks").getBytes(), 128);
        String hexString = Hex.encodeHexString(queryStr);
        String paramStr = "0x" + stateStr + hexString;
        List<Object> paramList = Arrays.asList(paramStr, pageSize, paramStr);
        List<String> storageKeys = (List<String>) getStorage("state_getKeysPaged", paramList);
        for (String storageKey : storageKeys) {
            try {
                String storage = (String) getStorage("state_getStorage", List.of(storageKey));
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
            block.setExtrinsics(transactions);
            result.add(block);
        }
        return result;
    }

    /**
     * 查询最后一个区块信息
     *
     * @return
     */
    public static ArrayList<IncentiveTransaction> getBlockTxs(String storage) {
        //解析交易明细
        int count = (int) BlockAnalysisUtil.getTransactionCount(storage);
        System.out.println("%%%%%%%%%: " + count + " " + storage.length());
        ArrayList<IncentiveTransaction> list  = new ArrayList<>();
        int index = 180;
        for (int i = 0; i < count; i++) {
            String str = storage.substring(index, index + 314);
            System.out.println();
            IncentiveTransaction transaction = getTransaction(str);
            System.out.println("==========: " + str);
            System.out.println(transaction);
            list.add(transaction);
            index = index + 314 ;
        }
        return list;
    }

    public static void main(String[] args) {
        String test0 = "0x1f000000000000001600000000000000000000000000000090830a00000000000000000000000000000000000000000089063b5eaba06850c4b6c9571c4656c87cceb604264d25e92079e39a89815ba601000000000000000";
        System.out.println(test0.length());
        String test1 = "0x0e0000000000000020bcbe000000000000000000000000002a81cc000000000000000000000000000000000000000000117613e734b6cb0fd7b7583f5b0e863a3f0c856cd32fa36f1b60b464d068c5a60900000000000000";
        System.out.println(test1.length());
        String test2 = "204554482d555344542c4167677265676174696f6e0abfe0826e02307b485625db371414713f95f1b40abfe0826e02307b485625db371414713f95f1b4e803000000000000000000000000000000000000000000000000000000000000df5012000000000000000000000000005294040000000000000000000000000081e47a19e6b29b0a65b9591762ce5143ed30d0261e5d24a3201752506b20f15c";
        System.out.println(test2.length());

        List<String> result = new ArrayList<>();
        byte[] moduleName = UtilsCrypto.xxhashAsU8a(("CposContribution").getBytes(), 128);
        String modulePrefix = Hex.encodeHexString(moduleName);
        byte[] storageName = UtilsCrypto.xxhashAsU8a(("ContributionBlocks").getBytes(), 128);
        String storagePrefix = Hex.encodeHexString(storageName);
        String paramStr = "0x" + modulePrefix + storagePrefix;
        List<Object> paramList = Arrays.asList(paramStr, 200, paramStr);
        List<String> storageKeys = (List<String>) getStorage("state_getKeysPaged", paramList);
//        System.out.println(storageKeys);
        for (String storageKey : storageKeys) {
            try {
                String storage = (String) getStorage("state_getStorage", List.of(storageKey));
                System.out.println("###: " + storage);
                IncentiveBlock incentiveBlock = getBlock(storage);
                System.out.println(incentiveBlock);
                ArrayList<IncentiveTransaction> transactions = getBlockTxs(storage);
            } catch (Exception err){
                log.warn("获取Storage异常：{}", err.getMessage());
            }
        }


//        String storage = getLastBlocStorage();
//        System.out.println(storage);
//        IncentiveBlock incentiveBlock = getBlock(storage);
//        System.out.println(incentiveBlock);
//        ArrayList<IncentiveTransaction> transactions = getBlockTxs(storage);
//        System.out.println(transactions);

//        byte[] keyHash = UtilsCrypto.xxhashAsU8a(("CposContribution").getBytes(), 128);
//        String stateStr = Hex.encodeHexString(keyHash);
//        System.out.println(stateStr);
//        byte[] queryStr = UtilsCrypto.xxhashAsU8a(("ContributionBlocks").getBytes(), 128);
//        String hexString = Hex.encodeHexString(queryStr);
//        System.out.println(hexString);

//        byte[] queryStr1 = UtilsCrypto.xxhashAsU8a(("e88b43fded6323ef02ffeffbd8c40846ee09bf316271bd22369659c959dd733a").getBytes(), 128);
//        String hexString1 = Hex.encodeHexString(queryStr1);
//        System.out.println(hexString1);
//
//        String questHash = "0xe88b43fded6323ef02ffeffbd8c40846ee09bf316271bd22369659c959dd733a";
//        String params = "0x 9293343c31a2fe6d0661fae242f58ffe 36e24f20e1f394fbbce18aeda8bce67a 30fc1a6518928bc2 e88b43fded6323ef02ffeffbd8c40846ee09bf316271bd22369659c959dd733a";
//        String aaaaaa = "0x 9293343c31a2fe6d0661fae242f58ffe 36e24f20e1f394fbbce18aeda8bce67a 30fc1a6518928bc2 e88b43fded6323ef02ffeffbd8c40846ee09bf316271bd22369659c959dd733a";
//        String paeass = "0x 9293343c31a2fe6d0661fae242f58ffe 36e24f20e1f394fbbce18aeda8bce67a 92c8058d20e6ca48 5e67cbdaec7beb4171fe7dbc33e029c014774cbd6b22f125efca486f7664a0ff";
//        String methods = "state_subscribeStorage";
//
//        ScaleCodecReader rdr  = new ScaleCodecReader(readMessage("30fc1a6518928bc2"));
//        String storage1 = "30fc1a6518928bc2";
//        System.out.println(BlockAnalysisUtil.hexStringToString(storage1));
//        System.out.println(rdr.readUint16());
//        System.out.println(rdr.readUint32());


    }

    /**
     * 查询最后一个区块信息
     *
     * @return
     */
    public static IncentiveBlock getBlock(String storage) {
        IncentiveBlock block = new IncentiveBlock();
        block.setBlockHeight(BlockAnalysisUtil.getBlockHeight(storage));
        block.setDifficulty(BlockAnalysisUtil.getDifficulty(storage));
        block.setBlockedFee(new BigDecimal(BlockAnalysisUtil.getBlockedFee(storage).longValue()));
        block.setStartTime(BlockAnalysisUtil.getStartTime(storage));
        block.setBlockHash(BlockAnalysisUtil.getBlockHash(storage));
        block.setTransactionCount(BlockAnalysisUtil.getTransactionCount(storage));
        return block;
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
    private static String getStorage(String input, String method) {
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
    private static Object getStorage(String method, List<Object> params) {
        Object result = null;
        HttpService httpService = new HttpService(vmUrl, new OkHttpClient(), false);
        Request request = new Request<>(method, params, httpService, Response.class);
        try {
            result = request.send().getResult();
        } catch (Exception err){
            err.printStackTrace();
        }
        return result;
    }

}
