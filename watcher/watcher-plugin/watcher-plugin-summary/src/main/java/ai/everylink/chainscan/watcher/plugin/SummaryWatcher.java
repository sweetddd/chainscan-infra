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
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.plugin.service.SummaryService;
import ai.everylink.chainscan.watcher.plugin.vo.EvmData;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 合约统计发行量
 *
 * @author brett
 * @since 2021-11-26
 */
@Slf4j
public class SummaryWatcher implements IWatcher {

    private SummaryService summaryService;

    @Override
    public List<EvmData> scanBlock() {
        initService();
        // 定时统计circulationSuppl
        long start = System.currentTimeMillis();
        log.info("SummaryWatcher-start:" + start);
        summaryService.circulationSuppl();  //统计合约发行量
        summaryService.totalLockAmount();  //统计合约锁定量
        summaryService.l2LockAmount();     //2层锁定量统计
        summaryService.l1LockAmount();     //1层锁定量统计
        summaryService.burnt();  //统计合约销毁量
        log.info("SummaryWatcher-end:" + System.currentTimeMillis());
        List<EvmData> blockList = Lists.newArrayList();
        return blockList;
    }

    @Override
    public List<IWatcherPlugin> getOrderedPluginList() {
        // 自己创建的
        List<IWatcherPlugin> pluginList = Lists.newArrayList(new SummaryPlugin());
        return pluginList;
    }

    @Override
    public void finalizedBlockStatus() {

    }

    @Override
    public String getCron() {
        return "0 0 * * * ?";
       //return "*/5 * * * * ?";
    }

    private void initService() {
        if (summaryService == null) {
            summaryService = SpringApplicationUtils.getBean(SummaryService.class);
        }
    }

}
