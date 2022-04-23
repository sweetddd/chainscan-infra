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

package ai.everylink.chainscan.watcher.plugin.service;


import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.entity.Transaction;

/**
 * transaction数据统计service
 *
 * @author brett
 * @since 2021-12-30
 */
public interface TransactionHistoryService {

    /**
     *transaction 信息扫描
     */
    void transactionHistoryScan(EvmData blockData);

    /**
     * 更新交易区块确认信息;
     * @param blockData
     */
    void updateConfirmBlock(EvmData blockData);

    /**
     * 扫描现有数据加载插件
     * @param transaction
     */
    void transactionHistoryTxScan(Transaction transaction);
}
