package java.ai.everylink.chainscan.watcher.plugin;

import ai.everylink.chainscan.watcher.plugin.util.DecodUtils;
import org.junit.Test;
import org.web3j.abi.TypeDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * @Author apple
 * @Description
 * @Date 2021/12/1 3:30 下午
 **/
public class DecodeUtileTest {

    @Test
    public void decod() throws Exception {
        String input = "0xa9059cbb00000000000000000000000076caea3316e7905cf59f5aaccc10927f06ad8ef1000000000000000000000000000000000000000000000000000000ba43b74000";
        String add = "00000000000000000000000076caea3316e7905cf59f5aaccc10927f06ad8ef1";
        String amount = "00000000000000000000000000000000000000000000000012f5dc3926dbd000";
        String params = DecodUtils.getParams(input);
        Object function = DecodUtils.getFunction(input);
        System.out.println(params);
        System.out.println(function);
        System.out.println(DecodUtils.decodAddress(add));
        System.out.println(DecodUtils.decodAmount(amount));
    }

    @Test
    public void typeDecoder() throws IOException {
        String inputData = "0xa9059cbb0000000000000000000000008e7e315fd8965b0fadc7c404307a55d5a6ccf15500000000000000000000000000000000000000000000000012f5dc3926dbd000";
        String method = inputData.substring(0, 10);
        System.out.println(method);
        String to    = inputData.substring(10, 74);
        String value = inputData.substring(74);
        System.out.println(value);
        Method refMethod;
        try {
            refMethod = TypeDecoder.class.getDeclaredMethod("decode", String.class, int.class, Class.class);
            refMethod.setAccessible(true);
            Address address = (Address) refMethod.invoke(null, to, 0, Address.class);
            System.out.println(address.toString());
            Uint256 amount = (Uint256) refMethod.invoke(null, value, 0, Uint256.class);
            System.out.println(amount.getValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
