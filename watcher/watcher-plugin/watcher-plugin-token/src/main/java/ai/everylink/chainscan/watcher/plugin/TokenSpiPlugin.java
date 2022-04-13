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
import ai.everylink.chainscan.watcher.core.WatcherExecutionException;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.core.vo.EvmData;
import ai.everylink.chainscan.watcher.plugin.service.impl.TokenInfoServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author brett
 * @since 2021-12-30
 */
@Slf4j
public class TokenSpiPlugin implements IEvmWatcherPlugin {


    private TokenInfoServiceImpl tokenInfoService;

    @Override
    public <T> boolean processBlock(T block) throws WatcherExecutionException {
        initService();
        EvmData blockData = (EvmData) block;
        long    start     = System.currentTimeMillis();
        log.info("TokenSpiPlugin-start:" + start);
        tokenInfoService.tokenScan(blockData);
        log.info("TokenSpiPlugin-end:" + System.currentTimeMillis());
        return true;
    }

    private void initService() {
        if (tokenInfoService == null) {
            tokenInfoService = SpringApplicationUtils.getBean(TokenInfoServiceImpl.class);
        }
    }
}
