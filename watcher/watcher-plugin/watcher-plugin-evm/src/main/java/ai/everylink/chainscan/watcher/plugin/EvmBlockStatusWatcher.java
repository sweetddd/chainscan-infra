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
import ai.everylink.chainscan.watcher.plugin.service.EvmDataService;
import com.google.common.collect.Lists;
import com.mysql.cj.xdevapi.SchemaImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * VM链区块打包确认
 *
 * @author david.zhang@everylink.ai
 * @since 2022-01-27
 */
@Component
public class EvmBlockStatusWatcher implements IWatcher {

    private static Logger logger = LoggerFactory.getLogger(EvmBlockStatusWatcher.class);

    /**
     * 产生多少个子块后，可以确认当前区块
     */
    private static final int CHILD_BLOCK_NUM = 12;

    private EvmDataService evmDataService;

    @Override
    public List<Object> scanBlock() {
        init();
        evmDataService.processUnconfirmedVMBlocks(CHILD_BLOCK_NUM);
        return Lists.newArrayList();
    }


    @Override
    public List<IWatcherPlugin> getOrderedPluginList() {
        return Lists.newArrayList();
    }


    @Override
    public void finalizedBlockStatus() {
    }


    @Override
    public String getCron() {
       return "*/30 * * * * ?";
    }

    private void init() {
        initService();
    }

    private void initService() {
        if (evmDataService == null) {
            evmDataService = SpringApplicationUtils.getBean(EvmDataService.class);
        }
    }

}
