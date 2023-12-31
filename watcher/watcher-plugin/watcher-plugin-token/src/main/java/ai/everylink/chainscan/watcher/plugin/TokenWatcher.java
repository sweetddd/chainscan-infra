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

import ai.everylink.chainscan.watcher.core.IEvmWatcherPlugin;
import ai.everylink.chainscan.watcher.core.IWatcher;
import ai.everylink.chainscan.watcher.core.IWatcherPlugin;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.service.TokenInfoService;
import ai.everylink.chainscan.watcher.plugin.service.TransactionService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 合约统计发行量
 *
 * @author brett
 * @since 2021-11-26
 */
@Slf4j
public class TokenWatcher implements IWatcher {

    private TokenInfoService tokenService;

    private TransactionService transactionService;

    private TransactionTemplate transactionTemplate;

    @Override
    public List<EvmData> scanBlock() {
        initService();
        long id = System.currentTimeMillis();
        //加载现有数据执行 插件扫描;
        // 2.获取plugin列表
        List<IWatcherPlugin> pluginList = getOrderedPluginList();
        if (CollectionUtils.isEmpty(pluginList)) {
            log.error("No plugins found for watcher [TokenWatcher]");
            return null;
        }

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus transactionStatus) {
                try {
                    List<Transaction> txList = new ArrayList<>();
                    if (onlyEvmPlugin()){
                        txList = transactionService.getTxData();
                    }
                    log.info("watcher=[TokenWatcher],txListSize = " + txList.size());
                    for (IWatcherPlugin plugin : pluginList) {
                        //数据加载插件
                        long start  = System.currentTimeMillis();
                        for (Transaction transaction : txList) {
                            boolean result = plugin.processBlock(transaction);
                            log.info("[{}]Processed block.watcher=[TokenWatcher],plugin=[{}],result={}",
                                    id, plugin.getClass().getSimpleName(), result);
                            // block需要按顺序处理，一个处理失败，后续不能再继续
                            if (!result) {
                                break;
                            }
                        }
                        log.info("Plugin [{}], processing time [{}]", plugin, System.currentTimeMillis() - start);
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                    transactionStatus.setRollbackOnly();
                }
            }
        });
        return Lists.newArrayList();
    }


    @Override
    public List<IWatcherPlugin> getOrderedPluginList() {
        // 自己创建的
        List<IWatcherPlugin> pluginList = Lists.newArrayList();
        // 通过SPI发现的
        pluginList.addAll(findErc20WatcherPluginBySPI());
        // 排序
        Collections.sort(pluginList, (o1, o2) -> o2.ordered() - o1.ordered());
        return pluginList;
    }

    private List<IEvmWatcherPlugin> findErc20WatcherPluginBySPI() {
        ServiceLoader<IEvmWatcherPlugin> list = ServiceLoader.load(IEvmWatcherPlugin.class);
        return list == null ? Lists.newArrayList() : Lists.newArrayList(list);
    }



    @Override
    public void finalizedBlockStatus() {

    }

    @Override
    public String getCron() {
        //return "0 0 * * * ?";
        return "*/5 * * * * ?";
    }

    private void initService() {
        if (tokenService == null) {
            tokenService = SpringApplicationUtils.getBean(TokenInfoService.class);
        }
        if (transactionService == null) {
            transactionService = SpringApplicationUtils.getBean(TransactionService.class);
        }
        if (transactionTemplate == null) {
            transactionTemplate = SpringApplicationUtils.getBean(TransactionTemplate.class);
        }
    }

    public static boolean onlyEvmPlugin() {
        String flag = System.getenv("watcher.process.only.evmplugin");
        if (!StringUtils.isEmpty(flag)) {
            return flag.trim().equalsIgnoreCase("true");
        }

        return false;
    }

}
