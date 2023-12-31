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
public interface NftAccountDao extends JpaRepository<NftAccount, Long> {

    /**
     * 删除账户在合约上的NFT信息
     * @param accountId
     * @param tokenId
     */
    @Query(value = "delete from nft_account   where  account_id=(?1) and token_id=(?2)", nativeQuery = true)
    @Modifying
    @Transactional
    void deleteNftAccount(Long accountId, Long tokenId);

    /**
     * 查询账户在合约上的NFT信息
     * @param nftId
     * @param tokenId
     * @return
     */
    @Query(value = "select * from nft_account where nft_id=:nftId and token_id =:tokenId and account_id=:accountId", nativeQuery = true)
    NftAccount selectByTokenIdContract(long nftId, Long tokenId, Long accountId);

    /**
     * 根据交易hash获取nft
     */
    NftAccount findByTxHash(String txHash);

    /**
     * 删除账户在合约上的NFT信息
     * @param accountId
     * @param tokenId
     */
    @Query(value = "delete from nft_account   where  account_id=(?1) and token_id=(?2) and nft_id = (?3)", nativeQuery = true)
    @Modifying
    @Transactional
    void deleteNftTokenId(Long accountId, Long tokenId, Long nftId);

    @Query(value = "update  nft_account set amount=:amount where id=:primaryId", nativeQuery = true)
    @Modifying
    @Transactional
    int updateAmount(Long primaryId, Long amount);

    @Query(value = "update  nft_account set watcher_updated=:watcherUpdated, amount=:amount where nft_id=:nftId and token_id =:tokenId and account_id=:accountId", nativeQuery = true)
    @Modifying
    @Transactional
    int updateAmountAndWatcherStatus(long nftId, Long tokenId, Long accountId, Integer watcherUpdated, Long amount);

    @Query(value = "update  nft_account set watcher_updated=:watcherUpdated, amount=:amount where id=:id", nativeQuery = true)
    @Modifying
    @Transactional
    int updateAmountAndWatcherStatus(long id, Long amount);

}
