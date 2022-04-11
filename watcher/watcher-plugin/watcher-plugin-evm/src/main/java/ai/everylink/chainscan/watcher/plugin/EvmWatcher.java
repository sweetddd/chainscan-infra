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
import ai.everylink.chainscan.watcher.core.util.OkHttpUtil;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.core.util.VmChainUtil;
import ai.everylink.chainscan.watcher.core.util.WatcherUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.plugin.rocketmq.SlackUtils;
import ai.everylink.chainscan.watcher.plugin.service.EvmDataService;
import ai.everylink.chainscan.watcher.plugin.service.EvmScanDataService;
import ai.everylink.chainscan.watcher.plugin.util.Utils;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.http.HttpService;

import java.util.*;
import java.util.concurrent.*;

/**
 * 以太坊扫块
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
@Component
public class EvmWatcher implements IWatcher {

    private static final Logger logger = LoggerFactory.getLogger(EvmWatcher.class);

    private VmChainUtil vmChainUtil;

    /**
     * 每次扫块最大扫块步数
     */
    private int step;

    /**
     * 每次查询的limit
     */
    private int processStep;

    /**
     * 当前扫块的链的id
     */
    private static int chainId;

    private static Web3j web3j;

    /**
     * 从数据库里面获取处理进度
     */
    private EvmDataService evmDataService;

    private EvmScanDataService evmScanDataService;

    /**
     * 区块生产超时时间
     */
    private static final Long BLOCK_PRODUCE_TIMEOUT = 11*60*1000L;

    private static final int MAX_SCAN_THREAD = 200;
    private static final int MAX_TX_SCAN_THREAD = 250;

    /**
     * 任务线程池。
     * 一个线程用来扫块，落库。
     * 另外一个线程用来查库，组装EvmData对象并交付plugin处理
     */
    private static final ExecutorService taskPool = new ThreadPoolExecutor(2, 2, 6, TimeUnit.HOURS, new ArrayBlockingQueue<>(1000));

    /**
     * 扫块时并发查询区块线程池。
     */
    private static final ThreadPoolExecutor scanBlockPool = new ThreadPoolExecutor(200, MAX_SCAN_THREAD, 30, TimeUnit.MINUTES, new ArrayBlockingQueue<>(1000), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            logger.error("scanBlockPool queue is full");
            printThreadPoolStat();
        }
    });

    /**
     * 扫块时并发查询区块下的交易线程池。
     */
    private static final ThreadPoolExecutor scanTxPool = new ThreadPoolExecutor(250, MAX_TX_SCAN_THREAD, 2, TimeUnit.HOURS, new ArrayBlockingQueue<>(500000), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            logger.error("scanTxPool queue is full");
            printThreadPoolStat();
        }
    });

    /**
     * 一次插入数据库记录数
     */
    private static int BATCH_INSERT_MAX_SIZE = 30;

    /**
     * 入口方法
     *
     * @return
     */
    @Override
    public List<EvmData> scanBlock() {
        init();

        List<EvmData> dataList = new CopyOnWriteArrayList<EvmData>();

        CountDownLatch latch = new CountDownLatch(2);
        taskPool.submit(new Utils.ScanChainThread(latch, this));
        taskPool.submit(new Utils.ListBlockThread(latch, this, dataList));

        try {
            latch.await(3, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.error("[EvmWatcher]scanBlock error", e);
        }

        return dataList;
    }


    @Override
    public List<IWatcherPlugin> getOrderedPluginList() {
        // 自己创建的
        List<IWatcherPlugin> pluginList = Lists.newArrayList(new EvmPlugin());

        // 通过SPI发现的
        pluginList.addAll(findErc20WatcherPluginBySPI());

        // 排序
        Collections.sort(pluginList, (o1, o2) -> o2.ordered() - o1.ordered());

        return pluginList;
    }

    /**
     * 获取最后区块状态更新;
     */
    @Override
    public void finalizedBlockStatus() {
        //获取最新确认hash;
        String finalizedHash = vmChainUtil.getFinalizedHead();
        evmDataService.updateBlockByHash(finalizedHash);
    }


    @Override
    public String getCron() {
        return "*/5 * * * * ?";
    }

    public List<EvmData> listBlock() {
        Long dbHeight = evmDataService.getMaxBlockNum(chainId);
        List<EvmData> list = evmScanDataService.queryBlockList(dbHeight, processStep);
        logger.info("[EvmWatcher]listBlock.dbHeight={},processStep={},list={}", dbHeight, processStep, list.size());
        return list;
    }

    public void scanChain() {
        if (WatcherUtils.isScanStop()) {
            logger.info("[EvmWatcher]scan stopped.");
            return;
        }

        // 获取数据库保存的扫块高度
        Long dbHeight = evmScanDataService.queryMaxBlockNumber();

        // 获取链上高度
        Long chainHeight = getNetworkBlockHeight();
        if (dbHeight.equals(chainHeight)) {
            logger.info("[EvmWatcher]dbHeight catch the chain height.");
            return;
        }

        if (dbHeight > chainHeight) {
            logger.info("[EvmWatcher]DB块高超过链上块高，maybe the chain was reset.");
            return;
        }

        // 计算扫块区间
        long start = dbHeight + 1;
        long end = chainHeight;
        if (chainHeight - start >= step) {
            end = start + step - 1;
        }
        logger.info("[EvmWatcher]begin to scan.dbHeight={},chainHeight={},start={},end={}", dbHeight, chainHeight, start, end);

        // 并发扫块
        List<EvmData> dataList = currentReplayBlock(start, end);

        // 校验数据
        if (CollectionUtils.isEmpty(dataList)) {
            logger.error("[EvmWatcher]data is empty.expected: {} blocks", (end - start + 1));
            return;
        }

        if (dataList.size() != (end - start + 1)) {
            logger.error("[EvmWatcher]Scan block size {} mismatch expect size {}", dataList.size(), (end - start + 1));
            return;
        }

        // 落库
        try {
            long t1 = System.currentTimeMillis();
            if (dataList.size() <= BATCH_INSERT_MAX_SIZE) {
                evmScanDataService.insert(dataList);
            } else {
                // 分批插入，防止爆仓
                int cnt = dataList.size() / BATCH_INSERT_MAX_SIZE;
                if (dataList.size() % BATCH_INSERT_MAX_SIZE != 0) {
                    cnt = cnt+1;
                }

                for (int i = 0; i < cnt; i++) {
                    int startIdx = i * BATCH_INSERT_MAX_SIZE;
                    int endIdx = startIdx + BATCH_INSERT_MAX_SIZE;
                    if (endIdx > dataList.size()) {
                        endIdx = dataList.size();
                    }
                    evmScanDataService.insert(dataList.subList(startIdx, endIdx));
                }
            }
            logger.info("[EvmWatcher]insert scanned blocks.consume={}ms", (System.currentTimeMillis() - t1));
        } catch (Exception e) {
            logger.error("[EvmWatcher]insert db failed.", e);
        }
    }

    private List<EvmData> currentReplayBlock(long start, long end) {
        List<EvmData> list = new CopyOnWriteArrayList<EvmData>();

        try {
            CountDownLatch latch = new CountDownLatch((int) (end - start + 1));

            for (long blockNum = start; blockNum <= end; blockNum++) {
                scanBlockPool.submit(new ReplayBlockThread(latch, blockNum, list));
            }

            latch.await(3, TimeUnit.MINUTES);

            if (CollectionUtils.isEmpty(list)) {
                logger.info("[EvmWatcher]replay block failied. start=" + start + ", end=" + end);
                sendVmAlertMsgToSlack();
                return Lists.newArrayList();
            }
        } catch (Exception e) {
            logger.error("[EvmWatcher]error when currentReplayBlock.", e);
            return Lists.newArrayList();
        }

        Collections.sort(list, new Comparator<EvmData>() {
            @Override
            public int compare(EvmData o1, EvmData o2) {
                return o1.getBlock().getNumber().compareTo(o2.getBlock().getNumber());
            }
        });

        logger.info("[EvmWatcher]end replay blocks.start={},end={},size={}", start, end, list.size());
        return list;
    }

    public static class ReplayBlockThread implements Runnable {
        private final CountDownLatch latch;
        private final Long           blockNum;
        private final List<EvmData>  list;
        private ReplayBlockThread (CountDownLatch latch, Long blockNum, List<EvmData> list) {
            this.latch = latch;
            this.blockNum = blockNum;
            this.list = list;
        }

        @Override
        public void run() {
            try {
                EvmData data = replayBlock(blockNum);
                if (data == null) {
                    logger.error("[EvmWatcher]fetched empty block:" + blockNum);
                    return;
                }

                // 并发查询交易列表
                if (!CollectionUtils.isEmpty(data.getBlock().getTransactions())) {
                    CountDownLatch txLatch = new CountDownLatch(data.getBlock().getTransactions().size());
                    for (EthBlock.TransactionResult transactionResult : data.getBlock().getTransactions()) {
                        Transaction tx = ((EthBlock.TransactionObject) transactionResult).get();
                        scanTxPool.submit(new ReplayTransactionThread(txLatch, data, tx.getHash()));
                    }
                    txLatch.await(3, TimeUnit.MINUTES);

                    // 校验
                    if (data.getBlock().getTransactions().size() != data.getTxList().size()) {
                        logger.warn("[EvmWatcher]Scan tx size {} mismatch expect size {}. blockNum={}",
                                data.getTxList().size(), data.getBlock().getTransactions().size(), blockNum);
//                        return;
                    }
                }

                list.add(data);
            } catch (Throwable e) {
                logger.error("[EvmWatcher]error when process block:" + blockNum, e);
            } finally {
                latch.countDown();
            }
        }
    }

    public static class ReplayTransactionThread implements Runnable {
        private final CountDownLatch latch;
        private final EvmData        data;
        private final String         txHash;

        public ReplayTransactionThread(CountDownLatch latch, EvmData data, String txHash) {
            this.latch = latch;
            this.data = data;
            this.txHash = txHash;
        }

        @Override
        public void run() {
            try {
                replayTx(data, txHash);
            } catch (Exception e) {
                logger.error("[EvmWatcher]error when process tx. blockNum="
                        + data.getBlock().getNumber().longValue() + ", txHash=" + txHash, e);
            } finally {
                latch.countDown();
            }
        }
    }

    private static EvmData replayBlock(Long blockNumber) throws Exception {
        EvmData data = new EvmData();
        data.setChainId(chainId);

        // 查询block
        EthBlock block = web3j.ethGetBlockByNumber(
                new DefaultBlockParameterNumber(blockNumber), true).send();
        if (block == null || block.getBlock() == null) {
            logger.error("[EvmWatcher]Block is null. block={}", blockNumber);
            return null;
        }

        data.setBlock(block.getBlock());

        return data;
    }

    private static void replayTx(EvmData data, String txHash) throws Exception {
        long blockNumber = data.getBlock().getNumber().longValue();

        // 获取receipt
        EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send();
        if (receipt.getResult() == null) {
            logger.warn("[EvmWatcher]tx receipt not found. blockNum={}, tx={}", blockNumber, txHash);
            return ;
        }

        data.getTxList().put(txHash, receipt.getResult());

        // 获取Logs
        if (!CollectionUtils.isEmpty(receipt.getResult().getLogs())) {
            logger.info("[EvmWatcher]Found logs.block={},tx={},count={}",
                    blockNumber, txHash, receipt.getResult().getLogs().size());
            data.getTransactionLogMap().put(txHash, receipt.getResult().getLogs());
        }
    }

    private void init() {
        logger.info("[EvmWatcher]timeZone={}", Calendar.getInstance().getTimeZone());
        initWeb3j();
        initService();
        step = WatcherUtils.getScanStep();
        processStep = WatcherUtils.getProcessStep();
        chainId = WatcherUtils.getChainId();
        BATCH_INSERT_MAX_SIZE = WatcherUtils.getBatchInsertSize();
        logger.info("[EvmWatcher]init config. step={},processStep={},chainId={},batchInsertSize={},rpcUrl={}, chainType={},db={}",
                    step, processStep, chainId, BATCH_INSERT_MAX_SIZE, WatcherUtils.getVmChainUrl(), WatcherUtils.getChainType(), System.getenv("spring.datasource.chainscan.jdbc-url"));
        logger.info("[EvmWatcher]got rocketmq name srv addr:{}", SlackUtils.getNamesrvAddr());
    }




    private void initService() {
        if (evmDataService == null) {
            evmDataService = SpringApplicationUtils.getBean(EvmDataService.class);
        }
        if (vmChainUtil == null) {
            vmChainUtil = SpringApplicationUtils.getBean(VmChainUtil.class);
        }

        if (evmScanDataService == null) {
            evmScanDataService = SpringApplicationUtils.getBean(EvmScanDataService.class);
        }

    }

    /**
     * 初始化web3j
     */
    private void initWeb3j() {
        if (web3j != null) {
            return;
        }

        try {
            String rpcUrl = WatcherUtils.getVmChainUrl();
            logger.info("[rpc_url]url=" + rpcUrl);

            OkHttpClient httpClient = OkHttpUtil.buildOkHttpClient();
            HttpService httpService = new HttpService(rpcUrl, httpClient, false);
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

    /**
     * 通过SPI机制发现所有三方开发的支持Erc20区块的plugin
     *
     * @return
     */
    private List<IEvmWatcherPlugin> findErc20WatcherPluginBySPI() {
        ServiceLoader<IEvmWatcherPlugin> list = ServiceLoader.load(IEvmWatcherPlugin.class);
        return list == null ? Lists.newArrayList() : Lists.newArrayList(list);
    }

    private static void printThreadPoolStat() {
        logger.info("[monitor]scanBlockPool.{},{},{},{},{},{},{};{},{},{}",
                scanBlockPool.getCorePoolSize(),
                scanBlockPool.getMaximumPoolSize(),
                scanBlockPool.getLargestPoolSize(),
                scanBlockPool.getTaskCount(),
                scanBlockPool.getActiveCount(),
                scanBlockPool.getCompletedTaskCount(),
                scanBlockPool.getPoolSize(),
                scanBlockPool.getQueue().size(),
                scanBlockPool.isShutdown(),
                scanBlockPool.isTerminated()
        );

        logger.info("[monitor]scanTxPool.{},{},{},{},{},{},{};{},{},{}",
                scanTxPool.getCorePoolSize(),
                scanTxPool.getMaximumPoolSize(),
                scanTxPool.getLargestPoolSize(),
                scanTxPool.getTaskCount(),
                scanTxPool.getActiveCount(),
                scanTxPool.getCompletedTaskCount(),
                scanTxPool.getPoolSize(),
                scanTxPool.getQueue().size(),
                scanTxPool.isShutdown(),
                scanTxPool.isTerminated()
        );
    }


    private static final RateLimiter slackNotifiyLimiter = RateLimiter.create(0.001);
    private void sendVmAlertMsgToSlack() {
        // slack notification limiter
        if (!slackNotifiyLimiter.tryAcquire()) {
            return;
        }
        Date lastBlockCreateTime = evmDataService.getMaxBlockCreationTime(chainId);
        if (lastBlockCreateTime == null) {
            return;
        }

        long diff = System.currentTimeMillis() - lastBlockCreateTime.getTime();
        if (diff < BLOCK_PRODUCE_TIMEOUT) {
            return;
        }

        SlackUtils.sendSlackNotify("C02SQNUGEAU", "DTX链告警",
                "VM链长时间未出块，请关注！最后出块于(\"" + diff/1000/60 + "\")分钟前");
    }
}
