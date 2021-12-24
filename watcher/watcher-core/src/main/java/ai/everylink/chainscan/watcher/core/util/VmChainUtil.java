package ai.everylink.chainscan.watcher.core.util;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.util.Arrays;

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

    public  String getFinalizedHead() {
        String storage   = getStorage("", "chain_getFinalizedHead");
        if (StringUtils.isEmpty(storage)) {
            return null;
        }
        return storage;
    }

    /**
     * 请求rpc接口
     *
     * @param input
     * @param method
     * @return
     */
    public   String getStorage(String input, String method) {
        HttpService httpService = new HttpService(vmUrl, new OkHttpClient(), false);
        Request state_getStorage = null;
        log.info("getStorage.method:" + method);
        log.info("getStorage.input:" + input);
        if(StringUtils.isBlank(input)){
            state_getStorage = new Request<>(
                    method,
                    null,
                    httpService,
                    Response.class);
        }else {
            state_getStorage = new Request<>(
                    method,
                    Arrays.asList(input),
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

    public static void main(String[] args) {

        VmChainUtil vmChainUtil       = new VmChainUtil();
        vmChainUtil.vmUrl = "http://10.233.65.245:9934";
        String state_subscribeStorage = vmChainUtil.getStorage("0xaf9e78df124ddb9027c2573e5fb15e127322f546e497e413366c0e4faa8974c3", "state_subscribeStorage");
        System.out.println(state_subscribeStorage);
    }
}
