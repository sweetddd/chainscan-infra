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

import ai.everylink.chainscan.watcher.core.IWatcher;
import ai.everylink.chainscan.watcher.core.IWatcherPlugin;
import ai.everylink.chainscan.watcher.core.util.OkHttpUtil;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.core.util.VmChainUtil;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.plugin.rocketmq.SlackUtils;
import ai.everylink.chainscan.watcher.plugin.service.EvmDataService;
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

/**
 * 以太坊扫块
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
@Component
public class EvmWatcher implements IWatcher {

    private static Logger logger = LoggerFactory.getLogger(EvmWatcher.class);

    private VmChainUtil vmChainUtil;

    /**
     * 当前扫描高度(扫描时从数据库实时获取当前高度)
     */
    private Long currentBlockHeight = 0L;

    /**
     * 每次扫块最大扫块步数
     */
    private int step;

    /**
     * 当前扫块的链的id
     */
    private int chainId;

    private Web3j web3j;

    /**
     * 从数据库里面获取处理进度
     */
    private EvmDataService evmDataService;

    /**
     * 区块生产超时时间
     */
    private static final Long BLOCK_PRODUCE_TIMEOUT = 11*60*1000L;

    @Override
    public List<EvmData> scanBlock() {
        long start = System.currentTimeMillis();

        init();

        List<EvmData> blockList = Lists.newArrayList();

        Long networkBlockHeight = getNetworkBlockHeight();
        logger.info("loop scan begin.curNum={},netNum={}", currentBlockHeight, networkBlockHeight);
        if (networkBlockHeight <= 0) {
            logger.info("[slack_alert]chain block height is 0, maybe the chain is down.");
            return Lists.newArrayList();
        }

        long startBlockNumber = 0;
        try {
            if (currentBlockHeight < networkBlockHeight) {
                startBlockNumber = currentBlockHeight + 1;
                currentBlockHeight = (networkBlockHeight - currentBlockHeight > step)
                        ? currentBlockHeight + step
                        : networkBlockHeight;

                blockList = replayBlock(startBlockNumber, currentBlockHeight);
                logger.info("Scan block from {} to {},resultSize={}", startBlockNumber, currentBlockHeight, blockList.size());
                if (CollectionUtils.isEmpty(blockList)) {
                    logger.info("[slack_alert]扫块失败！！！start=" + startBlockNumber + ", end=" + currentBlockHeight);
                    currentBlockHeight = startBlockNumber - 1;

                    // 发送slack通知
                    sendVmAlertMsgToSlack();

                    return Lists.newArrayList();
                }
            } else {
                logger.info("[slack_alert]当前块高超过链上块高，maybe the chain was reset.");
            }
        } catch (Throwable e) {
            currentBlockHeight = startBlockNumber - 1;
            logger.error("loop scan error.curNum={" + currentBlockHeight + "},netNum={" + networkBlockHeight + "}", e);
        }

        logger.info("loop scan end.curNum={},netNum={},consume={}ms",
                currentBlockHeight, networkBlockHeight, (System.currentTimeMillis() - start));

        return blockList;
    }


    @Override
    public List<IWatcherPlugin> getOrderedPluginList() {
        return Lists.newArrayList(new EvmPlugin());
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
       return "*/10 * * * * ?";
    }

    private void init() {
        logger.info("[EvmWatcher]timeZone={}", Calendar.getInstance().getTimeZone());
        initWeb3j();
        initService();
        step = Utils.getScanStep();
        chainId = Utils.getChainId();
        currentBlockHeight = evmDataService.getMaxBlockNum(chainId);
        logger.info("[EvmWatcher]init config. step={}, chainId={}, rpcUrl={}, chainType={}",
                step, chainId, Utils.getVmChainUrl(), Utils.getChainType());
        logger.info("[EvmWatcher]got rocketmq name srv addr:{}", SlackUtils.getNamesrvAddr());
        logger.info("==================Current DB block height:{},chainId:{}======", currentBlockHeight, chainId);
    }


    private void initService() {
        if (evmDataService == null) {
            evmDataService = SpringApplicationUtils.getBean(EvmDataService.class);
        }
        if (vmChainUtil == null) {
            vmChainUtil = SpringApplicationUtils.getBean(VmChainUtil.class);
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
            String rpcUrl = Utils.getVmChainUrl();
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

    public List<EvmData> replayBlock(Long startBlockNumber, Long endBlockNumber) throws Exception {
        List<EvmData> dataList = Lists.newArrayList();

        for (Long blockHeight = startBlockNumber; blockHeight <= endBlockNumber; blockHeight++) {
            logger.info("Begin to scan block={}", blockHeight);

            EvmData data = new EvmData();
            data.setChainId(chainId);

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
        }

        return dataList;
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
