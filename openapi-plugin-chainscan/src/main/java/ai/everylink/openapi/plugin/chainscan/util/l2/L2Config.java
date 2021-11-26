package ai.everylink.openapi.plugin.chainscan.util.l2;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class L2Config {

    private String l2ChainUrl;

    public String getRest(Long l2ChainId) {
        return JSON.parseObject(l2ChainUrl).getJSONObject(String.valueOf(l2ChainId)).getString("rest");
    }

    public String getWeb3j(Long l2ChainId) {
        return JSON.parseObject(l2ChainUrl).getJSONObject(String.valueOf(l2ChainId)).getString("web3j");
    }

    public String getJsRpc(Long l2ChainId) {
        return JSON.parseObject(l2ChainUrl).getJSONObject(String.valueOf(l2ChainId)).getString("jsrpc");
    }


    public List<String> getAllRests() {

        List<String> rests = Lists.newArrayList();
        JSON.parseObject(l2ChainUrl).forEach((k, v) -> {
            rests.add(((JSONObject) v).getString("rest"));
        });
        return rests;
    }

    public List<String> getAllWeb3js() {
        List<String> web3js = Lists.newArrayList();
        JSON.parseObject(l2ChainUrl).forEach((k, v) -> {
            web3js.add(((JSONObject) v).getString("web3j"));
        });
        return web3js;
    }

    public List<String> getAllJsRpcs() {
        List<String> jsrpcs = Lists.newArrayList();
        JSON.parseObject(l2ChainUrl).forEach((k, v) -> {
            jsrpcs.add(((JSONObject) v).getString("jsrpc"));
        });
        return jsrpcs;
    }

    public List<Long> getAllChainIds() {
        List<Long> chainIds = Lists.newArrayList();
        JSON.parseObject(l2ChainUrl).forEach((k, v) -> {
            chainIds.add(Long.valueOf(k));
        });
        return chainIds;
    }

}

