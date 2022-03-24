package ai.everylink.chainscan.watcher.core.util;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.TypeDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.*;

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
        put("0xa44f5fe6", "Function: deposit(uint32 destinationChainID, bytes32 resourceID, bytes calldata data) ***");
        put("0xee1c1c7b", "Function: depositETH(uint32 destinationChainID, bytes32 resourceID, bytes calldata data) ***");
        put("0xfe4464a7", "Function: depositNFT(uint32 destinationChainID, bytes32 resourceID, bytes calldata data) ***");
        //执行提按
        put("0x20e82d03", "Function: executeProposal(uint32 chainID,uint64 depositNonce,bytes calldata data,bytes32 resourceID,ProposalStatus proposalStatus,bytes32 proposalDataHash,bytes32 proposalResourceId,address[] memory yesVoteList,bytes[] memory relayerSignList) ***");
        put("0x860b533d", "Function: finishProposal(uint32 chainID, uint64 depositNonce, bytes32 dataHash) ***");
        put("0x699c0f17", "Function: cancelProposal(uint32 chainID, uint64 depositNonce, bytes32 dataHash) ***");
        put("0xd4ee6f32", "Function: voteProposal(uint32 chainID, uint64 depositNonce, bytes32 resourceID, bytes32 dataHash, bytes calldata relaySign) ***");
        put("0x780cf004", "Function: adminWithdraw(address handlerAddress,address tokenAddress,address recipient,uint256 amountOrTokenID) ***");
        put("0x8c0c2631", "Function: adminSetBurnable(address handlerAddress, address tokenAddress) ***");
        put("0x320b9006", "Function: adminRemoveOperator(address operatorAddress) ***");
        put("0x9d82dd63", "Function: adminRemoveRelayer(address relayerAddress) ***");
        put("0xcdb0f73a", "Function: adminAddRelayer(address relayerAddress) ***");
        put("0x4e056005", "Function: adminChangeRelayerThreshold(uint256 newThreshold) ***");
        put("0x5e1fab0f", "Function: renounceAdmin(address newAdmin) ***");
        put("0x5a1ad87c", "Function: adminSetGenericResource(address handlerAddress,bytes32 resourceID,address contractAddress,bytes4 depositFunctionSig,uint256 depositFunctionDepositerOffset,bytes4 executeFunctionSig) ***");
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
     * @param input
     * @return
     */
    public static  Object getFunction(String input){
        return functionMap.get(input.substring(0, 10));
    };



    /**
     * 解码params
     * @param input
     * @return
     */
    public static String getParams(String input) {
        String methodID = input.substring(0, 10);
        String sub = input.substring(10, input.length());
        LinkedHashMap<String, String> paramsMap = new LinkedHashMap<>();
        paramsMap.put("MethodID",methodID);
        int start = 0;
        for(int i = 1; i  <= sub.length()/64; i ++) {
            paramsMap.put("["+i+"]",sub.substring(start,start+64));
            start = start+64;
        }
        Gson gson = new Gson();
        return gson.toJson(paramsMap);
    }


    public static List<String> getParams2List(String input) {
        ArrayList<String>       list   = new ArrayList<>();
        String methodID = input.substring(0, 10);
        String sub = input.substring(10, input.length());
        list.add(methodID);
        int start = 0;
        for(int i = 1; i  <= sub.length()/64; i ++) {
            list.add(sub.substring(start,start+64));
            start = start+64;
        }
        return list;
    }

    /**
     * 解析地址信息
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
            if(StringUtils.isNotBlank(address.toString())){
                decodAddress = address.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decodAddress;
    }

    /**
     * 解析账户数字
     * @param data
     * @return
     */
    public static BigInteger decodAmount(String data) {
        BigInteger deAmount = new BigInteger("0");
        Method refMethod;
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
            String inputData = "0xd4ee6f320000000000000000000000000000000000000000000000000000000000000061000000000000000000000000000000000000000000000000000000000000000f000000000000000000000000000003542ebe4a02bbc34786d860b355f5a5ce005e196fad0db4ed72d7a78d4acbf6584d14cbca7053c4b556976b7d773d5f9ef700000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000000000000000000000000000000000000000004131756e84aac09ae9e74cbd146cfdc32057bd4dd118f77d396c65efcd89c3a46a17e52911531b965ddba2ad66ad2df98bf65d3e16c95f67296f80691d0d6cce680100000000000000000000000000000000000000000000000000000000000000";
            String params    = getFunction(inputData).toString();
            //List<String> params2List = getParams2List(inputData);
            System.out.println(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
