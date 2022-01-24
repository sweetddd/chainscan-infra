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
import ai.everylink.chainscan.watcher.plugin.service.EvmDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ERC20 chain data plugin
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
public class EvmPlugin implements IEvmWatcherPlugin {

    private Logger logger = LoggerFactory.getLogger(EvmPlugin.class);

    private EvmDataService evmDataService;

    @Override
    public <T> boolean processBlock(T block) throws WatcherExecutionException {
        EvmData blockData = (EvmData) block;
        initService();
        System.out.println("EvmPlugin 处理: " + blockData.getBlock().getNumber()
                                   + "; tx size=" + blockData.getBlock().getTransactions().size());

        try {
            evmDataService.saveEvmData(blockData);
        } catch (Exception e) {
            logger.error("Error occured when process block=" + ((EvmData) block).getBlock().getNumber(), e);
            return false;
        }

        return true;
    }

    private void initService() {
        if (evmDataService == null) {
            evmDataService = SpringApplicationUtils.getBean(EvmDataService.class);
        }
    }
}
