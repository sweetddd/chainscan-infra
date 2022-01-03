package ai.everylink.chainscan.watcher.core.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.emeraldpay.polkaj.api.*;
import io.emeraldpay.polkaj.apiws.JavaHttpSubscriptionAdapter;
import io.emeraldpay.polkaj.json.BlockResponseJson;
import io.emeraldpay.polkaj.json.MethodsJson;
import io.emeraldpay.polkaj.json.SystemHealthJson;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleExtract;
import io.emeraldpay.polkaj.scaletypes.AccountInfo;
import io.emeraldpay.polkaj.scaletypes.Metadata;
import io.emeraldpay.polkaj.scaletypes.MetadataReader;
import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import io.emeraldpay.polkaj.ss58.SS58Type;
import io.emeraldpay.polkaj.tx.AccountRequests;
import io.emeraldpay.polkaj.tx.ExtrinsicContext;
import io.emeraldpay.polkaj.types.*;
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
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
        //  queryParam.add("0x" + key + modelue + currentEra.substring(2));
        // queryParam.add("0x5f3e4907f716ac89b6347d15ececedcaa141c4fe67c2d11f4a10c6aca7a79a04b4def25cfda6ef3a00000000");
        queryParam.add("0x5f3e4907f716ac89b6347d15ececedcaa141c4fe67c2d11f4a10c6aca7a79a0400000000");

        if(StringUtils.isNotBlank(storage)){
            storage = storage.replace("[","");
            storage = storage.replace("]","");
            storage = storage.replace(" ","");
            String[]  split      = storage.split(",");
            for (String item : split) {
                queryParam.add(item);
            }
        }
        ArrayList<Object> param = new ArrayList<>();
        param.add(queryParam);
        String queryStorageA = getStorage(param, "state_queryStorageAt");
        System.out.println(queryStorageA);
        if (StringUtils.isEmpty(queryStorageA)) {
            return null;
        }
        return queryStorageA;
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
        HttpService httpService      = new HttpService(vmUrl, new OkHttpClient(), false);
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
        DotAmountFormatter AMOUNT_FORMAT = DotAmountFormatter.autoFormatter();

        String api = "ws://10.233.87.45:9944";
        if (args.length >= 1) {
            api = args[0];
        }
        System.out.println("Connect to: " + api);

        Schnorrkel.KeyPair aliceKey;
        Address            alice;
        Address            bob;
        if (args.length >= 3) {
            System.out.println("Use provided addresses");
            aliceKey = Schnorrkel.getInstance().generateKeyPairFromSeed(Hex.decodeHex(args[1]));
            bob =  Address.from(args[2]);
        } else {
            System.out.println("Use standard accounts for Alice and Bob, expected to run against development network");
            aliceKey = Schnorrkel.getInstance().generateKeyPairFromSeed(
                    Hex.decodeHex("e5be9a5092b81bca64be81d212e7f2f9eba183bb7a90954f7b76361f6edb5c0a")
            );
           // bob =  Address.from("0x6Da573EEc80f63c98b88cED15D32CA270787FB5a");
        }
        alice = new Address(SS58Type.Network.CANARY, aliceKey.getPublicKey());

        Random random = new Random();
        DotAmount amount = DotAmount.fromPlancks(
                Math.abs(random.nextLong()) % DotAmount.fromDots(0.002).getValue().longValue()
        );

        final JavaHttpSubscriptionAdapter adapter = JavaHttpSubscriptionAdapter.newBuilder().connectTo(api).build();
        try (PolkadotApi client = PolkadotApi.newBuilder().subscriptionAdapter(adapter).build()) {
            System.out.println("Connected: " + adapter.connect().get());

            // Subscribe to block heights
            AtomicLong              height        = new AtomicLong(0);
            CompletableFuture<Long> waitForBlocks = new CompletableFuture<>();
            client.subscribe(
                    StandardSubscriptions.getInstance().newHeads()
            ).get().handler((event) -> {
                long current = event.getResult().getNumber();
                System.out.println("Current height: " + current);
                if (height.get() == 0) {
                    height.set(current);
                } else {
                    long blocks = current - height.get();
                    if (blocks > 3) {
                        waitForBlocks.complete(current);
                    }
                }
            });

            // Subscribe to balance updates
            AccountRequests.AddressBalance aliceAccountRequest = AccountRequests.balanceOf(alice);
           // AccountRequests.AddressBalance bobAccountRequest   = AccountRequests.balanceOf(bob);
//            client.subscribe(
//                    StandardSubscriptions.getInstance()
//                            .storage(Arrays.asList(
//                                    // need to provide actual encoded requests
//                                    aliceAccountRequest.encodeRequest(),
//                                    bobAccountRequest.encodeRequest())
//                            )
//            ).get().handler((event) -> {
//                event.getResult().getChanges().forEach((change) -> {
//                    AccountInfo value = null;
//                    Address target = null;
//                    if (aliceAccountRequest.isKeyEqualTo(change.getKey())) {
//                        value = aliceAccountRequest.apply(change.getData());
//                        target = alice;
//                    } else if (bobAccountRequest.isKeyEqualTo(change.getKey())) {
//                        value = bobAccountRequest.apply(change.getData());
//                        target = bob;
//                    } else {
//                        System.err.println("Invalid key: " + change.getKey());
//                    }
//                    if (value != null) {
//                        System.out.println("Balance update. User: " + target + ", new balance: " + AMOUNT_FORMAT.format(value.getData().getFree()));
//                    }
//                });
//            });

            // get current runtime metadata to correctly build the extrinsic
            Metadata metadata = client.execute(
                            StandardCommands.getInstance().stateMetadata()
                    )
                    .thenApply(ScaleExtract.fromBytesData(new MetadataReader()))
                    .get();

            // prepare context for execution
            ExtrinsicContext context = ExtrinsicContext.newAutoBuilder(alice, client)
                    .get()
                    .build();

            // get current balance to show, optional
            AccountInfo aliceAccount = aliceAccountRequest.execute(client).get();

            System.out.println("Using genesis : " + context.getGenesis());
            System.out.println("Using runtime : " + context.getTxVersion() + ", " + context.getRuntimeVersion());
            System.out.println("Using nonce   : " + context.getNonce());
            System.out.println("------");
            System.out.println("Currently available: " + AMOUNT_FORMAT.format(aliceAccount.getData().getFree()));
         //   System.out.println("Transfer           : " + AMOUNT_FORMAT.format(amount) + " from " + alice + " to " + bob);

            // prepare call, and sign with sender Secret Key within the context
//            AccountRequests.Transfer transfer = AccountRequests.transfer()
//                    .runtime(metadata)
//                    .from(alice)
//                    .to(bob)
//                    .amount(amount)
//                    .sign(aliceKey, context)
//                    .build();

//            ByteData req = transfer.encodeRequest();
//            System.out.println("RPC Request Payload: " + req);
//            Hash256 txid = client.execute(
//                    StandardCommands.getInstance().authorSubmitExtrinsic(req)
//            ).get();
//            System.out.println("Tx Hash: " + txid);

            // wait for a few blocks, to show how subscription to storage changes works, which will
            // notify about relevant updates during those blocks
            waitForBlocks.get();
        }
    }
}
