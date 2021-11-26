package ai.everylink.openapi.plugin.chainscan.util.l2;

import com.google.common.collect.Maps;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class L2Client {

    private final static Map<String, L2JsonRpc> L2_JSON_RPC_POOL = Maps.newConcurrentMap();
    private final static Map<String, L2RestAPI> L2_REST_POOL     = Maps.newConcurrentMap();

    public static L2JsonRpc createJsonRpcProxy(L2Config l2Config,Long chainId) {
        String jsRpc = l2Config.getJsRpc(chainId);
        if (L2_JSON_RPC_POOL.containsKey(jsRpc)) {
            return L2_JSON_RPC_POOL.get(jsRpc);
        }
        L2JsonRpc proxy = ProxyUtil.createClientProxy(L2Client.class.getClassLoader(), L2JsonRpc.class, jsonRpcHttpClient(jsRpc));
        L2_JSON_RPC_POOL.put(jsRpc, proxy);
        return proxy;
    }

    private static JsonRpcHttpClient jsonRpcHttpClient(String jsRpcUrl) {
        URL url = null;
        //You can add authentication headers etc to this map
        Map<String, String> headers = new HashMap<>();
        try {
            url = new URL(jsRpcUrl);
            headers.put("Content-Type", "application/json");
        } catch (Exception e) {
            log.error("Layer2 json rpc client init error:{}", e.getMessage());
        }
        return new JsonRpcHttpClient(url, headers);
    }

    public static L2RestAPI createRestProxy(L2Config l2Config,Long chainId) {

        String rest = l2Config.getRest(chainId);
        if (L2_REST_POOL.containsKey(rest)) {
            return L2_REST_POOL.get(rest);
        }
        L2RestAPI proxy = L2RestAPI.createClientProxy(rest);
        L2_REST_POOL.put(rest, proxy);
        return proxy;

    }
}
