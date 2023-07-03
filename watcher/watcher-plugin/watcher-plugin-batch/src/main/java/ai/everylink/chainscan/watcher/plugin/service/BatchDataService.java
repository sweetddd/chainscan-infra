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

import ai.everylink.chainscan.watcher.entity.Batch;
import ai.everylink.chainscan.watcher.entity.Status;

import java.util.List;

/**
 * Batch数据服务
 *
 * @author sunny.shi@everylink.ai
 * @since 2023-06-29
 */
public interface BatchDataService {

    /**
     * 获取最新的PendingBatchNum
     * @return
     */
    Long getLatestPendingBatchNum();

    /**
     * 获取最新的SubmittedBatchNum
     * @return
     */
    Long getLatestSubmittedBatchNum();

    /**
     * 获取最新的FinalizedBatchNum
     * @return
     */
    Long getLatestFinalizedBatchNum();

    /**
     * 获取Batch列表
     * @return
     */
    List<Batch> getBatchList();

    /**
     * 通过batchNum获取Batch
     * @param num
     * @return
     */
    Batch getBatchByNum(Long num);

    /**
     * 获取数据库最大的batch
     * @return
     */
    Long getMaxBatchNum();


    /**
     * 获取数据库最大的batch
     * @param batch
     */
    void saveBatch(Batch batch);



    /**
     * 更新数据库batch的状态
     * @param id,status
     */
    void syncBatchStatus(Long id, int status);
}

