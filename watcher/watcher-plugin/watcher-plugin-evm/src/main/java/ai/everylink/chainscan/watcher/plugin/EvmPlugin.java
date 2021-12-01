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

import ai.everylink.chainscan.watcher.core.IWatcherPlugin;
import ai.everylink.chainscan.watcher.core.WatcherExecutionException;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.plugin.service.EvmDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.DefaultFunctionEncoder;
import org.web3j.abi.TypeDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.web3j.abi.TypeReference;

import java.util.ArrayList;
import java.util.List;

/**
 * ERC20 chain data plugin
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
public class EvmPlugin implements IWatcherPlugin {

    private static Logger logger = LoggerFactory.getLogger(EvmPlugin.class);


    private EvmDataService evmDataService;

    @Override
    public <T> boolean processBlock(T block) throws WatcherExecutionException {
        EvmData blockData = (EvmData) block;
        initService();
        System.out.println("EvmPlugin 处理: " + blockData.getBlock().getNumber()
                                   + "; tx size=" + blockData.getBlock().getTransactions().size());

        EthBlock.Block                   ethBlock     = blockData.getBlock();
        List<EthBlock.TransactionResult> transactions = ethBlock.getTransactions();
        for (EthBlock.TransactionResult transaction : transactions) {
            System.out.println(transaction);
        }

        try {
            evmDataService.saveEvmData(blockData);
        } catch (Exception e) {
            logger.error("Error occured when process block=" + ((EvmData) block).getBlock().getNumber(), e);
            return false;
        }

        return true;
    }

    private void initService() {
        if (evmDataService == null) {
            evmDataService = SpringApplicationUtils.getBean(EvmDataService.class);
        }
    }
}
