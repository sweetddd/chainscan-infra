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

import ai.everylink.chainscan.watcher.entity.TokenInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * CoinDao
 *
 * @author brett
 * @since 2021-12-30
 */
public interface TokenInfoDao extends JpaRepository<TokenInfo, String> {

    /**
     *
     * @param contract
     * @return
     */
    @Query(value = "select * from token_info where  address=:contract", nativeQuery = true)
    TokenInfo findAllByAddress(String contract);

    /**
     * 查询合约信息
     * @param txTag
     * @return
     */
    @Query(value = "select * from token_info where  token_name=:txTag", nativeQuery = true)
    TokenInfo selectByTokenName(String txTag);

    /**
     * 更新token信息
     * @param toString
     * @param id
     */
    @Modifying
    @Transactional
    @Query(value = "update token_info set address =(?1) where id=(?2)", nativeQuery = true)
    void updateAddress(String toString, long id);
}
