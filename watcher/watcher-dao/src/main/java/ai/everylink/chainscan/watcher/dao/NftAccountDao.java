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

import ai.everylink.chainscan.watcher.entity.NftAccount;
import ai.everylink.chainscan.watcher.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

/**
 * NftAccountDao
 *
 * @author brett
 * @since 2021-12-14
 */
public interface NftAccountDao extends JpaRepository<NftAccount, String> {

    /**
     * 删除账户在合约上的NFT信息
     * @param fromAddr
     * @param contract
     */
    @Query(value = "delete from nft_account   where  account=(?1) and contract=(?2)", nativeQuery = true)
    @Modifying
    @Transactional
    public void deleteNftAccount(String fromAddr, String contract);
}
