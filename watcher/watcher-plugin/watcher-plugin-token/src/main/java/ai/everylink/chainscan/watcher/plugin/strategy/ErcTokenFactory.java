package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.core.enums.ErcTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ErcTokenFactory {

    private static Map<String, ErcTokenService> ERC_TOKEN_SERVICE_MAP;

    public static ErcTokenService getInstance(ErcTypeEnum ercType){
        return ERC_TOKEN_SERVICE_MAP.get(ercType.getServiceName());
    }

    @Autowired
    public void setErcTokenServiceMap(Map<String, ErcTokenService> ercTokenServiceMap){
        ErcTokenFactory.ERC_TOKEN_SERVICE_MAP = ercTokenServiceMap;
    }

}
