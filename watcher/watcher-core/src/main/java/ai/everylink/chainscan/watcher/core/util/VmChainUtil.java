package ai.everylink.chainscan.watcher.core.util;

import io.emeraldpay.polkaj.api.*;
import io.emeraldpay.polkaj.apihttp.JavaHttpAdapter;
import io.emeraldpay.polkaj.json.BlockResponseJson;
import io.emeraldpay.polkaj.json.SystemHealthJson;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleExtract;
import io.emeraldpay.polkaj.scaletypes.Metadata;
import io.emeraldpay.polkaj.scaletypes.MetadataReader;
import io.emeraldpay.polkaj.types.ByteData;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * @Author Brett
 * @Description 琏上数据获取工具
 * @Date 2021/12/22 15:27
 **/
@Slf4j
@Component
public class VmChainUtil {

    @Value("${evm.vmChainUrl:}")
    private String vmUrl;

    public String getFinalizedHead() {
        String storage = getStorage(null, "chain_getFinalizedHead");
        if (StringUtils.isEmpty(storage)) {
            return null;
        }
        return storage;
    }

    /**
     * 获取1层质押量
     *
     * @return
     */
    public String getVMpledge() {
        String l1LockAmount = "";
        String currentEra = getCurrentEra();
        ArrayList<Object> params  = new ArrayList<>();
        String            key     = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("Staking").getBytes(), 128));
        String            modelue = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("ErasTotalStake").getBytes(), 128));
        params.clear();
        params.add("0x" + key + modelue);
        params.add(1000);
        params.add("0x" + key + modelue);
        String storage = getStorage(params, "state_getKeysPaged");
        ArrayList<String> queryParam  = new ArrayList<>();
        if(StringUtils.isNotBlank(storage)){
            storage = storage.replace("[","");
            storage = storage.replace("]","");
            storage = storage.replace(" ","");
            String[]  split      = storage.split(",");
            for (String item : split) {
                String eraIedex = item.substring(item.length() - 8, item.length());
                String currentIndex = currentEra.substring(2);
               if(eraIedex.equals(currentIndex)){
                   ArrayList<Object> param = new ArrayList<>();
                   ArrayList<Object> index = new ArrayList<>();
                   index.add(item);
                   param.add(index);
                   String amount = getStorage(index, "state_getStorage");
                    if(StringUtils.isNotBlank(amount)){
                        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(amount.substring(2)));
                        BigInteger bigInteger = rdr.readUint128();
                        l1LockAmount = bigInteger.toString();
                    }
               }
            }
        }
        if (StringUtils.isEmpty(l1LockAmount)) {
            return null;
        }
        return l1LockAmount;
    }


    /**
     * 获取最新质押数据索引;
     *
     * @return
     */
    public String getCurrentEra() {
        ArrayList<Object> params  = new ArrayList<>();
        String            currentEraKey     = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("Staking").getBytes(), 128));
        String            currentEra = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("CurrentEra").getBytes(), 128));
        params.add("0x" + currentEraKey + currentEra);
        String eraIndex = getStorage(params, "state_getStorage");
        if (StringUtils.isEmpty(eraIndex)) {
            return "";
        }
        return eraIndex;
    }


    /**
     * 请求rpc接口
     *
     * @param params
     * @param method
     * @return
     */
    public String getStorage(ArrayList<Object> params, String method) {
        HttpService httpService      = new HttpService("http://10.233.75.29:9934", new OkHttpClient(), false);
        Request     state_getStorage = null;
        log.info("method:" + method);
        if (params == null || params.size() < 1) {

            state_getStorage = new Request<>(
                    method,
                    null,
                    httpService,
                    Response.class);
        } else {
            log.info("params:" + params.toString());
            state_getStorage = new Request<>(
                    method,
                    params,
                    httpService,
                    Response.class);
        }
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

    public static byte[] readMessage(String input) {
        byte[] msg = new byte[0];
        try {
            msg = Hex.decodeHex(input);
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return msg;
    }



    @SneakyThrows
    public static void main(String[] args) {
        JavaHttpAdapter client = JavaHttpAdapter.newBuilder()
                //.connectTo("http://vmtest.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf")
                .connectTo("http://10.233.75.29:9934")
                .basicAuth("", "")
                .build();

        ArrayList<Object> params  = new ArrayList<>();
        String            key     = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("Staking").getBytes(), 128));
        String            modelue = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("ErasTotalStake").getBytes(), 128));
        params.clear();
        params.add("0x" + key + modelue);
        params.add(1000);
        params.add("0x" + key + modelue);

//        CompletableFuture<SystemHealthJson> future = client.produceRpcFuture(
//                StandardCommands.getInstance().systemHealth()
//        );
//
//        CompletableFuture<SystemHealthJson> systemHealthJsonCompletableFuture = client.produceRpcFuture(
//                StandardCommands.getInstance().systemHealth()
//        );
//        System.out.println(systemHealthJsonCompletableFuture);


        ScaleCodecReader rdr = new ScaleCodecReader(readMessage("ea229bbcf3fcd5d20d00000000000000"));
        BigInteger bigInteger = rdr.readUint128();
        System.out.println(bigInteger);
        System.out.println("0x5f3e4907f716ac89b6347d15ececedcaa141c4fe67c2d11f4a10c6aca7a79a040e0d969b0e48cab707000000".length());
    }
}
