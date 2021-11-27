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

import ai.everylink.chainscan.watcher.core.IErc20WatcherPlugin;
import lombok.extern.slf4j.Slf4j;

/**
 * Demo类，演示如何通过SPI机制来成为框架自带的Erc20Watcher的plugin。
 * 1.继承IErc20WatcherPlugin
 * 2.在META-INF/services目录下新建一个名为ai.everylink.chainscan.watcher.core.IErc20WatcherPlugin的文件
 * 3.在文件里面写入每个实现了IErc20WatcherPlugin接口的plugin的全限定名。
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
@Slf4j
public class Erc20SpiPlugin implements IErc20WatcherPlugin {

    @Override
    public boolean processBlock(Object block) {
        log.info("Erc20SpiPlugin处理block：" + block.getClass().getName());
        return true;
    }
}
