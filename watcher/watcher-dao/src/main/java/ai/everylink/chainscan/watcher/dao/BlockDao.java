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
     * 此处不能修改，chainId会导致全表扫描，影响数据库
     * @return
     */
    @Query(value = "select max(block_number) from block", nativeQuery = true)
    Long getMaxBlockNum(int chainId);

    /**
     * 查询 区块
     * @param finalizedHash
     */
    @Query(value = "select * from block where block_hash = ?1", nativeQuery = true)
    Block queryBlockByHash(String finalizedHash);

    /**
     * 查询 区块
     * @param id
     */
    @Query(value = "update block set status = :status where id = :id", nativeQuery = true)
    @Modifying
    @Transactional
    void syncBlockStatus(Long id, Integer status);


    /**
     * get max block num
     * @return
     */
    @Query(value = "select block_timestamp from block order by block_number desc limit 1", nativeQuery = true)
    Date getMaxBlockCreationTime(int chainId);

}
