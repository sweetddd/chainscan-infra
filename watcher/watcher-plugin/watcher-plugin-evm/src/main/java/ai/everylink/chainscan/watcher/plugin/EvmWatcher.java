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
import ai.everylink.chainscan.watcher.core.util.*;
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

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private EvmDataService evmDataService;

    private EvmScanDataService evmScanDataService;

    /**
     * 任务线程池。
     * 一个线程用来扫块，落库。
     * 另外一个线程用来查库，组装EvmData对象并交付plugin处理
     */
    private static final ExecutorService taskPool = new ThreadPoolExecutor(2, 2, 6, TimeUnit.HOURS, new ArrayBlockingQueue<>(1000));

    /**
     * 扫块时并发查询区块线程池。
     */
    private static final ThreadPoolExecutor scanBlockPool = new ThreadPoolExecutor(300, 400, 30, TimeUnit.MINUTES, new ArrayBlockingQueue<>(1000));

    /**
     * 扫块时并发查询区块下的交易线程池。
     */
    private static final ThreadPoolExecutor scanTxPool = new ThreadPoolExecutor(250, 250, 2, TimeUnit.HOURS, new ArrayBlockingQueue<>(500000));

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

        if (chainId == 4) {
            logger.info("Rinkeby scan optimize");
            transactionFix();
            logger.info("Rinkeby scan optimize end");
            return Lists.newArrayList();
//            return rinkebyScanSpecial();
        }

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
        if (chainId != 4) {
            logger.info("finalizedBlockStatus executed");
            String finalizedHash = vmChainUtil.getFinalizedHead();
            evmDataService.updateBlockByHash(finalizedHash);
        }
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

    private List<EvmData> rinkebyScanSpecial() {
        List<EvmData> defaultList = Lists.newArrayList();
        if (WatcherUtils.isScanStop()) {
            logger.info("[rinkebyScan]scan stopped.");
            return defaultList;
        }

        // 获取数据库保存的扫块高度
        Long dbHeight = evmDataService.getMaxBlockNum(chainId);

        // 获取链上高度
        Long chainHeight = getNetworkBlockHeight();
        if (dbHeight.equals(chainHeight)) {
            logger.info("[rinkebyScan]dbHeight catch the chain height.");
            return defaultList;
        }

        if (dbHeight > chainHeight) {
            logger.info("[rinkebyScan]DB块高超过链上块高，maybe the chain was reset.");
            return defaultList;
        }

        // 计算扫块区间
        long start = dbHeight + 1;
        long end = chainHeight;
        if (chainHeight - start >= step) {
            end = start + step - 1;
        }
        logger.info("[rinkebyScan]begin to scan.dbHeight={},chainHeight={},start={},end={}", dbHeight, chainHeight, start, end);

        long t1 = System.currentTimeMillis();

        // 并发扫块
        List<EvmData> dataList = currentReplayBlock(start, end);

        // 校验数据
        if (CollectionUtils.isEmpty(dataList)) {
            logger.error("[EvmWatcher]data is empty.expected: {} blocks", (end - start + 1));
            return defaultList;
        }

        logger.info("[rinkebyScan]end to scan.size={},consume={}ms", dataList.size(), (System.currentTimeMillis() - t1));
        return dataList;
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
                if (chainId != 4
                        || ( chainId == 4 && data.getBlock().getNumber().longValue() > 10000000L)) {
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

    /**
     * 通过SPI机制发现所有三方开发的支持Erc20区块的plugin
     *
     * @return
     */
    private List<IEvmWatcherPlugin> findErc20WatcherPluginBySPI() {
        ServiceLoader<IEvmWatcherPlugin> list = ServiceLoader.load(IEvmWatcherPlugin.class);
        return list == null ? Lists.newArrayList() : Lists.newArrayList(list);
    }

    private void init() {
        initService();
        initWeb3j();
        initMonitor();
        step = WatcherUtils.getScanStep();
        processStep = WatcherUtils.getProcessStep();
        chainId = WatcherUtils.getChainId();
        BATCH_INSERT_MAX_SIZE = WatcherUtils.getBatchInsertSize();
        logger.info("[EvmWatcher]config info. step={},processStep={},chainId={},batchInsertSize={},chainType={}",
                    step, processStep, chainId, BATCH_INSERT_MAX_SIZE, WatcherUtils.getChainType());
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

    private void initMonitor() {
        new MonitorThread(evmDataService).start();
    }

    private void initWeb3j() {
        if (web3j != null) {
            return;
        }

        try {
            String rpcUrl = WatcherUtils.getVmChainUrl();

            OkHttpClient httpClient = OkHttpUtil.buildOkHttpClient();
            HttpService httpService = new HttpService(rpcUrl, httpClient, false);
            web3j = Web3j.build(httpService);
        } catch (Exception e) {
            logger.error("初始化web3j异常", e);
        }
    }

    private Long getNetworkBlockHeight() {
        try {
            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            return blockNumber.getBlockNumber().longValue();
        } catch (Throwable e) {
            logger.error("Error occured when request web3j.ethBlockNumber.", e);
            return 0L;
        }
    }

    private static AtomicBoolean monitorInit = new AtomicBoolean(false);
    public static class MonitorThread extends Thread {
        private EvmDataService evmDataService;

        public MonitorThread(EvmDataService evmDataService) {
            this.evmDataService = evmDataService;
        }

        @Override
        public void run() {
            if (!monitorInit.compareAndSet(false, true)) {
                return;
            }

            while (true) {
                try {
                    Thread.sleep(WatcherUtils.getWatcherMonitorIntervalSecs() * 1000);
                } catch (Exception e) {
                }

                try {
                    web3j.ethBlockNumber().send();
                } catch (Throwable e) {
                    SlackUtils.sendSlackNotify("C02SQNUGEAU", "DTX链告警", "VM链连接出错: " + WatcherUtils.getVmChainUrl());
                    continue;
                }

                try {
                    Date lastBlockCreateTime = evmDataService.getMaxBlockCreationTime(chainId);
                    logger.info("[MonitorThread]last block time:{}", lastBlockCreateTime);
                    if (lastBlockCreateTime == null) {
                        continue;
                    }

                    long diff = System.currentTimeMillis() - lastBlockCreateTime.getTime();
                    if (diff < 60 * 1000) {
                        continue;
                    }

                    SlackUtils.sendSlackNotify("C02SQNUGEAU", "DTX链告警",
                            "VM链长时间未出块，请关注！最后出块于(\"" + diff / 1000 / 60 + "\")分钟前");
                } catch (Exception e) {
                    logger.error("[MonitorThread]error:{}", e.getMessage());
                }
            }
        }
    }


    /**
     * rinkeby transaction表数据修复
     */
    private static AtomicBoolean init = new AtomicBoolean(false);
    private static void transactionFix() {
        if (chainId != 4) {
            logger.info("[watcher_fix]not rinkeby");
            return;
        }
        if (!init.compareAndSet(false, true)) {
            logger.info("[watcher_fix]already init");
            return;
        }

        long max = 68600000;
        long step = WatcherUtils.getWatcherFixTxBatchSize();
        while (true) {
            long start = getMaxTid();
            if (start <= 0) {
                logger.error("[watcher_fix]incorrect start:{}", start);
                break;
            }
            long end = start + step;
            if (end > max) {
                logger.error("[watcher_fix]done.end:{}", end);
                break;
            }

            logger.info("[watcher_fix]begin to fix. step:{},start:{},end:{}", step, start, end);


            List<ai.everylink.chainscan.watcher.entity.Transaction> txList = Lists.newArrayList();

            Connection connection = null;
            PreparedStatement preparedStatement = null;
            try {
                connection = JDBCUtils.getConnection();
                String sql = "select * from transaction where id>=? and id<?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setLong(1, start);
                preparedStatement.setLong(2, end);
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    ai.everylink.chainscan.watcher.entity.Transaction tx = new ai.everylink.chainscan.watcher.entity.Transaction();
                    tx.setTransactionHash(rs.getString("transaction_hash"));
                    tx.setTransactionIndex(getResultSetInt(rs.getObject("transaction_index")));
                    tx.setBlockHash(rs.getString("block_hash"));
                    tx.setBlockNumber(rs.getLong("block_number"));
                    tx.setChainId(rs.getInt("chain_id"));
                    tx.setTxTimestamp(rs.getDate("tx_timestamp"));
                    tx.setFromAddr(rs.getString("from_addr"));
                    tx.setToAddr(rs.getString("to_addr"));
                    tx.setContractAddress(rs.getString("contract_address"));
                    tx.setValue(rs.getString("value"));
                    tx.setTxFee(rs.getString("tx_fee"));
                    tx.setGasLimit(getBigInteger(rs.getObject("gas_limit")));
                    tx.setGasUsed(getBigInteger(rs.getObject("gas_used")));
                    tx.setGasPrice(rs.getString("gas_price"));
                    tx.setNonce(rs.getString("nonce"));
                    tx.setInput(rs.getString("input"));
                    tx.setTxType(getResultSetInt(rs.getString("tx_type")));
                }
                updateTid(end);
            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                // 6. 释放资源
                JDBCUtils.close(preparedStatement,connection);
            }

            connection = null;
            preparedStatement = null;
            try {
                connection = JDBCUtils2.getConnection();
                String sql = "INSERT INTO transaction (transaction_hash, transaction_index, block_hash, block_number, chain_id, status, fail_msg, tx_timestamp, " +
                        "from_addr, to_addr, contract_address, value, tx_fee, gas_limit, gas_used, gas_price, nonce, input,tx_type, " +
                        "create_time, chain_type, token_tag) " +
                        " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                preparedStatement = connection.prepareStatement(sql);
                for (ai.everylink.chainscan.watcher.entity.Transaction b : txList) {
                    preparedStatement.setObject(1, b.getTransactionHash());
                    preparedStatement.setObject(2, b.getTransactionIndex());
                    preparedStatement.setObject(3, b.getBlockHash());
                    preparedStatement.setObject(4, b.getBlockNumber());
                    preparedStatement.setObject(5, b.getChainId());
                    preparedStatement.setObject(6, b.getStatus());
                    preparedStatement.setObject(7, b.getFailMsg());
                    preparedStatement.setObject(8, b.getTxTimestamp());
                    preparedStatement.setObject(9, b.getFromAddr());
                    preparedStatement.setObject(10, b.getToAddr());
                    preparedStatement.setObject(11, b.getContractAddress());
                    preparedStatement.setObject(12, b.getValue());
                    preparedStatement.setObject(13, b.getTxFee());
                    preparedStatement.setObject(14, b.getGasLimit());
                    preparedStatement.setObject(15, b.getGasUsed());
                    preparedStatement.setObject(16, b.getGasPrice());
                    preparedStatement.setObject(17, b.getNonce());
                    preparedStatement.setObject(18, b.getInput());
                    preparedStatement.setObject(19, b.getTxType());
                    preparedStatement.setObject(20, new Date());
                    preparedStatement.setObject(21, "EVM_PoW");
                    preparedStatement.setObject(22, 0);

                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                // 6. 释放资源
                JDBCUtils2.close(preparedStatement,connection);
            }

            logger.info("[watcher_fix]End to fix. step:{},start:{},end:{}", step, start, end);

        }
    }

    private static Integer getResultSetInt(Object val) {
        if (val == null) {
            return null;
        }

        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e){
        }

        return null;
    }

    private static Long getResultSetLong(Object val) {
        if (val == null) {
            return null;
        }

        try {
            return Long.parseLong(val.toString());
        } catch (Exception e){
        }

        return null;
    }

    private static BigInteger getBigInteger(Object val) {
        if (val == null) {
            return null;
        }

        try {
            return BigInteger.valueOf(Long.parseLong(val.toString()));
        } catch (Exception e){
        }

        return null;
    }

    private static String getResultSetString(Object val) {
        if (val == null) {
            return null;
        }

        try {
            return val.toString();
        } catch (Exception e){
        }

        return null;
    }

    private static long getMaxTid() {
        Connection conn = null;
        PreparedStatement pst = null;
        try {
            conn = JDBCUtils.getConnection();
            String sql = "select max(tid) from tid";
            pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            // 6. 释放资源
            JDBCUtils.close(pst,conn);
        }

        return 0;
    }

    private static void updateTid(long tid) {
        Connection conn = null;
        PreparedStatement pst = null;
        try {
            conn = JDBCUtils.getConnection();
            String sql = "insert into tid(tid) values(?)";
            pst = conn.prepareStatement(sql);
            pst.setLong(1, tid);
            int rows = pst.executeUpdate();
            logger.info("[watcher_fix]updateTid.tid={},rows={}", tid, rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            // 6. 释放资源
            JDBCUtils.close(pst,conn);
        }
    }
}
