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
import com.google.common.collect.Lists;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.*;

/**
 * 链监控
 *
 * @author david.zhang@everylink.ai
 * @since 2022-04-26
 */
@Component
public class ChainMonitorWatcher implements IWatcher {

    private static final Logger logger = LoggerFactory.getLogger(ChainMonitorWatcher.class);

    private static int chainId;

    private static Web3j web3j;

    private EvmDataService evmDataService;


    /**
     * 入口方法
     *
     * @return
     */
    @Override
    public List<EvmData> scanBlock() {
        init();
        monitor();
        return Lists.newArrayList();
    }


    @Override
    public List<IWatcherPlugin> getOrderedPluginList() {
        return Lists.newArrayList();
    }

    /**
     * 获取最后区块状态更新;
     */
    @Override
    public void finalizedBlockStatus() {
        // noop
    }

    @Override
    public String getCron() {
        return "0 */1 * * * ?";
    }

    private void init() {
        initService();
        initWeb3j();
        chainId = WatcherUtils.getChainId();
    }

    private void initService() {
        if (evmDataService == null) {
            evmDataService = SpringApplicationUtils.getBean(EvmDataService.class);
        }
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

    private void monitor() {
        logger.info("[ChainMonitorWatcher]monitor end");
        int web3ErrCnt = 0;
        for (int i = 0; i < 10; i++) {
            try {
                web3j.ethBlockNumber().send();
                Thread.sleep(1000);
            } catch (Throwable e) {
                web3ErrCnt ++;
                if (web3ErrCnt > 1) {
                    break;
                }
            }
        }
        if (web3ErrCnt > 1) {
            SlackUtils.sendSlackNotify("C02SQNUGEAU", "DTX链告警", "VM链连接出错: " + WatcherUtils.getVmChainUrl());
            return;
        }

        try {
            Date lastBlockCreateTime = evmDataService.getMaxBlockCreationTime(chainId);
            logger.info("[MonitorThread]last block time:{}", lastBlockCreateTime);
            if (lastBlockCreateTime == null) {
                return;
            }

            long diff = System.currentTimeMillis() - lastBlockCreateTime.getTime();
            if (diff < 60 * 1000) {
                return;
            }

            SlackUtils.sendSlackNotify("C02SQNUGEAU", "DTX链告警",
                    "VM链长时间未出块，请关注！最后出块于(\"" + diff / 1000 / 60 + "\")分钟前");
        } catch (Exception e) {
            logger.error("[MonitorThread]error:" + e.getMessage(), e);
        }

        logger.info("[ChainMonitorWatcher]monitor end");
    }

}
