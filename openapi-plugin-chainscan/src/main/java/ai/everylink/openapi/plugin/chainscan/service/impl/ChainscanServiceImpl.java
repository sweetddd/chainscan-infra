package ai.everylink.openapi.plugin.chainscan.service.impl;

import ai.everylink.common.entity.Tenant;
import ai.everylink.openapi.plugin.chainscan.service.ChainscanService;
import ai.everylink.openapi.plugin.chainscan.service.HttpService;
import ai.everylink.openapi.plugin.chainscan.util.Convert;
import ai.everylink.openapi.plugin.chainscan.util.httpUtil.HttpHeader;
import ai.everylink.openapi.plugin.chainscan.util.httpUtil.HttpParamers;
import ai.everylink.openapi.plugin.chainscan.util.l2.L2Client;
import ai.everylink.openapi.plugin.chainscan.util.l2.L2Config;
import ai.everylink.openapi.plugin.chainscan.vo.BalanceVO;
import ai.everylink.openapi.plugin.chainscan.vo.ChainscanResult;
import ai.everylink.openapi.plugin.chainscan.vo.Layer2Token;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import io.zksync.domain.token.Token;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Brett
 * @Description
 * @Date 2021/9/29 15:46
 **/
@Slf4j
public class ChainscanServiceImpl implements ChainscanService {

    private L2Config l2Config;

    private static HashMap<String, String> l2Tokens;

    private static final String CHAIN_ID = "4";
    private static final String REFERER_URL = "http://rinkeby-test.powx.io/";
    private static final String TOKENS_URL = "http://xapi.powx.io";
    private static final String TOKENS_PATH = "/wallet/v1.0/networks/4/tokens/layer2";


    @Autowired
    private ApplicationContext applicationContext;


    private static final Map<Long, Map<Object, Token>> tokens = new HashMap();

    public ChainscanServiceImpl(L2Config l2Config) {
        this.l2Config = l2Config;
    }

    @Override
    public Object balance(ServerWebExchange exchange) {

        String address  = exchange.getRequest().getQueryParams().getFirst("address");
        // get all chain id
        List<Long> chainIds = l2Config.getAllChainIds();
        if (CollectionUtils.isEmpty(chainIds)) {
            return Maps.newHashMap();
        }
        Map<String, BigDecimal> totalBalances = Maps.newHashMap();
        try {
            for (Long chainId : chainIds) {
                Map<String, BigDecimal> balances = getCommittedBalances(chainId, address);
                balances.forEach((symbol, balance) -> {
                    if (totalBalances.containsKey(symbol)) {
                        totalBalances.put(symbol, totalBalances.get(symbol).add(balance));
                    } else {
                        totalBalances.put(symbol, balance);
                    }
                });
            }
        }catch (Exception e){
            log.error("获取2层账户信息异常!");
            e.printStackTrace();
            return new ChainscanResult("500",null,"leary2 error!");
        }

        HashMap<String, String> tokens = getTokenMap();
        ArrayList<BalanceVO> list = new ArrayList<>();
        for(String key:totalBalances.keySet()){
            BalanceVO balanceVO = new BalanceVO();
            balanceVO.setContractAddress(tokens.get(key));
            balanceVO.setBalance(String.format("%08X",totalBalances.get(key)));
            list.add(balanceVO);
        }
        //处理十进制转十六进制
        return new ChainscanResult("1",list,"OK");
    }

