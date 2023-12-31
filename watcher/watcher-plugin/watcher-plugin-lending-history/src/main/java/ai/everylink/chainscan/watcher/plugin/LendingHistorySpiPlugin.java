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
import ai.everylink.chainscan.watcher.core.config.EvmConfig;
import ai.everylink.chainscan.watcher.core.config.LendingContractConfig;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.service.LendingHistoryService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author brett
 * @since 2022-03-21
 */
@Slf4j
public class LendingHistorySpiPlugin implements IEvmWatcherPlugin {


    private LendingHistoryService lendingHistoryService;

    @Override
    public <T> boolean processBlock(T block) throws WatcherExecutionException {

        initService();

        long    start     = System.currentTimeMillis();
        log.info("lending-start:" + start);
        Transaction transaction = null;
        try {
            transaction =   (Transaction) block;
        }   catch (Exception e) {
            // log.error("block is not a transaction", e);
        }
        if (transaction != null) {
           // transactionHistoryService.transactionHistoryTxScan(transaction);
        }else {
            EvmData blockData = (EvmData) block;
            lendingHistoryService.transactionHistoryScan(blockData);
        }
        log.info("lending-end:" + System.currentTimeMillis());
        return true;
    }

    private void initService() {
        if (lendingHistoryService == null) {
            lendingHistoryService = SpringApplicationUtils.getBean(LendingHistoryService.class);
        }
    }

}
