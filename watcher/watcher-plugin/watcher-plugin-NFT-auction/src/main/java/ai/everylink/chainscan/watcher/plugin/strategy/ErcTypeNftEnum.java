package ai.everylink.chainscan.watcher.plugin.strategy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Getter
public enum ErcTypeNftEnum {

    ERC1155("ercNft1155ServiceImpl", List.of("0x6863adcf"), List.of(""), List.of("0xeb47b241")),
    DEFAULT("ercNftDefaultServiceImpl", List.of("0x8fa4a10f"), List.of("0x848e5c77", "0xc24d5a5c"), List.of("0xebea6025")),
    ;

    private final String serviceName;
    private final List<String> createAuctionMethods;
    private final List<String> finishAuctionMethods;
    private final List<String> cancelAuctionMethods;

    ErcTypeNftEnum(String serviceName, List<String> createAuctionMethods, List<String> finishAuctionMethods, List<String> cancelAuctionMethods){
        this.serviceName = serviceName;
        this.createAuctionMethods = createAuctionMethods;
        this.finishAuctionMethods = finishAuctionMethods;
        this.cancelAuctionMethods = cancelAuctionMethods;
    }

    public static ErcTypeNftEnum isCreateAuctionMethod(String method){
        ErcTypeNftEnum[] values = values();
        for (ErcTypeNftEnum typeEnum : values) {
            if(typeEnum.createAuctionMethods.contains(method.toLowerCase())){
                return typeEnum;
            }
        }
        return null;
    }

    public static ErcTypeNftEnum isFinishAuctionMethod(String method){
        ErcTypeNftEnum[] values = values();
        for (ErcTypeNftEnum typeEnum : values) {
            if(typeEnum.finishAuctionMethods.contains(method.toLowerCase())){
                return typeEnum;
            }
        }
        return null;
    }

    public static ErcTypeNftEnum isCancelAuctionMethod(String method){
        ErcTypeNftEnum[] values = values();
        for (ErcTypeNftEnum typeEnum : values) {
            if(typeEnum.cancelAuctionMethods.contains(method.toLowerCase())){
                return typeEnum;
            }
        }
        return null;
    }

    public static Method getMethod(String method){
        ErcTypeNftEnum createAuctionMethod = isCreateAuctionMethod(method);
        if (createAuctionMethod != null) {
            return new Method(true, false, false, createAuctionMethod == ERC1155);
        }
        ErcTypeNftEnum finishAuctionMethod = isFinishAuctionMethod(method);
        if (finishAuctionMethod != null) {
            return new Method(false, true, false, finishAuctionMethod == ERC1155);
        }
        ErcTypeNftEnum cancelAuctionMethod = isCancelAuctionMethod(method);
        if (cancelAuctionMethod != null) {
            return new Method(false, false, true, cancelAuctionMethod == ERC1155);
        }
        return null;
    }

    @Data
    @AllArgsConstructor
    public static class Method {
        private boolean isCreate;
        private boolean isFinish;
        private boolean isCancel;
        private boolean isErc1155;
    }

}