    private HashMap<String, String> getTokenMap() {
        if(l2Tokens != null){
            return l2Tokens;
        }
        String contInfo = "";
        HashMap<String, String> tokens = new HashMap<>();
        //获取各个symbol的合约地址信息:
        HttpHeader   header   = new HttpHeader();
        HttpParamers paramers = HttpParamers.httpGetParamers();
        paramers.addParam("chain_id", CHAIN_ID);
        header.addParam("Referer",REFERER_URL);
        HttpService httpService = new HttpService(TOKENS_URL);
        try {
            contInfo = httpService.service(TOKENS_PATH, paramers, header);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(StringUtils.isNotBlank(contInfo)){
            JSONObject jsonObject = JSONObject.parseObject(contInfo);
            List<Layer2Token> data = JSONArray.parseArray(jsonObject.get("data").toString(), Layer2Token.class);
            for (Layer2Token datum : data) {
                tokens.put(datum.getSymbol(),datum.getContractAddress());
            }
        }else {
            log.error("获取symbol合约地址信息失败!");
        }
        l2Tokens = tokens;
        return tokens;
    }

    private Map<String, BigDecimal> getCommittedBalances(Long chainId, String address) {

        JSONObject              accountInfo = L2Client.createJsonRpcProxy(l2Config,chainId).accountInfo(address);
        Map<String, BigDecimal> result      = Maps.newHashMap();
        if (accountInfo != null) {
            JSONObject committedBalances = accountInfo.getJSONObject("committed")
                    .getJSONObject("balances");
            committedBalances.forEach((symbol, balance) -> {
                result.put(symbol, Convert.fromWei(balance.toString(), getToken(symbol)));
            });
        }
        return result;
    }

    private Token getToken(String tokenIdentifier) {
        List<Long> chainIds = l2Config.getAllChainIds();
        for (Long chainId : chainIds) {
            try {
                Token token = getToken(l2Config,chainId, tokenIdentifier);
                if (token != null) {
                    return token;
                }
            } catch (Exception ex) {
                log.error("Get token error", ex);
            }
        }
        return null;
    }

    public Token getToken(L2Config l2Config,Long chainId, String tokenIdentifier) {
       // Long tenantId = ApplicationContextHelper.getTenant().get().getId();
        Tenant tenant = new Tenant();
        Long tenantId       = tenant.getId();
        if (tokens.containsKey(tenantId) && tokens.get(tenantId).containsKey(tokenIdentifier)) {
            return tokens.get(tenantId).get(tokenIdentifier);
        }
        Map<Object, Token> tokenMap = Maps.newHashMap();
        Token[] tokenArray = L2Client.createRestProxy(l2Config,chainId).tokens();
        for (Token token : tokenArray) {
            tokenMap.put(token.getSymbol(), token);
            tokenMap.put(token.getAddress(), token);
            tokenMap.put(token.getId().toString(), token);
        }
        tokens.put(tenantId, tokenMap);
        return tokenMap.get(tokenIdentifier);
    }


    @Override
    public Object txlist(ServerWebExchange exchange) {
        String address  = exchange.getRequest().getQueryParams().getFirst("address");
        String coinName  = exchange.getRequest().getQueryParams().getFirst("coin_name");

        List<Long> chainIds = l2Config.getAllChainIds();

        // 查询所有的历史记录
//        JSONArray transArray = L2Client.createRestProxy(l2Config,4L).transactionHistoryAll(address);
//
//        System.out.println(transArray);

        String result = "{\"result\":{\"total_number\":23,\"total_page\":0,\"transaction_history_list\":[{\"symbol\":\"ETH\",\"amount\":\"0\",\"address\":\"0x1c1860870a362c3b7ed3f278347c4f20b1ea4953\",\"network_name\":\"ETH\",\"memo\":\"\",\"txid\":\"sync-tx:cc191f184f0f954b426aeb4bfebecb9767c4c5f19e46173e83735419cbf80772\",\"gas_fee\":\"0.000094\",\"type\":\"ChangePubKey\",\"action_symbol\":\"-\",\"id\":530,\"time\":\"2021-10-21 17:29:14 UTC\",\"status\":\"Verified\"}]},\"staus\":\"200\",\"message\":\"success\"}\n";
        System.out.println(JSONObject.parse(result));
        return JSONObject.parse(result);




//
//        //分页
//        List<BaseTransactionHistoryVO> result = historyListVO.getTransactionHistoryList();
//        if (result.size() <= pageSize) {
//            historyListVO.setTransactionHistoryList(result);
//        } else {
//            int endSize = (pageNo + 1) * pageSize - 1;
//            if (endSize > result.size()) {
//                endSize = result.size();
//            }
//            historyListVO.setTransactionHistoryList(result.subList(pageNo * pageSize, endSize));
//        }
//
//        historyListVO.setTotalNumber(Long.valueOf(result.size()));

      // return historyListVO;
    }
}
