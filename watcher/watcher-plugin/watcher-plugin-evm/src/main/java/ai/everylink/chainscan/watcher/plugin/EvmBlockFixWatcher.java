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
import ai.everylink.chainscan.watcher.core.util.WatcherUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.plugin.rocketmq.SlackUtils;
import ai.everylink.chainscan.watcher.plugin.service.EvmDataService;
import ai.everylink.chainscan.watcher.plugin.util.Utils;
import com.google.common.collect.Lists;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.*;

/**
 * 以太坊扫块
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
@Component
public class EvmBlockFixWatcher implements IWatcher {

    private static Logger logger = LoggerFactory.getLogger(EvmBlockFixWatcher.class);

    /**
     * 当前扫块的链的id
     */
    private int chainId;

    private Web3j web3j;

    /**
     * 从数据库里面获取处理进度
     */
    private EvmDataService evmDataService;

    private Long startBlockNum = 0L;

    @Override
    public List<EvmData> scanBlock() {
        init();

        List<Long> missedBlockIdList = evmDataService.listMissedBlockNumber(startBlockNum);
        logger.info("[EvmBlockFixWatcher]Fixing. size:{}", missedBlockIdList == null ? 0 : missedBlockIdList.size());
        if (CollectionUtils.isEmpty(missedBlockIdList)) {
            return Lists.newArrayList();
        }

        List<EvmData> list = Lists.newArrayList();
        for (Long blockId : missedBlockIdList) {
            try {
                EvmData evmData = Utils.replayOneBlock(blockId, logger, web3j, chainId);
                if (evmData == null) {
                    logger.error("[EvmBlockFixWatcher]no block found: {}", blockId);
                    break;
                }
                list.add(evmData);
            } catch (Exception e) {
                logger.error("[EvmBlockFixWatcher]error when process block:" + blockId, e);
                list.clear(); // clear
                break;
            }
        }

        return list;
    }

    @Override
    public List<IWatcherPlugin> getOrderedPluginList() {
        // 自己创建的
        List<IWatcherPlugin> pluginList = Lists.newArrayList(new EvmPlugin());
        return pluginList;
    }

    @Override
    public void finalizedBlockStatus() {
    }

    @Override
    public String getCron() {
       return "*/10 * * * * ?";
    }


    private void init() {
        initWeb3j();
        initService();
        chainId = WatcherUtils.getChainId();
        logger.info("[EvmBlockFixWatcher]init config. chainId={}, rpcUrl={}, chainType={},db={}, timeZone={},mqAddr:{}",
                chainId, WatcherUtils.getVmChainUrl(), WatcherUtils.getChainType(),
                System.getenv("spring.datasource.chainscan.jdbc-url"),
                Calendar.getInstance().getTimeZone(), SlackUtils.getNamesrvAddr());
    }


    private void initService() {
        if (evmDataService == null) {
            evmDataService = SpringApplicationUtils.getBean(EvmDataService.class);
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

    public List<EvmData> replayBlock(Long startBlockNumber, Long endBlockNumber) throws Exception {
        return Utils.replayBlock(startBlockNumber, endBlockNumber, logger, web3j, chainId);
    }

}
