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

import ai.everylink.chainscan.watcher.entity.BlockData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * BlockDataDao
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
public interface BlockDataDao extends JpaRepository<BlockData, Long> {

    @Query(value = "select max(block_number) from block_data", nativeQuery = true)
    Long queryMaxBlockNumber();

    @Query(value = "select * from block_data where block_number > ?1 order by block_number asc limit ?2", nativeQuery = true)
    List<BlockData> listBlock(long startBlock, int limit);

    @Query(value = "delete from  block_data where block_number=?1 limit 1", nativeQuery = true)
    @Modifying
    @Transactional
    int deleteBlockByBlockNumber(Long blockNumber);
}
