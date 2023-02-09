package ai.everylink.chainscan.watcher.plugin.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ErcNftFactory {

    private static Map<String, ErcNftService> ERC_TOKEN_SERVICE_MAP;

    public static ErcNftService getInstance(ErcTypeNftEnum ercType){
        return ERC_TOKEN_SERVICE_MAP.get(ercType.getServiceName());
    }

    @Autowired
    public void setErcTokenServiceMap(Map<String, ErcNftService> ercNftServiceMap){
        ErcNftFactory.ERC_TOKEN_SERVICE_MAP = ercNftServiceMap;
    }

}
