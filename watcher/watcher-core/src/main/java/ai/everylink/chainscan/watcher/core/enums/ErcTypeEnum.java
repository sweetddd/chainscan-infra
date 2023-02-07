package ai.everylink.chainscan.watcher.core.enums;

import lombok.Getter;

@Getter
public enum ErcTypeEnum {

    ERC1155("ercToken1155ServiceImpl"),
    DEFAULT("ercTokenDefaultServiceImpl"),
    ;

    private String serviceName;

    ErcTypeEnum(String serviceName){
        this.serviceName = serviceName;
    }

}
