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
import ai.everylink.chainscan.watcher.core.util.WatcherUtils;
import ai.everylink.chainscan.watcher.plugin.EvmPlugin;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.*;

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
                if (onlyEvmPlugin() && plugin.getClass() != EvmPlugin.class) {
                    log.info("Not EvmPlugin");
                    continue;
                }
                long t1 = System.currentTimeMillis();
                if (!WatcherUtils.isProcessConcurrent()) {
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
                } else {
                    // 并发处理区块
                    log.info("Concurrent process block start. total {} blocks", blockList.size());
                    CountDownLatch latch = new CountDownLatch(blockList.size());
                    for (Object block : blockList) {
                        blockProcessPool.submit(new BlockProcessThread(latch, watcher, plugin, block));
                    }
                    latch.await(3, TimeUnit.MINUTES);
                    log.info("Concurrent process block end. {} blocks processed", blockList.size());
                }

                log.info("Plugin process blocks end. consume={}ms,blockSize={},plugin={}",
                        (System.currentTimeMillis() - t1), blockList.size(), plugin.getClass().getSimpleName());

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

    public static boolean onlyEvmPlugin() {
        String flag = System.getenv("watcher.process.only.evmplugin");
        if (!StringUtils.isEmpty(flag)) {
            return flag.trim().equalsIgnoreCase("true");
        }

        return false;
    }

    private static final ThreadPoolExecutor blockProcessPool = new ThreadPoolExecutor(300, 400, 30, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.error("blockProcessPool queue is full");
        }
    });

    public static class BlockProcessThread implements Runnable {
        private final CountDownLatch latch;
        private final IWatcher watcher;
        private final IWatcherPlugin plugin;
        private final Object block;

        public BlockProcessThread(CountDownLatch latch, IWatcher watcher, IWatcherPlugin plugin, Object block) {
            this.latch = latch;
            this.watcher = watcher;
            this.plugin = plugin;
            this.block = block;
        }

        @Override
        public void run() {
            try {
                plugin.processBlock(block);
            } catch (Throwable e) {
                log.error(String.format("Process block error. watcher=[%s],plugin=[%s]",
                        watcher.getClass().getSimpleName(), plugin.getClass().getSimpleName()), e);
            } finally {
                latch.countDown();
            }
        }
    }

}
