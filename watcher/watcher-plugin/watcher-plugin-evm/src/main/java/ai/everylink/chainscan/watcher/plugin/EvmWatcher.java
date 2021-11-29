/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.everylink.chainscan.watcher.plugin;

import ai.everylink.chainscan.watcher.core.IEvmWatcherPlugin;
import ai.everylink.chainscan.watcher.core.IWatcher;
import ai.everylink.chainscan.watcher.core.IWatcherPlugin;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.plugin.config.EvmConfig;
import com.google.common.collect.Lists;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 以太坊扫块
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
public class EvmWatcher implements IWatcher {

    private static Logger logger = LoggerFactory.getLogger(EvmWatcher.class);

    /** 当前扫描高度。TODO 应该从数据库获取当前最新块高度 */
    private Long currentBlockHeight = 0L;

    /** 每次扫描步数. */
    private int step = 5;

    /** watcher当前扫块的链url */
    private String currentChainuRL;

    private Web3j web3j;

    @Override
    public List<EvmData> scanBlock() {
        long start = System.currentTimeMillis();

        initWeb3j();

        List<EvmData> blockList = Lists.newArrayList();

        Long networkBlockHeight = getNetworkBlockHeight();
        logger.info("loop scan begin.curNum={},netNum={}", currentBlockHeight, networkBlockHeight);
        if (networkBlockHeight <= 0) {
            return Lists.newArrayList();
        }

        long startBlockNumber = 0;
        try {
            if (currentBlockHeight < networkBlockHeight) {
                startBlockNumber = currentBlockHeight + 1;
                currentBlockHeight = (networkBlockHeight - currentBlockHeight > step)
                                    ? currentBlockHeight + step
                                    : networkBlockHeight;
                logger.info("Scan block from {} to {}", startBlockNumber, currentBlockHeight);

//                blockList = replayBlock(startBlockNumber, currentBlockHeight);
                blockList = replayBlock(9716550L, 9716553L);
                logger.info("Scan block from {} to {},resultSize={}", startBlockNumber, currentBlockHeight, blockList.size());
                if (CollectionUtils.isEmpty(blockList)) {
                    logger.info("扫块失败！！！");
                    currentBlockHeight = startBlockNumber - 1;
                    return Lists.newArrayList();
                }
            }
        } catch (Throwable e) {
            currentBlockHeight = startBlockNumber - 1;
            logger.error("loop scan error.curNum={"+currentBlockHeight+"},netNum={"+networkBlockHeight+"}", e);
        }

        logger.info("loop scan end.curNum={},netNum={},consume={}ms",
                currentBlockHeight, networkBlockHeight, (System.currentTimeMillis()-start));

        return blockList;
    }


    @Override
    public List<IWatcherPlugin> getOrderedPluginList() {
        // 自己创建的
        List<IWatcherPlugin> pluginList = Lists.newArrayList(new EvmPlugin());

        // 通过SPI发现的
        pluginList.addAll(findErc20WatcherPluginBySPI());

        // 排序
        Collections.sort(pluginList, (o1, o2) -> o2.ordered() - o1.ordered());

        return  pluginList;
    }

    @Override
    public List<String> listSupportedChain() {
        try {
            String url = SpringApplicationUtils.getBean(EvmConfig.class).getUrls();
            logger.info("{} supports chain list: {}", getClass().getSimpleName(), url);
            return Lists.newArrayList(url.split(","));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return Lists.newArrayList();
    }

    @Override
    public void setCurrentChain(String chain) {
        this.currentChainuRL = chain;
    }

    @Override
    public String getCron() {
//        return "*/5 * * * * ?";

        return "*/10 * * * * ?";
    }

    /**
     * 初始化web3j
     */
    private void initWeb3j() {
        if (web3j != null) {
            return ;
        }

        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            builder.writeTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            builder.readTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            OkHttpClient httpClient = builder.build();
            HttpService httpService = new HttpService(currentChainuRL, httpClient, false);

            if (SpringApplicationUtils.getBean(EvmConfig.class).getRinkebyUrl().equals(currentChainuRL)) {
                httpService.addHeader("Authorization", Credentials.basic("", SpringApplicationUtils.getBean(EvmConfig.class).getRinkebyRpcSecret()));
            }
            web3j = Web3j.build(httpService);
        } catch (Exception e) {
            logger.error("初始化web3j异常", e);
        }
    }

    /**
     * 获取最新区块高度
     *
     * @return 高度
     */
    private Long getNetworkBlockHeight() {
        try {
            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            return blockNumber.getBlockNumber().longValue();
        } catch (Throwable e) {
            logger.error("Error occured when request web3j.ethBlockNumber.", e);
            return 0L;
        }
    }

    public List<EvmData> replayBlock(Long startBlockNumber, Long endBlockNumber) throws Exception {
        List<EvmData> dataList = Lists.newArrayList();

        for (Long blockHeight = startBlockNumber; blockHeight <= endBlockNumber; blockHeight++) {
            logger.info("Begin to scan block={}", blockHeight);
//            try {
            EvmData data = new EvmData();

            // 查询block
            EthBlock block = web3j.ethGetBlockByNumber(
                    new DefaultBlockParameterNumber(blockHeight), true).send();
            data.setBlock(block.getBlock());
            dataList.add(data);
            if (CollectionUtils.isEmpty(block.getBlock().getTransactions())) {
                logger.info("No transactions found. block={}", blockHeight);
                continue;
            }

            // 交易列表
            logger.info("Found txs.block={},count={}", blockHeight, block.getBlock().getTransactions().size());
            for (EthBlock.TransactionResult transactionResult : block.getBlock().getTransactions()) {
                Transaction tx = ((EthBlock.TransactionObject) transactionResult).get();
                if (tx.getInput() == null || tx.getInput().length() < 138) {
                    logger.info("No logs.block={},tx={}", blockHeight, tx.getHash());
                    continue;
                }

                // 获取Logs
                EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(tx.getHash()).send();
                if (receipt.getResult() != null && receipt.getResult().getLogs() != null) {
                    logger.info("Found logs.block={},tx={},count={}",
                            blockHeight, tx.getHash(), receipt.getResult().getLogs().size());
                    data.getTransactionLogMap().put(tx.getHash(), receipt.getResult().getLogs());
                }
            }
//            } catch (Throwable e) {
//                logger.error("Error occured when scan block=" + blockHeight, e);
//            }
        }

        return dataList;
    }

    /**
     * 通过SPI机制发现所有三方开发的支持Erc20区块的plugin
     *
     * @return
     */
    private List<IEvmWatcherPlugin> findErc20WatcherPluginBySPI() {
        ServiceLoader<IEvmWatcherPlugin> list = ServiceLoader.load(IEvmWatcherPlugin.class);
        return list == null ? Lists.newArrayList() : Lists.newArrayList(list);
    }
}