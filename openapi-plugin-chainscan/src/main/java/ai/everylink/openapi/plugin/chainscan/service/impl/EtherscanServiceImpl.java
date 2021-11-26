package ai.everylink.openapi.plugin.chainscan.service.impl;

import ai.everylink.openapi.plugin.chainscan.service.ChainscanService;
import ai.everylink.openapi.plugin.chainscan.service.HttpService;
import ai.everylink.openapi.plugin.chainscan.util.httpUtil.HttpHeader;
import ai.everylink.openapi.plugin.chainscan.util.httpUtil.HttpParamers;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

/**
 * @Author Brett
 * @Description
 * @Date 2021/9/29 15:46
 **/
@Slf4j
public class EtherscanServiceImpl implements ChainscanService {

    private static final String ETHERSCAN_URL  = "https://api.etherscan.io";
    private static final String ETHERSCAN_PATH = "/api";

    private long TIMEOUT = 10000;

    private int RETRY = 3;

    @Override
    public Object balance(ServerWebExchange exchange) {
        String result   = "";
        HttpHeader   header   = new HttpHeader();
        HttpParamers paramers = HttpParamers.httpGetParamers();

        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
        for (String key : queryParams.keySet()) {
            paramers.addParam(key, exchange.getRequest().getQueryParams().getFirst(key));
        }
        log.info("etherscan:balance");
        log.info("etherscan:balance:paramers:"+paramers.toString());

        paramers.addParam("apikey", exchange.getRequest().getQueryParams().getFirst("apikey"));

        HttpService httpService = new HttpService(ETHERSCAN_URL);
        try {
            result = httpService.service(ETHERSCAN_PATH, paramers, header);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return JSONObject.parseObject(result);
    }


    @Override
    public Object txlist(ServerWebExchange exchange) {
        String result = "";
        HttpHeader   header   = new HttpHeader();
        HttpParamers paramers = HttpParamers.httpGetParamers();

        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
        for (String key : queryParams.keySet()) {
            paramers.addParam(key, exchange.getRequest().getQueryParams().getFirst(key));
        }
        log.info("etherscan:txlist");
        log.info("etherscan:txlist:paramers:" + paramers.toString());

        //String address = exchange.getRequest().getQueryParams().getFirst("address");
        paramers.addParam("apikey", exchange.getRequest().getQueryParams().getFirst("apikey"));

        HttpService httpService = new HttpService(ETHERSCAN_URL);
        try {
            result = httpService.service(ETHERSCAN_PATH, paramers, header);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return JSONObject.parseObject(result);
    }



    public static void main(String[] args) {
//        String response = "";
//        HttpHeader          header      = new HttpHeader();
//        HttpParamers paramers = HttpParamers.httpGetParamers();
//        paramers.addParam("module", "account");
//        paramers.addParam("action", "balance");
//        paramers.addParam("address", "0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae");
//        paramers.addParam("tag", "latest");
//        paramers.addParam("apikey", "RAWVEBW2RS6EEETNQ83ASVE972E415G5C3");
//
//        HttpService httpService = new HttpService(ETHERSCAN_URL);
//        try {
//            response = httpService.service(ETHERSCAN_PATH, paramers, header);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        System.out.println(response);

        String result = "{\n" +
                "    \"status\": \"1\",\n" +
                "    \"message\": \"OK\",\n" +
                "    \"result\": \"374868436783144397866641\"\n" +
                "}";
        JSONObject jsonObject = JSONObject.parseObject(result);
        Object     status     = jsonObject.get("status");
        if ("1".equals(status)) {
            System.out.println(jsonObject.get("result"));
        }
    }

}

















