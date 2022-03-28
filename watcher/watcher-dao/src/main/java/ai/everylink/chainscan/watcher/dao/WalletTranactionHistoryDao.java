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

import ai.everylink.chainscan.watcher.entity.WalletTransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * BridgeHistoryDao
 *
 * @author brett
 * @since 2021-03-26
 */
@Repository
public interface WalletTranactionHistoryDao extends JpaRepository<WalletTransactionHistory, String> {

    /**
     * 更新交易记录
     * @param txHistory
     */
    @Query(value = "update  wallet_transaction_history set from_tx_state = :#{#txHistory.fromTxState},from_tx_time = :#{#txHistory.fromTxTime},confirm_block = :#{#txHistory.confirmBlock}, from_deposit_nonce = :#{#txHistory.fromDepositNonce} where id =:#{#txHistory.id}", nativeQuery = true)
    @Modifying
    @Transactional
    public void updateTxHistory(@Param("txHistory")WalletTransactionHistory txHistory);

    /**
     * 查询指定交易
     * @param chainID
     * @param depositNonce
     * @return
     */
    @Query(value = "select * from wallet_transaction_history where from_chain_id=:chainID and from_deposit_nonce =:depositNonce", nativeQuery = true)
    public WalletTransactionHistory findByChainNonce(Integer chainID, Integer depositNonce);

    /**
     * 更新 to的tx信息
     * @param txHistory
     */
    @Query(value = "update  wallet_transaction_history set to_tx_state = :#{#txHistory.toTxState},to_tx_time = :#{#txHistory.toTxTime},confirm_block = :#{#txHistory.confirmBlock}, to_deposit_nonce = :#{#txHistory.toDepositNonce} where id =:#{#txHistory.id}", nativeQuery = true)
    @Modifying
    @Transactional
    public void updateTxToHistory(@Param("txHistory")WalletTransactionHistory txHistory);
}
