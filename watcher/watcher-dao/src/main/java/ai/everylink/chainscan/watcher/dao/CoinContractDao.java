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

import ai.everylink.chainscan.watcher.entity.CoinContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * CoinContractDao
 *
 * @author brett
 * @since 2021-12-14
 */
public interface CoinContractDao extends JpaRepository<CoinContract, String> {

    @Query(value = "select * from token_contract where  chain_id=:chainId", nativeQuery = true)
    public List<CoinContract> selectByChainId(@Param("chainId") Long chainId);

    @Query(value = "select * from token_contract where  name=:coinNam", nativeQuery = true)
    public List<CoinContract> selectByName(String coinNam);
}
