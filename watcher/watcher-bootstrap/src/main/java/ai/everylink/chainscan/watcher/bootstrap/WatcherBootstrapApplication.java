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
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.ServiceLoader;

/**
 * Watcher boot
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
@Slf4j
@SpringBootApplication(scanBasePackages="ai.everylink.chainscan.watcher")
public class WatcherBootstrapApplication {

    /**
     * Main Entrance.
     *
     * @param args startup arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(WatcherBootstrapApplication.class, args);

        loopScan();
    }

    private static void loopScan() {
        try {
            Scheduler scheduler = new StdSchedulerFactory().getScheduler();

            // 把每个watcher封装为一个job，交由Quartz框架进行调度
            Iterable<IWatcher> watcherList = listWatcher();
            for (IWatcher watcher : watcherList) {
                List<String> chainList = watcher.listSupportedChain();
                if (CollectionUtils.isEmpty(chainList)) {
                    log.error("Chain config not found for watcher: " + watcher.getClass().getName());
                }

                for (String chain : chainList) {
                    // watcher当前扫块的链
                    watcher.setCurrentChain(chain);

                    JobDataMap map = new JobDataMap();
                    map.put("watcher", watcher);

                    JobDetail jobDetail = JobBuilder
                            .newJob(BlockChainScanJob.class)
                            .withIdentity(watcher.getClass().getName() + "_Job")
                            .setJobData(map)
                            .build();

                    CronTrigger trigger = TriggerBuilder
                            .newTrigger()
                            .withIdentity(watcher.getClass().getSimpleName() + "_Trigger")
                            .withSchedule(CronScheduleBuilder.cronSchedule(watcher.getCron()))
                            .build();

                    scheduler.scheduleJob(jobDetail, trigger);
                }
            }

            scheduler.start();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static List<IWatcher> listWatcher() {
        // 通过JAVA SPI机制加载所有的watcher
        ServiceLoader<IWatcher> watcherList = ServiceLoader.load(IWatcher.class);
        return watcherList == null ? Lists.newArrayList() : Lists.newArrayList(watcherList);
    }

}