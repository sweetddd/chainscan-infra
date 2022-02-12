package ai.everylink.chainscan.watcher.core.util;

import lombok.SneakyThrows;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * VM30 合约调用工具类
 */
public class VM30 extends Contract {
    private static final String BINARY = null;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<>();
    }

    protected VM30(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
        super(BINARY, contractAddress, web3j, credentials, gasProvider);
    }

    protected VM30(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider gasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, gasProvider);
    }

    public RemoteCall<BigInteger> price() {
        Function function = new Function("price",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<TransactionReceipt> mint(String _senders, BigInteger _values) {
        Function function = new Function(
                "_mint",
                Arrays.<Type>asList(new Address(_senders),
//                        new Uint256(
//                                _values),
                        new Uint256(
                                _values)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }


    public RemoteCall<TransactionReceipt> approve(String _spender, Long _value) {
        Function function = new Function(
                "approve",
                Arrays.<Type>asList(new Address(_spender),
                        new Uint256(_value)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<BigInteger> totalSupply() {
        Function function = new Function("totalSupply",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<BigInteger> totalLockAmount() {
        Function function = new Function("totalLockAmount",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<BigInteger> distributionReserve() {
        Function function = new Function("un_distribution_reserve",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<BigInteger> burnt() {
        Function function = new Function("burnt",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<Utf8String> name() {
        Function function = new Function("name",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Utf8String.class);
    }

    public RemoteCall<Uint8> decimals() {
        Function function = new Function("decimals",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Uint8.class);
    }

    public RemoteCall<Utf8String> symbol() {
        Function function = new Function("symbol",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Utf8String.class);
    }

    public RemoteCall<Utf8String> tokenURL(BigInteger tokenId) {
        Function function = new Function("tokenURL",
                Arrays.<Type>asList(
                        new Uint256(1)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Utf8String.class);
    }


    public RemoteCall<TransactionReceipt> transferFrom(String _from, String _to, BigInteger _value) {
        Function function = new Function(
                "transferFrom",
                Arrays.<Type>asList(new Address(_from),
                        new Address(_to),
                        new Uint256(_value)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }
    public RemoteCall<TransactionReceipt> deposit( Long chainId, String resourceId,String callData) {
        Function function = new Function(
                "deposit",
                Arrays.<Type>asList(
                        new Uint8(chainId),
                        new Bytes32(Numeric.hexStringToByteArray(resourceId.substring(2))),
                        new DynamicBytes(Numeric.hexStringToByteArray(callData.substring(2)))),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> init( String name, String symbol, Long decimal,BigInteger amount,String address) {
        Function function = new Function(
                "init",
                Arrays.<Type>asList(new Utf8String(name),
                        new Utf8String(symbol),
                        new Uint8(decimal)
                        //new Uint256(amount),
                        //new Address(address)
                        ),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }));
        return executeRemoteCallTransaction(function);
    }


    public Bytes32 toBytes32(String data){
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] bytes1 = new byte[32];
        System.arraycopy(bytes,0,bytes1,0,bytes.length);
        return new Bytes32(bytes1);
    }

    public RemoteCall<BigInteger> balanceOf(String _owner) {
        Function function = new Function("balanceOf",
                Arrays.<Type>asList(new Address(_owner)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    /**
     * 获取ERC-20 token指定地址余额
     *
     * @param address         查询地址
     * @param contractAddress 合约地址
     * @return
     * @throws InterruptedException
     */
    @SneakyThrows
    public String getERC20Balance(String address, String contractAddress){
        Function function = new Function("balanceOf",
                                         Arrays.asList(new Address(address)),
                                         Arrays.asList(new TypeReference<Address>() {
                                         }));

        String      encode             = FunctionEncoder.encode(function);
        Transaction ethCallTransaction = Transaction.createEthCallTransaction(address, contractAddress, encode);
        EthCall     ethCall            = web3j.ethCall(ethCallTransaction, DefaultBlockParameterName.LATEST).sendAsync().get();
        return ethCall.getResult();
    }


    public RemoteCall<TransactionReceipt> transfer(String _to, BigInteger _value) {
        Function function = new Function(
                "transfer",
                Arrays.<Type>asList(new Address(_to),
                        new Uint256(_value)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }




    public RemoteCall<TransactionReceipt> rigidPayment(BigInteger amount) {
        Function function = new Function(
                "rigidPayment",
                Arrays.<Type>asList(
                        new Uint256(amount)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }
    public RemoteCall<TransactionReceipt> convert(BigInteger amount) {
        Function function = new Function(
                "convert",
                Arrays.<Type>asList(
                        new Uint256(amount)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<BigInteger> allowance(String _owner, String _spender) {
        Function function = new Function("allowance",
                Arrays.<Type>asList(new Address(_owner),
                        new Address(_spender)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public static RemoteCall<VM30> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(VM30.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<VM30> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(VM30.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static VM30 load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
        return new VM30(contractAddress, web3j, credentials, gasProvider);
    }

    public static VM30 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider gasProvider) {
        return new VM30(contractAddress, web3j, transactionManager, gasProvider);
    }

    @Override
    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

}
