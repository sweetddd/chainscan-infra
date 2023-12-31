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

import ai.everylink.chainscan.watcher.entity.AccountInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * BlockDao
 *
 * @author brett
 * @since 2022-04-09
 */
public interface AccountInfoDao extends JpaRepository<AccountInfo, Long> {

    /**
     * 查询指定账户信息;
     * @param fromAddr
     * @return
     */
    @Query(value = "select * from account_info where address=:fromAddr", nativeQuery = true)
    AccountInfo findByAddress(String fromAddr);
}
