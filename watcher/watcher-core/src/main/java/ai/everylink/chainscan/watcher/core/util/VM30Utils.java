package ai.everylink.chainscan.watcher.core.util;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
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
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class VM30Utils {

    private final BigInteger gasLimit = BigInteger.valueOf(9000000);

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
        VM30       contract = getContract(web3j, contractAddress);
        Utf8String tokenURL   = new Utf8String("");
        try {
            tokenURL = contract.tokenURL(tokenId).send();
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message.indexOf("to expected type: Utf8String") >= 0) {
                message = message.replace(" to expected type: Utf8String", "");
                message = message.replace("Unable to convert response: ", "");
                tokenURL = new Utf8String(message);
            } else {
                log.debug("获取tokenURL失败:" + contractAddress);
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
            if(error != null && StringUtils.isNotBlank(error.getMessage()) && error.getMessage().equals("VM Exception while processing transaction: revert") ){
                return false;
            }
        } catch (Exception e) {
            e.getMessage();
            return false;
        }
        return true;
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
}
