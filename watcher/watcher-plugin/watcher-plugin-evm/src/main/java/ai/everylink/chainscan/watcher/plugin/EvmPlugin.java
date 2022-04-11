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
import ai.everylink.chainscan.watcher.core.WatcherExecutionException;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.plugin.service.EvmDataService;
import ai.everylink.chainscan.watcher.plugin.service.EvmScanDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

/**
 * ERC20 chain data plugin
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
public class EvmPlugin implements IEvmWatcherPlugin {

    private Logger logger = LoggerFactory.getLogger(EvmPlugin.class);

    private EvmDataService evmDataService;

    private EvmScanDataService evmScanDataService;

    @Override
    public <T> boolean processBlock(T block) throws WatcherExecutionException {
        EvmData blockData = (EvmData) block;
        initService();

        BigInteger blockNumber = blockData.getBlock().getNumber();
        logger.info("EvmPlugin 处理: " + blockNumber
                                   + "; tx size=" + blockData.getBlock().getTransactions().size());

        long t1 = System.currentTimeMillis();
        try {
            evmDataService.saveEvmData(blockData);
            logger.info("[EvmWatcher]saveEvmData.consume={}ms", (System.currentTimeMillis() - t1));
        } catch (Exception e) {
            logger.error("Error occured when process block=" + blockNumber, e);
            return false;
        }

        // 删除原生扫块数据
        try {
            long t2 = System.currentTimeMillis();
            evmScanDataService.deleteBlockData(blockNumber.longValue());
            logger.info("Successfully delete old data:{},consume={}ms", blockNumber, (System.currentTimeMillis() - t2));
        } catch (Exception e) {
            logger.error("Error occured when delete old block=" + blockNumber, e);
        }

        return true;
    }

    private void initService() {
        if (evmDataService == null) {
            evmDataService = SpringApplicationUtils.getBean(EvmDataService.class);
        }

        if (evmScanDataService == null) {
            evmScanDataService = SpringApplicationUtils.getBean(EvmScanDataService.class);
        }
    }
}
