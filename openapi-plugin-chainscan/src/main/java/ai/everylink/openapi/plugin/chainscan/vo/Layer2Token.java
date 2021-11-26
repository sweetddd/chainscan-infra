package ai.everylink.openapi.plugin.chainscan.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Brett
 * @Description
 * @Date 2021/10/27 14:33
 **/
@Data
public class Layer2Token  implements Serializable {
    private static final long serialVersionUID = 6921535550313780198L;


    private String symbol;

    @JsonProperty("contract_address")
    private String contractAddress;

}
