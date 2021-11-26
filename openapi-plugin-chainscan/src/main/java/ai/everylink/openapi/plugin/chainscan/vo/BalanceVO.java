package ai.everylink.openapi.plugin.chainscan.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Brett
 * @Description
 * @Date 2021/10/7 22:34
 **/
@Data
public class BalanceVO implements Serializable {

    private static final long serialVersionUID = -3783316657677071171L;

    @JsonProperty("contract_address")
    private String contractAddress;

    private String balance;

}
