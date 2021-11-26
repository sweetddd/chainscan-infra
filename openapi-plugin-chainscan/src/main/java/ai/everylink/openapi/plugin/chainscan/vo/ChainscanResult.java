package ai.everylink.openapi.plugin.chainscan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Brett
 * @Description
 * @Date 2021/10/28 17:44
 **/
@Data
@AllArgsConstructor
public class ChainscanResult implements Serializable {

    private static final long serialVersionUID = -3783316657677671171L;

    /**
     * status
     */
    private String status;

    /**
     *result
     */
    private Object result;

    /**
     *message
     */
    private String message;
}
