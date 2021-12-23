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

package ai.everylink.chainscan.watcher.bootstrap;

import ai.everylink.chainscan.watcher.core.IWatcher;
import ai.everylink.chainscan.watcher.core.IWatcherPlugin;
import ai.everylink.chainscan.watcher.core.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.util.CollectionUtils;
import java.util.List;

/**
 * 扫块模块
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-6
 */
@Slf4j
@DisallowConcurrentExecution
public class BlockChainScanJob implements Job {

    private IWatcher watcher;

    /**
     * 扫块和处理块数据
     * @param ctx
     * @throws JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        // 标识一次处理过程，便于排查问题
        long id = System.currentTimeMillis();

        watcher = (IWatcher) ctx.getMergedJobDataMap().get("watcher");
        log.info("[{}]Execute watcher start.watcher=[{}],nextFireTime=[{}]]",
                id,
                watcher.getClass().getSimpleName(),
                DateUtil.format_yyyy_MM_dd_HH_mm_ss(ctx.getTrigger().getNextFireTime()));

        try {
            // 1.扫块
            List<Object> blockList = watcher.scanBlock();
            if (CollectionUtils.isEmpty(blockList)) {
                log.info("[{}]Scan blocks not found.watcher=[{}]", id, watcher.getClass().getSimpleName());
                return;
            }
            log.info("[{}]Scan blocks end.watcher=[{}],size={}", id, watcher.getClass().getSimpleName(), blockList.size());

            // 2.获取plugin列表
            List<IWatcherPlugin> pluginList = watcher.getOrderedPluginList();
            if (CollectionUtils.isEmpty(pluginList)) {
                log.error("["+id+"]No plugins found for watcher [{}]", watcher.getClass().getSimpleName());
                return;
            }

            // 3.处理块信息
            for (IWatcherPlugin plugin : pluginList) {
                for (Object block : blockList) {
                    try {
                        boolean result = plugin.processBlock(block);
                        log.info("[{}]Processed block.watcher=[{}],plugin=[{}],result={}",
                                id, watcher.getClass().getSimpleName(), plugin.getClass().getSimpleName(), result);

                        // block需要按顺序处理，一个处理失败，后续不能再继续
                        if (!result) {
                            break;
                        }
                    } catch (Throwable e) {
                        log.error(String.format("[%s]Process block error. watcher=[%s],plugin=[%s]",
                                id+"", watcher.getClass().getSimpleName(), plugin.getClass().getSimpleName()), e);
                    }
                }
            }
        } catch (Throwable e) {
            log.error("["+id+"]Execute watcher error.watcher=["+watcher.getClass().getSimpleName()+"]", e);
        }

        //最后确认区块数据更新;
        try {
            watcher.finalizedBlockStatus();
        } catch (Throwable e) {
            log.error("["+id+"]Execute watcher error.watcher=["+watcher.getClass().getSimpleName()+"]", e);
        }
    }
}
