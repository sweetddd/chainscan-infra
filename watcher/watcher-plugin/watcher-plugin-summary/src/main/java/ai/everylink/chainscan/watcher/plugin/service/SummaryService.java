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

package ai.everylink.chainscan.watcher.plugin.service;


import ai.everylink.chainscan.watcher.plugin.vo.EvmData;

/**
 * EVM数据服务
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
public interface SummaryService {

    /**
     *统计合约发行量
     */
    public void circulationSuppl();

    /**
     *总奖励的MOBI数量
     */
    public void TotalRewards();

    /**
     *总质押MOS数量
     */
    public void TotalStake();

    /**
     *总质押MOS数量
     */
    public void burnt();


    /**
     *总质押MOS数量
     */
    public void totalLockAmount();




}