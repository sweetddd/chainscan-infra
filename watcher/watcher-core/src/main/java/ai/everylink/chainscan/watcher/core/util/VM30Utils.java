package ai.everylink.chainscan.watcher.core.util;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class VM30Utils {

    private BigInteger gasLimit = BigInteger.valueOf(9000000);

    private Credentials credentials;

    @SneakyThrows
    public VM30 getContranct(Web3j web3j, String contractAddress) {
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger  gasPrice    = ethGasPrice.getGasPrice().multiply(new BigInteger("105")).divide(new BigInteger("100"));
        //调用合约
        credentials = Credentials.create(contractAddress);
        ContractGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);
        VM30                contract    = VM30.load(contractAddress, web3j, credentials, gasProvider);
        return contract;
    }

    @SneakyThrows
    public BigInteger totalSupply(Web3j web3j, String contractAddress) {
        VM30       contract    = getContranct(web3j, contractAddress);
        BigInteger totalSupply = new BigInteger("0");
        try {
            totalSupply = contract.totalSupply().send();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("获取totalSupply失败:" + contractAddress);
        }
        return totalSupply;
    }

    @SneakyThrows
    public BigInteger totalLockAmount(Web3j web3j, String contractAddress) {
        VM30       contract    = getContranct(web3j, contractAddress);
        BigInteger totalSupply = new BigInteger("0");
        try {
            totalSupply = contract.totalLockAmount().send();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("获取totalLockAmount失败:" + contractAddress);
        }
        return totalSupply;
    }

    @SneakyThrows
    public BigInteger burnt(Web3j web3j, String contractAddress) {
        VM30       contract    = getContranct(web3j, contractAddress);
        BigInteger totalSupply = new BigInteger("0");
        try {
            totalSupply = contract.burnt().send();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("获取burnt失败:" + contractAddress);
        }
        return totalSupply;
    }

    /**
     * 查询合约的symbol
     *
     * @param web3j
     * @param contractAddress
     * @return
     */
    @SneakyThrows
    public Utf8String symbol(Web3j web3j, String contractAddress) {
        VM30       contract = getContranct(web3j, contractAddress);
        Utf8String symbol   = new Utf8String("");
        try {
            symbol = contract.symbol().send();
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message.indexOf("to expected type: Utf8String") >= 0) {
                message = message.replace(" to expected type: Utf8String", "");
                message = message.replace("Unable to convert response: ", "");
                symbol = new Utf8String(message);
            } else {
                log.error("获取symbol失败:" + contractAddress);
            }
        }
        return symbol;
    }

    /**
     * 查询合约的name
     *
     * @param web3j
     * @param contractAddress
     * @return
     */
    @SneakyThrows
    public Utf8String name(Web3j web3j, String contractAddress) {
        VM30       contract = getContranct(web3j, contractAddress);
        Utf8String name     = new Utf8String("");
        try {
            name = contract.name().send();
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message.indexOf("to expected type: Utf8String") >= 0) {
                message = message.replace(" to expected type: Utf8String", "");
                message = message.replace("Unable to convert response: ", "");
                name = new Utf8String(message);
            } else {
                log.error("获取name失败:" + contractAddress);
            }
        }
        return name;
    }

    /**
     * 查询合约的精度
     *
     * @param web3j
     * @param contractAddress
     * @return
     */
    @SneakyThrows
    public BigInteger decimals(Web3j web3j, String contractAddress) {
        VM30       contract = getContranct(web3j, contractAddress);
        Uint8      decimal  = new Uint8(0);
        BigInteger decimals = new BigInteger("0");
        try {
            decimal = contract.decimals().send();
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message.indexOf("to expected type: Uint8") >= 0) {
                message = message.replace(" to expected type: Uint8", "");
                message = message.replace("Unable to convert response: ", "");
                decimals = new BigInteger(message);
            } else {
                log.error("获取decimals失败:" + contractAddress);
            }


        }
        return decimals;
    }

    /**
     * 查询指定合约,某个地址的余额
     *
     * @param web3j
     * @param contractAddress
     * @param address
     * @return
     */
    @SneakyThrows
    public BigInteger balanceOf(Web3j web3j, String contractAddress, String address) {
        VM30       contract = getContranct(web3j, contractAddress);
        BigInteger balance  = new BigInteger("0");
        try {
            balance = contract.balanceOf(address).send();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("获取totalLockAmount失败:" + contractAddress);
        }
        return balance;
    }

    /**
     * 查询指定合约, 转账接口
     *
     * @param web3j
     * @param contractAddress
     * @param to
     * @param value
     * @return
     */
    @SneakyThrows
    public BigInteger transfer(Web3j web3j, String contractAddress, String to, BigInteger value) {
        VM30       contract = getContranct(web3j, contractAddress);
        BigInteger balance  = new BigInteger("0");
        try {
            TransactionReceipt send = contract.transfer(to, value).send();
            System.out.println(send);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("操作transfer失败contract:" + contractAddress + "to:Address" + to);
        }
        return balance;
    }

//    /**
//     * 校验合约 是否包含transfer 方法;
//     *
//     * @param web3j
//     * @param to
//     * @param val
//     * @param from
//     * @param contractAddress
//     * @return
//     * @throws Exception
//     */
//    public boolean querryTransfer(Web3j web3j, String to, BigInteger val, String from, String contractAddress) throws Exception {
//        List<Type> inputParameters = new ArrayList<>();
//        inputParameters.add(new Address(to));
//        inputParameters.add(new Uint256(val));
//        Function function = new Function("transfer",
//                                         inputParameters,
//                                         Collections.<TypeReference<?>>emptyList());
//        String encodedFunction = FunctionEncoder.encode(function);
//        org.web3j.protocol.core.methods.response.EthCall response = web3j.ethCall(
//                        Transaction.createEthCallTransaction(from, contractAddress, encodedFunction),
//                        DefaultBlockParameterName.LATEST)
//                .sendAsync().get();
//        if (response.getValue().equals("0x"))
//            return false;
//        return true;
//    }


    /**
     * 校验合约 是否包含指定方法;
     *
     * @param web3j
     * @param parameters
     * @param functionName
     * @param from
     * @param contract
     * @return
     * @throws Exception
     */
    public boolean querryFunction(Web3j web3j,List<Type> parameters,String functionName,String from,String contract){
        try {
            Function function = new Function(functionName,
                                             parameters,
                                             Collections.<TypeReference<?>>emptyList());
            String encodedFunction = FunctionEncoder.encode(function);
            org.web3j.protocol.core.methods.response.EthCall response = null;

                response = web3j.ethCall(
                                Transaction.createEthCallTransaction(from, contract, encodedFunction),
                                DefaultBlockParameterName.LATEST)
                        .sendAsync().get();
            Response.Error error = response.getError();
            if(error != null && StringUtils.isNotBlank(error.getMessage()) && error.getMessage().equals("VM Exception while processing transaction: revert") ){
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            e.getMessage();
            return false;
        }
        return true;
    }


}
