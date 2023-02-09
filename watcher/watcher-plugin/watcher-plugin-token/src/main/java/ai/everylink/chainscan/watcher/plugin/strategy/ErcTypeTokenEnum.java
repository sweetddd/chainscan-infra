package ai.everylink.chainscan.watcher.plugin.strategy;

import lombok.Getter;

@Getter
public enum ErcTypeTokenEnum {

    ERC1155("ercToken1155ServiceImpl"),
    DEFAULT("ercTokenDefaultServiceImpl"),
    ;

    private final String serviceName;

    ErcTypeTokenEnum(String serviceName){
        this.serviceName = serviceName;
    }

}
