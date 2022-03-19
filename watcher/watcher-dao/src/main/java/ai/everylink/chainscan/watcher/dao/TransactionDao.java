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

import ai.everylink.chainscan.watcher.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TransactionDao
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
public interface TransactionDao extends JpaRepository<Transaction, Long> {

    /**
     * 删除某个block下的所有transaction
     *
     * @param blockNum
     * @param chainId
     * @return
     */
    @Query(value = "delete from transaction where block_num=:blockNum and chain_id=:chainId", nativeQuery = true)
    @Modifying
    @Transactional
    int deleteByBlockNum(@Param("blockNum") Long blockNum, @Param("chainId") int chainId);

    /**
     * 更新input字段
     *
     * @param transactionHash
     * @param inputMethod
     * @param inputParams
     * @return
     */
    @Query(value = "update transaction set input_method=:inputMethod,input_params=:inputParams where transaction_hash=:transactionHash", nativeQuery = true)
    @Modifying
    @Transactional
    int updateInputByHash(@Param("transactionHash") String transactionHash, @Param("inputMethod") String inputMethod,
                          @Param("inputParams") String inputParams);


    /**
     * 更新token插件扫描状态
     * @param id
     */
    @Query(value = "update transaction set token_tag=1 where id=:id", nativeQuery = true)
    @Modifying
    @Transactional
    public void updateTokenTag( @Param("id")Long id);


    /**
     * 查询未扫描的交易;
     * @param tokenTag
     * @return
     */
    @Query(value = "select * from transaction where token_tag=:tokenTag order by create_time", nativeQuery = true)
    List<Transaction> findByTokenTag(int tokenTag);
}
