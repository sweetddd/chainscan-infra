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
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.service.NFTAuctionService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author brett
 * @since 2022-05-05
 */
@Slf4j
public class NFTAuctionSpiPlugin implements IEvmWatcherPlugin {


    private NFTAuctionService nftAuctionService;

    @Override
    public <T> boolean processBlock(T block) throws WatcherExecutionException {
        log.info("NFTAuctionSpiPlugin.processBlock start, block:{}", block);
        initService();

        Transaction transaction = null;
        try {
            transaction = (Transaction) block;
        }   catch (Exception e) {
            log.error("block is not a transaction", e);
        }
        log.info("NFTAuctionSpiPlugin.processBlock, transaction:{}", transaction);
        if (transaction != null) {
           return true;
        }else {
            EvmData blockData = (EvmData) block;
            log.info("NFTAuctionSpiPlugin.processBlock, blockData.start:{}", blockData);
            nftAuctionService.nftAuctionScan(blockData);
            log.info("NFTAuctionSpiPlugin.processBlock, blockData.end");
        }
        return true;
    }

    private void initService() {
        if (nftAuctionService == null) {
            nftAuctionService = SpringApplicationUtils.getBean(NFTAuctionService.class);
        }
    }

}
