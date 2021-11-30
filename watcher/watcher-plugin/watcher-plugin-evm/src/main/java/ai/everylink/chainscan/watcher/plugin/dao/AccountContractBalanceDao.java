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

import ai.everylink.chainscan.watcher.plugin.entity.AccountContractBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * AccountContractBalanceDao
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
public interface AccountContractBalanceDao extends JpaRepository<AccountContractBalance, Long> {

    /**
     * 更新余额
     *
     * @param balance
     * @param accountAddr
     * @param contractAddr
     * @return
     */
    @Query(value = "update account_contract_balance set balance=(?1) where account_addr=(?2) and contract_addr=(?3)", nativeQuery = true)
    @Modifying
    @Transactional
    int updateBalance(String balance, String accountAddr, String contractAddr);

}
