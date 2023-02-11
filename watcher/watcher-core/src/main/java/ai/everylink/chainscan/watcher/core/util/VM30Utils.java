package ai.everylink.chainscan.watcher.core.util;


import ai.everylink.chainscan.watcher.core.vo.EvmData;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VM30Utils {

    private final BigInteger gasLimit = BigInteger.valueOf(9000000);
    @Autowired
    Environment environment;
    public final static Integer GLOBAL_RETRY_COUNT = 10;
    public final static Long GLOBAL_RETRY_SLEEP_MILL = 50L;

    @SneakyThrows
    public VM30 getContract(Web3j web3j, String contractAddress) {
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice().multiply(new BigInteger("105")).divide(new BigInteger("100"));
        //调用合约
        Credentials credentials = Credentials.create(contractAddress);
        ContractGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);
        return VM30.load(contractAddress, web3j, credentials, gasProvider);
    }


    @SneakyThrows
    public VM30 createContract(Web3j web3j, String contractAddress, String secret) {
        Credentials credentials = Credentials.create(secret);
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice();
        ContractGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);
        return VM30.load(contractAddress, web3j, credentials, gasProvider);
    }

    @SneakyThrows
    public BigInteger totalSupply(Web3j web3j, String contractAddress) {
        VM30       contract    = getContract(web3j, contractAddress);
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
        VM30       contract    = getContract(web3j, contractAddress);
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
        VM30       contract    = getContract(web3j, contractAddress);
        BigInteger totalSupply = new BigInteger("0");
        try {
            totalSupply = contract.burnt().send();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.debug("获取burnt失败:" + contractAddress);
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
        VM30       contract = getContract(web3j, contractAddress);
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
                log.debug("获取symbol失败:" + contractAddress);
            }
        }
        return symbol;
    }

    /**
     * 查询721合约的tokenURL
     *
     * @param web3j
     * @param contractAddress
     * @return
     */
    @SneakyThrows
    public Utf8String tokenURL(Web3j web3j, String contractAddress,BigInteger tokenId) {
        Utf8String tokenURL   = new Utf8String("");
        try {
            VM30 contract = getContract(web3j, contractAddress);
            tokenURL = contract.tokenURL(tokenId).send();
        } catch (Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            log.error("message error:{}", message);
            if (message.indexOf("to expected type: Utf8String") >= 0) {
                message = message.replace(" to expected type: Utf8String", "");
                message = message.replace("Unable to convert response: ", "");
                tokenURL = new Utf8String(message);
            } else {
                log.error("获取tokenURL失败:" + contractAddress);
            }
        }
        return tokenURL;
    }

    /**
     * 查询1155合约的tokenURL
     *
     * @param web3j
     * @param contractAddress
     * @return
     */
    @SneakyThrows
    public String URI(Web3j web3j, String contractAddress,BigInteger tokenId) {
        String tokenURL = "";
        try {
            VM30 contract = getContract(web3j, contractAddress);
            tokenURL = contract.URI(tokenId).send();
        } catch (Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            log.error("message error:{}", message);
            if (message.indexOf("to expected type: Utf8String") >= 0) {
                message = message.replace(" to expected type: Utf8String", "");
                message = message.replace("Unable to convert response: ", "");
                tokenURL = message;
            } else {
                log.error("获取uri失败:" + contractAddress);
            }
        }
        return tokenURL;
    }

    @SneakyThrows
    public String underlying(Web3j web3j, String contractAddress) {
        VM30       contract = getContract(web3j, contractAddress);
        String address   = "";
        try {
            address = contract.underlying().send().toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            if (message.indexOf("to expected type: Utf8String") >= 0) {
                message = message.replace(" to expected type: Utf8String", "");
                message = message.replace("Unable to convert response: ", "");
                address = "";
            } else {
                log.debug("获取underlying失败:" + contractAddress);
            }
        }
        return address;
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
        VM30       contract = getContract(web3j, contractAddress);
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
                log.debug("获取name失败:" + contractAddress);
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
        VM30       contract = getContract(web3j, contractAddress);
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
                log.debug("获取decimals失败:" + contractAddress);
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
        BigInteger result = BigInteger.ZERO;
        try {
            VM30 contract = getContract(web3j, contractAddress);
            result = contract.balanceOf(address).send();
            log.info("Address [{}] sync balance from contract [{}] success, balance [{}]", address, contractAddress, result);
        } catch (Exception ex) {
            String message = ex.getMessage();
            if(!message.equals("Contract Call has been reverted by the EVM with the reason: 'VM Exception while processing transaction: revert'.")){
                ex.printStackTrace();
            }
            log.error("Address [{}] sync balance from contract [{}] error: [{}]", address, contractAddress, ex.getMessage());
        }
        return result;
    }

    @SneakyThrows
    public BigInteger balanceOfErc1155(Web3j web3j, String contractAddress, String address, Long tokenId, int retryCount) {
        BigInteger result = BigInteger.ZERO;
        try {
            VM30 contract = getContract(web3j, contractAddress);
            result = contract.balanceOfErc1155(address, tokenId).send();
            log.info("Address [{}] sync balance from contract [{}] success, balance [{}]", address, contractAddress, result);
        } catch (Exception e) {
            retryCount = retryCount + 1;
            log.error("balanceOfErc1155.获取 balanceOf 重试次数：{}，异常：{}", retryCount, ExceptionUtils.getStackTrace(e));
            if(retryCount < GLOBAL_RETRY_COUNT){
                TimeUnit.MILLISECONDS.sleep(GLOBAL_RETRY_SLEEP_MILL);
                return balanceOfErc1155(web3j, contractAddress, address, tokenId, retryCount);
            }
        }
        return result;
    }

    /*@SneakyThrows
    public Object balanceOfBatch(Web3j web3j, String contractAddress, List<String> address, List<Long> tokenId) {
        Object result = BigInteger.ZERO;
        try {
            VM30 contract = getContract(web3j, contractAddress);
            result = contract.balanceOfBatch(address, tokenId).send();
            log.info("Address [{}] sync balance from contract [{}] success, balance [{}]", address, contractAddress, result);
        } catch (Exception ex) {
            String message = ex.getMessage();
            if(!message.equals("Contract Call has been reverted by the EVM with the reason: 'VM Exception while processing transaction: revert'.")){
                ex.printStackTrace();
            }
            log.error("Address [{}] sync balance from contract [{}] error: [{}]", address, contractAddress, ex.getMessage());
        }
        return result;
    }*/

    @SneakyThrows
    public BigInteger balanceOf(Web3j web3j, String contractAddress, String address, String secret) {
        BigInteger balance = BigInteger.ZERO;
        try {
            VM30 contract = createContract(web3j, contractAddress, secret);
            balance = contract.balanceOf(address).send();
        } catch (Exception ex) {
            String message = ex.getMessage();
            if(!message.equals("Contract Call has been reverted by the EVM with the reason: 'VM Exception while processing transaction: revert'.")){
                ex.printStackTrace();
            }
            log.warn("获取balanceOf失败:" + contractAddress);
        }
        return balance;
    }

    /**
     * 查询ERC721 合约的所有NFT的tokenId
     * @param web3j
     * @param contractAddress
     * @param address
     * @return
     */
    @SneakyThrows
    public BigInteger tokenOfOwnerByIndex(Web3j web3j, String contractAddress, String address,int index) {
        VM30       contract = getContract(web3j, contractAddress);
        BigInteger tokenId  = new BigInteger("0");
        try {
            tokenId = contract.tokenOfOwnerByIndex(address,index).send();
        } catch (Exception ex) {
          //  ex.printStackTrace();
            String message = ex.getMessage();
            if(message.contains("org.web3j.tx.exceptions.ContractCallException")){
                return new BigInteger("-1");
            }
            //org.web3j.tx.exceptions.ContractCallException: Contract Call has been reverted by the EVM with the reason: 'invalid opcode: INVALID'.

        }
        return tokenId;
    }

    /**
     * 更具tokenId 查询NFT的元数据
     * @param web3j
     * @param tokenId
     * @return
     */
    @SneakyThrows
    public String tokenURI(Web3j web3j, String contractAddress, BigInteger tokenId) {
        VM30       contract = getContract(web3j, contractAddress);
        String tokenURI  = "";
        try {
            tokenURI= contract.tokenURL(tokenId).send().toString();
        } catch (Exception ex) {
           // ex.printStackTrace();
            log.debug("获取tokenURI失败:" + contractAddress);
        }
        return tokenURI;
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
    public TransactionReceipt transfer(Web3j web3j, String contractAddress, String to, BigInteger value) {
        VM30       contract = getContract(web3j, contractAddress);
        TransactionReceipt transactionReceipt = new TransactionReceipt();
        try {
            transactionReceipt = contract.transfer(to, value).send();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("操作transfer失败contract:" + contractAddress + "to:Address" + to);
        }
        return transactionReceipt;
    }


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
                                             Collections.emptyList());
            String encodedFunction = FunctionEncoder.encode(function);
            org.web3j.protocol.core.methods.response.EthCall response = null;

                response = web3j.ethCall(
                                Transaction.createEthCallTransaction(from, contract, encodedFunction),
                                DefaultBlockParameterName.LATEST)
                        .sendAsync().get();
            Response.Error error = response.getError();
            if("0x".equals(response.getValue()) && null == error){
                return false;
            }
            if(error != null && StringUtils.isNotBlank(error.getMessage())
                    && (error.getMessage().equals("VM Exception while processing transaction: revert")
                        || error.getMessage().equals("execution reverted"))
                        ){
                return false;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    public boolean isErc1155(Web3j web3j, String fromAddr, String nftContractAddress){
        return this.querryFunction(web3j, Lists.newArrayList(new Uint256(1)), "uri", fromAddr, nftContractAddress);
    }

    public static void main(String[] args) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(30 * 1000, TimeUnit.MILLISECONDS);
        builder.writeTimeout(30 * 1000, TimeUnit.MILLISECONDS);
        builder.readTimeout(30 * 1000, TimeUnit.MILLISECONDS);
        OkHttpClient httpClient  = builder.build();
        HttpService httpService = new HttpService("http://goerli.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf/", httpClient, false);
        Web3j web3j = Web3j.build(httpService);

        List<Type> parames = new ArrayList<>();

        parames.add(new Uint256(0));
        VM30Utils vm30Utils = new VM30Utils();
        boolean tokenURI = vm30Utils.querryFunction(web3j, parames, "tokenURI1", "0xA2D479F43E2992ACC5eDbE95C2B8B6702888C1AF", "0x11c9edc2543c7adb3f38606eb57260cabe42b3f5");
        System.out.println(tokenURI);

    }


    public BigInteger distributionReserve(Web3j web3j, String contractAddress) {
        VM30       contract = getContract(web3j, contractAddress);
        BigInteger distributionReserve  = new BigInteger("0");
        try {
            distributionReserve = contract.distributionReserve().send();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("操作distributionReserve失败contract:" + contractAddress );
        }
        return distributionReserve;
    }

    public boolean isTransferContract(String transactionHash, String contractAddress){
        log.info("转账合约判断，txHash：{}, 合约地址：{}", transactionHash, contractAddress);
        if(StrUtil.isBlank(contractAddress)){
            return false;
        }
        List<String> monitorContractAddress = WatcherUtils.getConfigValues(environment, WatcherUtils.TRANSFER_CONTRACT_ADDRESS);
        if(CollUtil.isEmpty(monitorContractAddress)){
            return true;
        }
        return monitorContractAddress.stream().map(String::toLowerCase).collect(Collectors.toList()).contains(contractAddress.toLowerCase());
    }

    /**
     * 根据txHash查询交易明细（收据）
     */
    public void replayTxJudge(EvmData data, String transactionHash, Web3j web3j){
        String selectTransactionLog = WatcherUtils.getConfigValue(environment, WatcherUtils.MONITOR_SELECT_TRANSACTION_LOG, String.class);
        log.info("监听资产变化.selectTransactionLog:{}", selectTransactionLog);
        if(Boolean.parseBoolean(selectTransactionLog) && MapUtil.isEmpty(data.getTxList())){
            log.info("监听资产变化.transactionHash:{}, txList为空，进行查询", transactionHash);
            this.replayTx(web3j, data, transactionHash, 0);
        }
    }

    @SneakyThrows
    public void replayTx(Web3j web3j, EvmData data, String txHash, int retryCount) {
        long blockNumber = data.getBlock().getNumber().longValue();

        // 获取receipt
        try {
            log.info("VM30Utils.replayTx.start.txHash:{}", txHash);
            //指定一个交易哈希，返回一个交易的收据。需要指出的是，处于pending状态的交易，收据是不可用的。
            EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send();
            if (receipt.getResult() == null) {
                log.info("VM30Utils.[EvmWatcher]tx receipt not found. blockNum={}, tx={}", blockNumber, txHash);
                return ;
            }
            log.info("VM30Utils.replayTx.end.txHash:{}，receipt.getResult():{}", txHash, receipt.getResult().toString());
            //txList
            data.getTxList().put(txHash, receipt.getResult());
            // 获取Logs
            if (!CollectionUtils.isEmpty(receipt.getResult().getLogs())) {
                //LogMap
                data.getTransactionLogMap().put(txHash, receipt.getResult().getLogs());
            }
            log.info("VM30Utils.replayTx.txHash:{}，data.getTransactionLogMap():{}", txHash, data.getTransactionLogMap().size());
        } catch (Exception e) {
            retryCount = retryCount + 1;
            log.error("VM30Utils.获取 Transaction Receipt 重试次数：{}，异常：{}", retryCount, ExceptionUtils.getStackTrace(e));
            if(retryCount < GLOBAL_RETRY_COUNT){
                TimeUnit.MILLISECONDS.sleep(GLOBAL_RETRY_SLEEP_MILL);
                replayTx(web3j, data, txHash, retryCount);
            }
        }
    }



}
