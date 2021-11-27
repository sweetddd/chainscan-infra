package ai.everylink.chainscan.watcher.plugin;

import ai.everylink.chainscan.watcher.core.IErc20WatcherPlugin;
import ai.everylink.chainscan.watcher.core.IWatcher;
import ai.everylink.chainscan.watcher.core.IWatcherPlugin;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.plugin.config.VmSecret;
import com.google.common.collect.Lists;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * ERC20
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
public class Erc20Watcher implements IWatcher {

    private static Logger logger = LoggerFactory.getLogger(Erc20Watcher.class);

    private static Web3j web3j;

    private Long currentBlockHeight = 0L;
    //每次扫描步数
    private int step = 5;

    static {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(30 * 1000, TimeUnit.MILLISECONDS);
        builder.writeTimeout(30 * 1000, TimeUnit.MILLISECONDS);
        builder.readTimeout(30 * 1000, TimeUnit.MILLISECONDS);
        OkHttpClient httpClient  = builder.build();
        String       credential  = Credentials.basic("", SpringApplicationUtils.getBean(VmSecret.class).getRpcSecret());
        HttpService httpService = new HttpService(SpringApplicationUtils.getBean(VmSecret.class).getRpcApi(), httpClient, false);
        httpService.addHeader("Authorization", credential);
        web3j = Web3j.build(httpService);
    }

    @Override
    public List<Object> scanBlcok() {
        Long networkBlockNumber = getNetworkBlockHeight();
        logger.info("networkBlockNumber:{} currentBlockHeight:{}", networkBlockNumber, currentBlockHeight);

        long startBlockNumber = 0;
        try {
            if (currentBlockHeight < networkBlockNumber) {
                startBlockNumber = currentBlockHeight + 1;
                currentBlockHeight = (networkBlockNumber - currentBlockHeight > step) ? currentBlockHeight + step : networkBlockNumber;
                logger.info("replay block from {} to {}", startBlockNumber, currentBlockHeight);

                List<Object> blockList = replayBlock(startBlockNumber, currentBlockHeight);
                if (CollectionUtils.isEmpty(blockList)) {
                    logger.info("扫块失败！！！");
                    // 未扫描成功
                    currentBlockHeight = startBlockNumber - 1;
                    return Lists.newArrayList();
                }
            }
        } catch (Exception e) {
            currentBlockHeight = startBlockNumber - 1;
            e.printStackTrace();
        }

        return Lists.newArrayList("block1", "block2", "block3");
    }

    public List<Object> replayBlock(Long startBlockNumber, Long endBlockNumber) {
        for (Long blockHeight = startBlockNumber; blockHeight <= endBlockNumber; blockHeight++) {
            EthBlock block = null;
            try {
                logger.info("ethGetBlockByNumber {}", blockHeight);
                block = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(blockHeight), true).send();
                List<EthBlock.TransactionResult> transactionResults = block.getBlock().getTransactions();
                if (CollectionUtils.isEmpty(transactionResults)) {
                    continue;
                }
                logger.info("replayBlock: Height({}) - Transactions count({})", blockHeight, transactionResults.size());
                for (EthBlock.TransactionResult transactionResult : transactionResults) {
                    EthBlock.TransactionObject transactionObject = (EthBlock.TransactionObject) transactionResult;
                    Transaction transaction = transactionObject.get();
                    logger.info("Transaction Detail: Height({}) - {}", blockHeight, transaction.getHash());
                    String input = transaction.getInput();
                    if (StringUtils.isNotEmpty(input) && input.length() >= 138) {
                        // 获取Logs
                        EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(transaction.getHash()).send();
                        System.out.println(receipt);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                //why return null: I'm trying to make it scan again
                return null;
            }
        }
        return null;
    }


    private boolean checkLog(Long blockHeight, String contractAddress, String transactionHash) throws IOException {
        org.web3j.protocol.core.methods.request.EthFilter ethFilter = createFilter(blockHeight, contractAddress);
        Request<?, EthLog> ethLogRequest = web3j.ethGetLogs(ethFilter);
        EthLog ethLog = ethLogRequest.send();
        List<EthLog.LogResult> logResultList = ethLog.getLogs();
        if (CollectionUtils.isEmpty(logResultList)) {
            return false;
        }
        for (int i = 0; i < logResultList.size(); i++) {
            if (((EthLog.LogObject) logResultList.get(i).get()).getTransactionHash().equalsIgnoreCase(transactionHash)) {
                return true;
            }
        }
        return false;
    }

    private org.web3j.protocol.core.methods.request.EthFilter createFilter(Long blockHeight, String contractAddress) {
        org.web3j.protocol.core.methods.request.EthFilter filter = new org.web3j.protocol.core.methods.request.EthFilter(
                DefaultBlockParameter.valueOf(new BigInteger(String.valueOf(blockHeight))),
                DefaultBlockParameter.valueOf(new BigInteger(String.valueOf(blockHeight))),
                contractAddress);
        filter.addSingleTopic(EventEncoder.encode(createEvent()));
        return filter;
    }

    private Event createEvent() {
        Event event = new Event("Transfer", Arrays.asList(
                new TypeReference<Address>() {
                },
                new TypeReference<Address>() {
                },
                new TypeReference<Uint256>() {
                }));
        return event;
    }




    private Long getNetworkBlockHeight() {
        try {
            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            long networkBlockNumber = blockNumber.getBlockNumber().longValue();
            return networkBlockNumber;
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }


    @Override
    public String getCron() {
        return "*/5 * * * * ?";
    }

    @Override
    public List<IWatcherPlugin> getOrderedPluginList() {
        // 自己创建的
        List<IWatcherPlugin> pluginList = Lists.newArrayList(new Erc20Plugin());

        // 通过SPI发现的
        pluginList.addAll(findErc20WatcherPluginBySPI());

        // 排序
        Collections.sort(pluginList, new Comparator<IWatcherPlugin>() {
            @Override
            public int compare(IWatcherPlugin o1, IWatcherPlugin o2) {
                return o2.ordered() - o1.ordered();
            }
        });

        return  pluginList;
    }

    /**
     * 通过SPI机制发现所有三方开发的支持Erc20区块的plugin
     *
     * @return
     */
    private List<IErc20WatcherPlugin> findErc20WatcherPluginBySPI() {
        ServiceLoader<IErc20WatcherPlugin> list = ServiceLoader.load(IErc20WatcherPlugin.class);
        return list == null ? Lists.newArrayList() : Lists.newArrayList(list);
    }

}
