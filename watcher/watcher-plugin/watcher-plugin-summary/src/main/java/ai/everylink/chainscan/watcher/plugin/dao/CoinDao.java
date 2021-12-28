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

package ai.everylink.chainscan.watcher.plugin.dao;

import ai.everylink.chainscan.watcher.plugin.entity.Coin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * CoinDao
 *
 * @author brett
 * @since 2021-12-14
 */
public interface CoinDao extends JpaRepository<Coin, String> {

    /**
     * 更新totalSupply
     * @param totalSupply
     * @param name
     */
    @Query(value = "update coin set total_supply =(?1) where name=(?2)", nativeQuery = true)
    @Modifying
    @Transactional
    public void updateTotalSupply(BigInteger totalSupply, String name);

    /**
     * 更新totalLockAmount
     * @param totalLockAmount
     * @param name
     */
    @Query(value = "update coin set total_lock_amount =(?1) where name=(?2)", nativeQuery = true)
    @Modifying
    @Transactional
    public void updateTotalLockAmount(BigInteger totalLockAmount, String name);

    /**
     * 更新burntAmount
     * @param totalBurntAmount
     * @param name
     */
    @Query(value = "update coin set total_burnt_amount =(?1) where name=(?2)", nativeQuery = true)
    @Modifying
    @Transactional
    public void burntAmount(BigInteger totalBurntAmount, String name);

    /**
     * 更新2层锁定量
     * @param balance
     * @param symbol
     */
    @Query(value = "update coin set l2_lock_amount =(?1) where symbol=(?2)", nativeQuery = true)
    @Modifying
    @Transactional
    public void updateL2LockAmount(String balance,String symbol) ;
}
