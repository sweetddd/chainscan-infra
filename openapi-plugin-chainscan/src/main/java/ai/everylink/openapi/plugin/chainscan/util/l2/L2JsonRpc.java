package ai.everylink.openapi.plugin.chainscan.util.l2;

import com.alibaba.fastjson.JSONObject;
import com.googlecode.jsonrpc4j.JsonRpcMethod;

/**
 * L2 js rpc api
 */
public interface L2JsonRpc {
    @JsonRpcMethod("account_info")
    JSONObject accountInfo(String address);
    @JsonRpcMethod("get_tx_fee")
    JSONObject getTxFee(String feeType, String address, String tokenLike);
    @JsonRpcMethod("get_tx_fee")
    JSONObject getTxChangePubKeyFee(JSONObject feeType, String address, String tokenLike);
    @JsonRpcMethod("contract_address")
    String contractAddress();
    @JsonRpcMethod("tx_info")
    JSONObject txInfo(String txHash);
}
