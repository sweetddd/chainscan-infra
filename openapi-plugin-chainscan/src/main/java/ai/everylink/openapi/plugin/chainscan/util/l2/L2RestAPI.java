package ai.everylink.openapi.plugin.chainscan.util.l2;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Maps;
import io.zksync.domain.token.Token;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * L2 client api
 */
public class L2RestAPI {

    private RestTemplate restTemplate;
    private String restUrl;
    //交易历史接口
    private final static String TRANSACTION_HISTORY_ACTION = "/account/{address}/history/{offset}/{limit}";
    private final static String TOKENS_ACTION = "/tokens";

    public L2RestAPI(String restUrl) {
        this.restTemplate = new RestTemplate();
        this.restUrl = restUrl;
    }

    public static L2RestAPI createClientProxy(String restUrl) {
        return new L2RestAPI(restUrl);
    }

    public JSONArray transactionHistory(String address, int offset, int limit) {
        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("address", address);
        parameters.put("offset", offset);
        parameters.put("limit", limit);
        return restTemplate.getForObject(restUrl + TRANSACTION_HISTORY_ACTION, JSONArray.class, parameters);
    }

    public JSONArray transactionHistoryAll(String address) {
        JSONArray result = new JSONArray();
        int page = 100000 / 100;
        int limit = 100;
        int offset = 0;
        for (int i = 0; i < page; i++) {
            if (i > 0) {
                offset = 100 * i +1;
            }
            Map<String, Object> parameters = Maps.newHashMap();
            parameters.put("address", address);
            parameters.put("offset", offset);
            parameters.put("limit", limit);
            JSONArray trans = restTemplate.getForObject(restUrl + TRANSACTION_HISTORY_ACTION, JSONArray.class, parameters);
            if(trans.isEmpty()){
                break;
            }else{
                result.addAll(trans);
            }
        }
        return result;
    }

    public Token[] tokens() {
        return restTemplate.getForObject(restUrl + TOKENS_ACTION, Token[].class);
    }
}
