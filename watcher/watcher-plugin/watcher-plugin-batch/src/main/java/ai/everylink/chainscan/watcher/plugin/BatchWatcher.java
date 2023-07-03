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

package ai.everylink.chainscan.watcher.plugin;

import ai.everylink.chainscan.watcher.core.IWatcher;
import ai.everylink.chainscan.watcher.core.IWatcherPlugin;
import ai.everylink.chainscan.watcher.core.util.*;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.dao.BatchDao;
import ai.everylink.chainscan.watcher.entity.Batch;
import ai.everylink.chainscan.watcher.plugin.service.BatchDataService;
import com.google.common.collect.Lists;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * l2 coordinator扫Batch
 *
 * @author sunny.shi@everylink.ai
 * @since 2023-06-30
 */
@Component
@Service
@Data
public class BatchWatcher implements IWatcher {

    private static final Logger logger = LoggerFactory.getLogger(BatchWatcher.class);

    @Autowired
    private BatchDao batchDao;

    /**
     * 当前扫块的链的id
     */
    private static int chainId;

    private BatchDataService batchDataService;


    private Long dbSubmitttedBatchNum=1L;

    private Long dbFinalizedBatchNum=1L;


    @Override
    public String getCron() {
        return "*/1 * * * * ?";
    }

    @Override
    public List<IWatcherPlugin> getOrderedPluginList() {
        return null;
    }


    /**
     * 入口方法
     */
    @Override
    public List<EvmData> scanBlock() {
        init();

        List<EvmData> defaultList = Lists.newArrayList();

        Long latestPendingBatchNum=batchDataService.getLatestPendingBatchNum();

        Long dbBatchNum=batchDataService.getMaxBatchNum() ;

        logger.info("latestPendingBatchNum:{}, dbBatchNum: {}", latestPendingBatchNum,dbBatchNum);

        if (dbBatchNum.equals(latestPendingBatchNum)) {
            logger.info("[WatcherScan]dbBatchNum catch the latestPendingBatchNum");
            return defaultList;
        }

        if (dbBatchNum > latestPendingBatchNum) {
            logger.info("[WatcherScan]dbBatchNum 超过 coordinator latestPendingBatchNum，maybe the coordinator was reset.");
            return defaultList;
        }

        logger.info("[WatcherScan]begin to scan.dbBatchNum={},latestPendingBatchNum={}", dbBatchNum, latestPendingBatchNum);

        long start=dbBatchNum+1;

        long startTime = System.currentTimeMillis();
        while (dbBatchNum<latestPendingBatchNum){
            Batch batch=batchDataService.getBatchByNum(dbBatchNum+1);
            batchDataService.saveBatch(batch);
            dbBatchNum++;
        }

        logger.info("[WatcherScan]end to scan. start={},end={},size={},consume={}ms", start, latestPendingBatchNum, latestPendingBatchNum-start+1, (System.currentTimeMillis() - startTime));
        finalizedBlockStatus();
        return Lists.newArrayList();
    }

    private void init() {
        initService();

    }

    private void initService() {
        if (batchDataService == null) {
            batchDataService = SpringApplicationUtils.getBean(BatchDataService.class);
        }
    }

    /**
     * 数据库中的batch状态更新;
     */
    @Override
    public void finalizedBlockStatus() {

//        if (WatcherUtils.isFinalizedStatus()) {
            logger.info("finalizedBatchStatus executed");

            Long latestSubmittedBatchNum=batchDataService.getLatestSubmittedBatchNum();
            Long latestFinalizedBatchNum=batchDataService.getLatestFinalizedBatchNum();
            while (dbSubmitttedBatchNum<=latestSubmittedBatchNum) {
                batchDataService.syncBatchStatus(dbSubmitttedBatchNum,1);
                dbSubmitttedBatchNum++;
            }
            while (dbFinalizedBatchNum<=latestFinalizedBatchNum) {
                batchDataService.syncBatchStatus(dbFinalizedBatchNum,2);
                dbFinalizedBatchNum++;
            }
            dbSubmitttedBatchNum=latestSubmittedBatchNum+1;
            dbFinalizedBatchNum=latestFinalizedBatchNum+1;
    }
}
