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

import ai.everylink.chainscan.watcher.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * BlockDao
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
public interface BlockDao extends JpaRepository<Block, Long> {

    /**
     * get block by block num
     *
     * @param blockNum
     * @param chainId
     * @return
     */
    @Query(value = "select block_number from block where block_number=:blockNum and chain_id=:chainId", nativeQuery = true)
    Long getBlockIdByNum(@Param("blockNum") Long blockNum, @Param("chainId") int chainId);

    /**
     * get max block num
     * @return
     */
    @Query(value = "select max(block_number) from block", nativeQuery = true)
    Long getMaxBlockNum(int chainId);

    /**
     * 更新 区块状态
     * @param finalizedHash
     */
    @Query(value = "update  block set status = 1 where   id  < (select  max_id from  (select id as  max_id from block where block_hash = ?1) as b)", nativeQuery = true)
    @Modifying
    @Transactional
    void updateBlockByHash(String finalizedHash);

    /**
     * get max block num
     * @return
     */
    @Query(value = "select block_timestamp from block order by block_number desc limit 1", nativeQuery = true)
    Date getMaxBlockCreationTime(int chainId);

    @Query(value = "INSERT INTO block (block_number, block_hash, chain_id, block_timestamp, parent_hash, miner, nonce, validator, burnt, tx_size, reward, difficulty, total_difficulty, block_size, gas_used, gas_limit, extra_data, create_time, status, block_fee, chain_type, finalized) VALUES ( ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20, ?21, ?21, ?22)", nativeQuery = true)
    @Modifying
    @Transactional
    int insertNative(Long blockNumber, String blockHash, Integer chainId, Date blockTimestamp,
                     String parentHash, String miner, String nonce, String validator, String burnt,
                     Integer txSize, String reward, String difficulty, String totalDifficulty,
                     Integer blockSize, BigInteger gasUsed, BigInteger gasLimit, String extraData,
                     Date createTime, Integer status, BigDecimal blockFee, String chainType, Integer finalized);

}
