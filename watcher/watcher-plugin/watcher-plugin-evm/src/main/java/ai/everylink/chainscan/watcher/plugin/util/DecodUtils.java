package ai.everylink.chainscan.watcher.plugin.util;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.TypeDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static javax.swing.UIManager.put;

/**
 * @Author brett
 * @Description 解码工具类；
 * @Date 2021/12/1 12:06 上午
 **/
public class DecodUtils {


    private static Map functionMap = new HashMap() {{
        put("0x40c10f19", "Function: mint(address _who, uint256 _value) ***");
        put("0x095ea7b3", "Function: approve(address _spender, uint256 _value) ***");
        put("0x05e2ca17", "Function: deposit(uint8 destinationChainID, bytes32 resourceID, bytes data) ***");
        put("0x8c0c2631", "Function: adminSetBurnable(address handlerAddress, address tokenAddress) ***");
        put("0xd3fc9864", "Function: mint(address _to, uint256 _value, string symbol) ***");
        put("0x2f2ff15d", "Function: grantRole(bytes32 role, address account) ***");
        put("0xa9059cbb", "Function: transfer(address _to, uint256 _value) ***");
        put("0xcb10f215", "Function: adminSetResource(address handlerAddress, bytes32 resourceID, address tokenAddress) ***");
        put("0x42966c68", "Function: burn(uint256 _value) ***");
        put("0x6a761202", "Function: execTransaction(address to, uint256 value, bytes data, uint8 operation, uint256 safeTxGas, uint256 dataGas, uint256 gasPrice, address gasToken, address refundReceiver, bytes signatures) ***");
        put("0xfa31de01", "Function: dispatch(uint32 _destinationDomain, bytes32 _recipientAddress, bytes _messageBody) ***");
        put("0xb31c01fb", "Function: update(bytes32 _committedRoot, bytes32 _newRoot, bytes _signature) ***");
        //拍卖合约
        put("0xeedcff91", "Function: function mintingMultiple(uint176 activityId, address to, uint256 number) ***");
        put("0xb62c77c4", "Function setAlphabetAddr(address alphabetAddr) ***");
        put("0xb52b0b09", "function creatActivity(string memory activityname,address recipientAddr,bool auctionState,address payTokenAddr,uint256 price,string memory symbol,uint8 unit,uint256 total) ***");
    }};

    /**
     * 获取Function
     *
     * @param input
     * @return
     */
    public static Object getFunction(String input) {
        return functionMap.get(input.substring(0, 10));
    }

    ;

    /**
     * 解码params
     *
     * @param input
     * @return
     */
    public static String getParams(String input) {
        String                  methodID  = input.substring(0, 10);
        String                        sub       = input.substring(10, input.length());
        LinkedHashMap<String, Object> paramsMap = new LinkedHashMap<>();
        paramsMap.put("MethodID", methodID);
        int start = 0;
        for (int i = 1; i <= sub.length() / 64; i++) {
            paramsMap.put("[" + i + "]", sub.substring(start, start + 64));
            start = start + 64;
        }
        Gson gson = new Gson();
        return gson.toJson(paramsMap);
    }


    /**
     * 解析地址信息
     *
     * @param data
     * @return
     */
    public static String decodAddress(String data) {
        String decodAddress = "";
        Method refMethod;
        try {
            refMethod = TypeDecoder.class.getDeclaredMethod("decode", String.class, int.class, Class.class);
            refMethod.setAccessible(true);
            Address address = (Address) refMethod.invoke(null, data, 0, Address.class);
            if (StringUtils.isNotBlank(address.toString())) {
                decodAddress = address.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decodAddress;
    }

    /**
     * 解析账户数字
     *
     * @param data
     * @return
     */
    public static BigInteger decodAmount(String data) {
        BigInteger deAmount = new BigInteger("0");
        Method     refMethod;
        try {
            refMethod = TypeDecoder.class.getDeclaredMethod("decode", String.class, int.class, Class.class);
            refMethod.setAccessible(true);
            Uint256 amount = (Uint256) refMethod.invoke(null, data, 0, Uint256.class);
            deAmount = amount.getValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deAmount;
    }


    public static void main(String[] args) {
        try {
            String inputData = "0xcb10f215000000000000000000000000c2aea54c49a59ca56cd654ff36a29ff8c0d906fc000000000000000000000000000003521ebe4a02bbc34786d860b355f5a5ce0000000000000000000000000061e9ceecb6e162457e9416ffb692c116446256dd";
            String params    = getParams(inputData);
            System.out.println(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
