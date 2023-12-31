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

package ai.everylink.chainscan.watcher.core.vo;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.List;
import java.util.Map;

/**
* ERC20单个Block数据，包括Block信息、block包含的transaction列表、每个transaction下的logs列表。
*
* 示例可以参考：
* https://etherscan.io/tx/0x3b7ec6ab8515c249ff2d3624f4ce1e0509706b5b66d9b64c0b80dae5c3857649
*
* @author david.zhang@everylink.ai
* @since 2021-11-26
*/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvmData {

    /**
     * 链id
     */
   private int chainId;

   /**
    * 区块信息
    */
   private EthBlock.Block block;

    /**
     * Block对应的交易列表
     * key: txHash
     * value ： tx list
     */
   private Map<String, TransactionReceipt> txList = Maps.newConcurrentMap();

   /**
    * 交易对应的log列表
    *
    * key: transaction hash
    * value: log list
    */
   private Map<String, List<Log>> transactionLogMap = Maps.newConcurrentMap();

    /**
     * 扩展数据。watcher根据需要添加，各个plugin根据自身的业务需要进行处理。
     */
   private Map<String, Object> extraData = Maps.newConcurrentMap();

}
