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

/**
 * 扫块结果处理
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
public interface IWatcherPlugin {

    /**
     * 处理块信息
     *
     * @param block
     * @return true-处理成功 false-处理失败。框架暂时不处理返回结果。
     * @throws WatcherExecutionException 方法可以选择抛出异常或者自己处理异常(try-catch)。框架目前捕获到异常后只是打印日志。
     */
    <T> boolean processBlock(T block) throws WatcherExecutionException;

    /**
     * 执行顺序。
     *
     * @return 值越小优先级越高。
     */
    default int ordered(){return 1;}
}
