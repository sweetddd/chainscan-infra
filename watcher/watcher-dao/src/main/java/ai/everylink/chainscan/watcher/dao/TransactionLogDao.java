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

package ai.everylink.chainscan.watcher.dao;

import ai.everylink.chainscan.watcher.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TransactionLogDao
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
public interface TransactionLogDao extends JpaRepository<TransactionLog, Long> {

    /**
     * 批量删除
     *
     * @param transactionHashList
     * @return
     */
    @Query(value = "delete from transaction_log where transaction_hash in(?1)", nativeQuery = true)
    @Modifying
    @Transactional
    int deleteByTransactionHash(List<String> transactionHashList);


    /**
     * 查询指定hash的log
     */
    @Query(value = "select * from transaction_log where transaction_hash=:txHash", nativeQuery = true)
    public List<TransactionLog>  findByTxHash(String txHash);

    /**
     * 查询跨链event
     * @param txHash
     * @param logIndex
     * @return
     */
    @Query(value = "select * from transaction_log where transaction_hash=:txHash and log_index =:logIndex", nativeQuery = true)
    TransactionLog findTopic(String txHash, int logIndex);
}
