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

import ai.everylink.chainscan.watcher.core.IWatcherPlugin;
import ai.everylink.chainscan.watcher.core.WatcherExecutionException;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.entity.Block;
import ai.everylink.chainscan.watcher.entity.IncentiveBlock;
import ai.everylink.chainscan.watcher.entity.IncentiveTransaction;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.service.IncentiveService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 *
 *
 * @author jerry
 * @since 2021-12-24
 */
public class IncentivePlugin implements IWatcherPlugin {

    private IncentiveService incentiveService;

    private void init() {
        initService();
    }

    private void initService() {
        if (incentiveService == null) {
            incentiveService = SpringApplicationUtils.getBean(IncentiveService.class);
        }
    }

    @Override
    @Transactional
    public <T> boolean processBlock(T block) throws WatcherExecutionException {
        init();
        IncentiveBlock incentiveBlock = (IncentiveBlock) block;
        Block          _block         = incentiveService.incentiveBlockConvert(incentiveBlock);
        _block.setBurnt(null);
        _block.setReward(null);
        Block                      savedBlock            = incentiveService.saveBlock(_block);
        List<IncentiveTransaction> incentiveTransactions = incentiveBlock.getExtrinsics();
        Integer                    index                 = 0;
        for (IncentiveTransaction incentiveTransaction : incentiveTransactions) {
            Transaction transaction = incentiveService.incentiveTransactionConvert(savedBlock, incentiveTransaction, index);
            incentiveService.saveTransaction(transaction);
            index ++;
        }
        return true;
    }

}
