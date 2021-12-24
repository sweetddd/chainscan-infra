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

import ai.everylink.chainscan.watcher.plugin.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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
    @Query(value = "select * from block where block_num=:blockNum and chain_id=:chainId", nativeQuery = true)
    Block getBlockByNum(@Param("blockNum") Long blockNum, @Param("chainId") int chainId);

    /**
     * get block by block hash
     * @param blockHash
     * @param chainId
     * @return
     */
    @Query(value = "select * from block where block_hash=:blockHash and chain_id=:chainId", nativeQuery = true)
    Block getBlockByHash(@Param("blockHash") String blockHash, @Param("chainId") int chainId);

    /**
     * get max block num
     * @return
     */
    @Query(value = "select max(block_number) from block where chain_id=?1", nativeQuery = true)
    Long getMaxBlockNum(int chainId);


    /**
     * 更新 区块状态
     * @param finalizedHash
     */
    @Query(value = "update  block set status = 1 where   id  < (select  max_id from  (select id as  max_id from block where block_hash = ?1) as b)", nativeQuery = true)
    @Modifying
    @Transactional
    void updateBlockByHash(String finalizedHash);
}
