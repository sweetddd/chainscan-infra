package ai.everylink.chainscan.watcher.plugin.utils;

import ai.everylink.chainscan.watcher.core.config.LendingContractConfig;
import ai.everylink.chainscan.watcher.core.config.PluginChainId;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;


@Component
@Service
public class LendingContractUtils {




    @Autowired
    Environment environment;


    private Map<String,String> lendingContractMap = new HashMap<>();
    private Map<String,String> stableContractMap = new HashMap<>();

    public  Set<String> topicList = new TreeSet<>();


    public  boolean containTopic(String topic){

        if(topicList.size() == 0){
            initTopic();
        }
        return topicList.contains(topic);
    }

    private void initTopic(){

        //Supply Rinkeby-4 存入 0.12 ETH
        topicList.add("0x4c209b5fc8ad50758f13e2e1088ba56a560dff690a1c6fef26394f4c03821c4f");

        //Withdraw Rinkeby-4 取出 0.21 ETH
        topicList.add("0xe5b754fb1abb7f01b499791d0b820ae3b6af3424ac1c59768edb53f4ec31a929");

        //Borrow Rinkeby-4 借款 1.2 USDT
        topicList.add("0x13ed6866d4e1ee6da46f845c46d7e54120883d75c5ea9a2dacc1c4ca8984ab80");

        //Repay Rinkeby-4 还款 1.2 USDT
        topicList.add("0x1a2a22cb034d26d1854bdc6666a5b91fe25efbbb5dcad3b0355478d6f5c362a1");


    }


    private void init(){
        String lendingConfig = SpringApplicationUtils.getBean(LendingContractConfig.class).getLendingConfig();
        String stableConfig = SpringApplicationUtils.getBean(LendingContractConfig.class).getStableConfig();

//        String property = environment.getProperty("watcher.lending.contract.config");
//        String lendingProperty ="[{\"address\":\"0x3F48560889A44fd746047dC5F70a87B0F747e7a3\",\"symbol\":\"ETH\"},{\"address\":\"0x9f4b5195e29437dc32b90e0dde822616b59cc175\",\"symbol\":\"USDT\"}]";
//        String stableProperty ="[{\"address\":\"0xb5bB6Ef764BA8fD8C7eEAf00764E2FA57b0419f7\",\"symbol\":\"rUSDT\"},{\"address\":\"0x2d9269E3E53b16001F3f20F286E8E5F5Cf7EE8f0\",\"symbol\":\"rUSDR\"}]";
        String lendingProperty =lendingConfig;
        String stableProperty =stableConfig;

        if(StringUtils.isEmpty(lendingProperty) ){
            //
            return;
        }

        List<JSONObject> jsonObjects = JSONArray.parseArray(lendingProperty, JSONObject.class);
        for(JSONObject obj : jsonObjects){
            String contractAddress = obj.getString("address").toLowerCase();
            String symbol = obj.getString("symbol");
            lendingContractMap.put(contractAddress,symbol);
        }

        if(StringUtils.isEmpty(stableProperty)){
            return;
        }
        List<JSONObject> stableJson = JSONArray.parseArray(stableProperty, JSONObject.class);
        for(JSONObject obj : stableJson){
            String contractAddress = obj.getString("address").toLowerCase();
            String symbol = obj.getString("symbol");
            stableContractMap.put(contractAddress,symbol);
        }
    }

    // stablecoin 返回symbol

    public String stableContains(String contract){
        if( stableContractMap.size() == 0 ){
            init();
        }
        return stableContractMap.get(contract.toLowerCase());
    }


    //lending 返回symbol
    public String containsContract(String contract){
        if( lendingContractMap.size() == 0 ){
            init();
        }
        return lendingContractMap.get(contract.toLowerCase());
    }
}
