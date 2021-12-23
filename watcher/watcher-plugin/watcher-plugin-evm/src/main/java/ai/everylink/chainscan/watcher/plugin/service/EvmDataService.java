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


import ai.everylink.chainscan.watcher.plugin.EvmData;

/**
 * EVM数据服务
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
public interface EvmDataService {

    /**
     * 保存区块数据
     *
     * @param data
     */
    void saveEvmData(EvmData data);

    /**
     * 获取指定chain的处理进度
     * @param chainId
     * @return
     */
    Long getMaxBlockNum(int chainId);

    /**
     * 根据最后确认hash更新block状态;
     * @param finalizedHash
     */
    public void updateBlockByHash(String finalizedHash);
}
