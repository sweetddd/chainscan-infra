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

package ai.everylink.chainscan.watcher.core;

import java.util.List;

/**
 * 区块链扫块接口
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
public interface IWatcher {

    /**
     * 扫块定时配置
     *
     * @return cron expression
     */
    String getCron();

    /**
     * 返回有序plugin列表
     * @return
     */
    List<IWatcherPlugin> getOrderedPluginList();

    /**
     * 扫块。
     *
     * @return 区块对象列表
     * @throws WatcherExecutionException 框架负责处理异常，不影响下一次调度。
     */
    <T> List<T> scanBlock() throws WatcherExecutionException;

    /**
     * 支持的链名称(大写)。如EVM、VM
     * @return
     */
    default String supportedChain() {return "EVM";};

    /**
     * watcher调度顺序。
     *
     * @return 调度顺序。值越小优先级越高。
     */
    default int order() {return 1;};
}
