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
     * 更新token插件扫描状态
     * @param id
     */
    @Query(value = "update transaction set token_tag=1 where id=:id", nativeQuery = true)
    @Modifying
    @Transactional
    void updateTokenTag(@Param("id") Long id);

    /**
     * 加载tx数据
     * @return
     */
    @Query(value = "select * from transaction where token_tag=0 order by id ASC limit 100", nativeQuery = true)
    List<Transaction> getTxData();
}
