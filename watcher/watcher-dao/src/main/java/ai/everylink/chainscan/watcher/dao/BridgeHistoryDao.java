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

import ai.everylink.chainscan.watcher.entity.BridgeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * BridgeHistoryDao
 *
 * @author brett
 * @since 2021-03-21
 */
@Repository
public interface BridgeHistoryDao extends JpaRepository<BridgeHistory, String> {

    /**
     * 查询Bridge 记录
     * @param chainID
     * @param depositNonce
     * @return
     */
    @Query(value = "select * from bridge_history where src_chain_id=:chainID and src_deposit_nonce =:depositNonce", nativeQuery = true)
    public List<BridgeHistory> findByChainId(Integer chainID, Integer depositNonce);


    /**
     * 更细beidge
     * @param bridgeHistory
     */
    @Query(value = "update  bridge_history set bridge_state = :#{#bridgeHistory.bridgeState},dst_chain_id = :#{#bridgeHistory.dstChainId},dst_deposit_nonce = :#{#bridgeHistory.dstDepositNonce}, dst_network = :#{#bridgeHistory.dstNetwork}, dst_tx_hash = :#{#bridgeHistory.dstTxHash}, dst_tx_state = :#{#bridgeHistory.dstTxState}, dst_tx_time = :#{#bridgeHistory.dstTxTime} where id =:#{#bridgeHistory.id}", nativeQuery = true)
    @Modifying
    @Transactional
    public void updaeBridgeHistory(@Param("bridgeHistory")BridgeHistory bridgeHistory);
}
