package ai.everylink.chainscan.watcher.plugin;

import ai.everylink.chainscan.watcher.core.util.OkHttpUtil;
import okhttp3.OkHttpClient;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.http.HttpService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Main
 *
 * @author: david.zhanghui@everylink.ai
 */
public class Main {
    private static Logger logger = LoggerFactory.getLogger(Main.class);
    private static String rpcUrl = "https://rinkeby.infura.io/v3/b9769dd0ae344706b1cebca95e52e887";
    public static void main(String[] args) throws Exception {

        OkHttpClient httpClient = OkHttpUtil.buildOkHttpClient();
        HttpService httpService = new HttpService(rpcUrl, httpClient, false);
        Web3j web3j = Web3j.build(httpService);
        EthBlock block = web3j.ethGetBlockByNumber(
                new DefaultBlockParameterNumber(1719600L), true).send();

        String txHash = "0x0adde1ce5805d0a0980938ea4072a54670441db367bff705201c39ec4da1b854";
        Optional<Transaction> tx = web3j.ethGetTransactionByHash(txHash).send().getTransaction();
        EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send();
        System.out.println(block.getBlock());
    }
}
