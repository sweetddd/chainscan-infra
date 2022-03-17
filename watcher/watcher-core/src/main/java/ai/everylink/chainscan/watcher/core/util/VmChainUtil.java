package ai.everylink.chainscan.watcher.core.util;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
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
import java.util.List;
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
     * Staking缓存未发放的奖励 取链上的cposStaking的PendingRewards   ，单位是MOBI
     *
     * @return
     */
    public String getPendingRewards() {
        String            pendingRewards = "0";
        ArrayList<Object> params         = new ArrayList<>();
        ArrayList<Object> param          = new ArrayList<>();
        String            key            = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("CposStaking").getBytes(), 128));
        String            modelue        = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("PendingRewards").getBytes(), 128));
        param.add("0x" + key + modelue);
        params.add(param);
        String storage = getStorage(param, "state_getStorage");
        if (StringUtils.isNotBlank(storage)) {
            ScaleCodecReader rdr        = new ScaleCodecReader(readMessage(storage.substring(2)));
            BigInteger       bigInteger = rdr.readUint128();
            pendingRewards = bigInteger.toString();
        }
        return pendingRewards;
    }

    /**
     * 不足144个区块尚未分红的奖励。取链上的cposContribution的BufferRewards。单位是MOBI
     *
     * @return
     */
    public String getBufferRewards() {
        String            bufferRewards = "0";
        ArrayList<Object> params        = new ArrayList<>();
        ArrayList<Object> param         = new ArrayList<>();
        String            key           = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("CposContribution").getBytes(), 128));
        String            modelue       = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("BufferRewards").getBytes(), 128));
        param.add("0x" + key + modelue);
        params.add(param);
        String storage = getStorage(param, "state_getStorage");
        if (StringUtils.isNotBlank(storage)) {
            ScaleCodecReader rdr        = new ScaleCodecReader(readMessage(storage.substring(2)));
            BigInteger       bigInteger = rdr.readUint128();
            bufferRewards = bigInteger.toString();
        }
        return bufferRewards;
    }


    /**
     * 获取1层质押量
     *
     * @return
     */
    public String getVMpledge() {
        String            l1LockAmount = "";
        String            currentEra   = getCurrentEra();
        ArrayList<Object> params       = new ArrayList<>();
        String            key          = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("Staking").getBytes(), 128));
        String            modelue      = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("ErasTotalStake").getBytes(), 128));
        params.clear();
        params.add("0x" + key + modelue);
        params.add(1000);
        params.add("0x" + key + modelue);
        String            storage    = getStorage(params, "state_getKeysPaged");
        ArrayList<String> queryParam = new ArrayList<>();
        if (StringUtils.isNotBlank(storage)) {
            storage = storage.replace("[", "");
            storage = storage.replace("]", "");
            storage = storage.replace(" ", "");
            String[] split = storage.split(",");
            for (String item : split) {
                String eraIedex     = item.substring(item.length() - 8, item.length());
                String currentIndex = currentEra.substring(2);
                if (eraIedex.equals(currentIndex)) {
                    ArrayList<Object> index = new ArrayList<>();
                    index.add(item);
                    String amount = getStorage(index, "state_getStorage");
                    if (StringUtils.isNotBlank(amount)) {
                        ScaleCodecReader rdr        = new ScaleCodecReader(readMessage(amount.substring(2)));
                        BigInteger       bigInteger = rdr.readUint128();
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
        ArrayList<Object> params        = new ArrayList<>();
        String            currentEraKey = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("Staking").getBytes(), 128));
        String            currentEra    = Hex.encodeHexString(UtilsCrypto.xxhashAsU8a(("CurrentEra").getBytes(), 128));
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

//        Hash256 from = Hash256.from("0xdec6cbe3a9abda95639a17634685d8acfefcabb807b942cd6be7187edc21c4f7");
//        System.out.println(from.toString());
//
//        PolkadotApi client = PolkadotApi.newBuilder().rpcCallAdapter(JavaHttpAdapter.newBuilder().connectTo("http://10.233.75.29:9934").build()).build();
////
////        DotAmount total = AccountRequests.totalIssuance().execute(client).get();
//        RpcCall<BlockResponseJson> block = StandardCommands.getInstance().getBlock(from);
//
//        CompletableFuture<BlockResponseJson> execute = client.execute(block);
//        try {
//            BlockResponseJson blockResponseJson = execute.get();
//
//            System.out.println(blockResponseJson);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
//        client.close();

//        JavaHttpAdapter httpAdapter = JavaHttpAdapter.newBuilder()
//                //.connectTo("http://vmtest.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf")
//                .connectTo("http://10.233.120.228:9934")
//                .basicAuth("", "")
//                .build();
//        PolkadotApi api = PolkadotApi.newBuilder()
//                .rpcCallAdapter(httpAdapter)
//                .build();
//
//        Hash256 from = Hash256.from("0x70f7a409201ca86177817c441f5b64de331aa06ccd730e5c7162cb1d46a6a76a");
//
//        Future<Hash256> hashFuture = api.execute(
//            RpcCall.create(Hash256.class, PolkadotMethod.CHAIN_GET_FINALIZED_HEAD)
//        );
//        Hash256 hash      = hashFuture.get();
//        Hash256 blockHash = api.execute(PolkadotApi.commands().getBlockHash()).get();
//
//        //查询指定区块数据;
//        Future<BlockResponseJson> blockFuture = api.execute(
//                PolkadotApi.commands().getBlock(from)
//        );
//        BlockResponseJson block = blockFuture.get();
//
//        String version = api.execute(PolkadotApi.commands().systemVersion())
//                .get(5, TimeUnit.SECONDS);
//
//        RuntimeVersionJson runtimeVersion = api.execute(PolkadotApi.commands().getRuntimeVersion())
//                .get(5, TimeUnit.SECONDS);
//
//        SystemHealthJson health = api.execute(PolkadotApi.commands().systemHealth())
//                .get(5, TimeUnit.SECONDS);
//        System.out.println("Software: " + version);
//        System.out.println("Spec: " + runtimeVersion.getSpecName() + "/" + runtimeVersion.getSpecVersion());
//        System.out.println("Impl: " + runtimeVersion.getImplName() + "/" + runtimeVersion.getImplVersion());
//        System.out.println("Peers count: " + health.getPeers());
//        System.out.println("Is syncing: " + health.getSyncing());
//        System.out.println("Current head: " + hash);
//        System.out.println("Current block hash: " + blockHash);
//        System.out.println("Current height: " + block.getBlock().getHeader().getNumber());
//        System.out.println("State hash: " + block.getBlock().getHeader().getStateRoot());
//        api.close();

        //0x280403000bc11000287e01
        //0xed0184043cd0a705a2dc65e5b1e1205896baa2be8a07c6e001ef14848a071f9790b9db7706e65c102dddb818d41a06c80d6294b846da47da087b9e330ada7f8131d109ff20122de5a3c1f4e0981e12dbe41564721468b8d80065022400050304c0f0f4ab324c46e55d02d0033343b4be8a55532d130000c84e676dc11b
        //0xed0184043cd0a705a2dc65e5b1e1205896baa2be8a07c6e017989320e659e726eabf08fd3f293f90c18d34f38b165cfa93b8346e50f135fea71366bdc4d102c54ea49310ddb978410a7f8220556c4dd7bdb047c88692ab400175022800050304c0f0f4ab324c46e55d02d0033343b4be8a55532d13000064a7b3b6e00d

//        ExtrinsicReader<BalanceTransfer> reader = new ExtrinsicReader<>(
//                new BalanceTransferReader(SS58Type.Network.SUBSTRATE),
//                SS58Type.Network.SUBSTRATE
//        );
//
//
//
//       String existing = "51028400a6a11c9cf2b58fd914ffc8f667e31e8e6175514833a2892100c8c3bcc904906100634c879c40daf331254bafdbfb24ac3f5286f60d38ed4d056caffd6c5efbd8451fbb0e277f2be832e8e8aad428492c25e8f354f9976500a41e8943284a4e540b0004074ea0efcd01040300b587b6f4e35da071696161b345b378eb282c884a03d23cf7e44ba27cf3f63d4c070088526a74";
//        ScaleCodecReader rdr = new ScaleCodecReader(Hex.decodeHex(existing));
//        Extrinsic<BalanceTransfer> read = reader.read(rdr);
//        System.out.println(read);



//
//        List<ByteData> extrinsics = block.getBlock().getExtrinsics();
//        for (ByteData extrinsic : extrinsics) {
//            //System.out.println(extrinsic);
//        }
//
//        String existing = "41028400b8fdf4f080eeaa6d3f32a445c91c7effa6ffef16d5fe81783837ab7a23602b3b01bc11655de6e7461b0951353db25f4aaf67a58db547fa3a2f20cbcd7772ba715f8ccbe9d8bddf253c7f6e6f6acb83848a7da1f27de248afca10d3291de92ede8ce5000c00040000483eae8765348ef3e347e6b55995f99353223a8b28cf63829554933bcd5e801d0780cff40808";
//        String existing1 = "ed0184043cd0a705a2dc65e5b1e1205896baa2be8a07c6e001ef14848a071f9790b9db7706e65c102dddb818d41a06c80d6294b846da47da087b9e330ada7f8131d109ff20122de5a3c1f4e0981e12dbe41564721468b8d80065022400050304c0f0f4ab324c46e55d02d0033343b4be8a55532d130000c84e676dc11b";
//        String existing2 = "0xed0184043cd0a705a2dc65e5b1e1205896baa2be8a07c6e017989320e659e726eabf08fd3f293f90c18d34f38b165cfa93b8346e50f135fea71366bdc4d102c54ea49310ddb978410a7f8220556c4dd7bdb047c88692ab400175022800050304c0f0f4ab324c46e55d02d0033343b4be8a55532d13000064a7b3b6e00d";
//        ExtrinsicReader<BalanceTransfer> reader = new ExtrinsicReader<>(
//                new BalanceTransferReader(SS58Type.Network.SUBSTRATE_SECONDARY),
//                SS58Type.Network.substrate_SECONDARY
//        );
//        ScaleCodecReader           scaleCodecReader = new ScaleCodecReader(Hex.decodeHex(existing));
//        Extrinsic<BalanceTransfer> read = reader.read(scaleCodecReader);
//        System.out.println(read);
//        ScaleCodecReader rdr = new ScaleCodecReader(readMessage("ed018404"));
//        int       value = rdr.readUint16();
//        System.out.println(value);



        ScaleCodecReader rdr        = new ScaleCodecReader(readMessage("65022400050304130000c84e676dc11b"));
        //int              i          = Byte.toUnsignedInt("65022400050304");
        System.out.println(intToByteArray(9));
    }

    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte)((i >> 24) & 0xFF);
        result[1] = (byte)((i >> 16) & 0xFF);
        result[2] = (byte)((i >> 8) & 0xFF);
        result[3] = (byte)(i & 0xFF);
        return result;
    }


}
