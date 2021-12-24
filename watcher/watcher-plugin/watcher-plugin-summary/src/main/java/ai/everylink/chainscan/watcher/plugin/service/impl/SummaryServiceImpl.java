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

package ai.everylink.chainscan.watcher.plugin.service.impl;

import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.core.util.VmChainUtil;
import ai.everylink.chainscan.watcher.plugin.config.SummaryConfig;
import ai.everylink.chainscan.watcher.plugin.dao.*;
import ai.everylink.chainscan.watcher.plugin.entity.CoinContract;
import ai.everylink.chainscan.watcher.plugin.service.SummaryService;
import ai.everylink.chainscan.watcher.plugin.util.VM30Utils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 统计发行量
 *
 * @author brett
 * @since 2021-12-14
 */
@Slf4j
@Service
public class SummaryServiceImpl implements SummaryService {

    @Value("#{'${cion.chainIds}'.split(',')}")
    private List<Long> chainIds;

    @Value("#{'${cion.lockCoinNams}'.split(',')}")
    private List<String> lockCoinNams;

    @Value("#{'${cion.burntCoinNams}'.split(',')}")
    private List<String> burntCoinNams;

    @Value("#{'${cion.rewardCoinNams}'.split(',')}")
    private List<String> rewardCoinNams;

    @Value("#{'${cion.web3Urls}'.split(',')}")
    private List<String> web3Urls;

    private HashMap<Long,Web3j> web3jMap = new HashMap<Long,Web3j>();

    @Autowired
    private CoinDao coinDao;

    @Autowired
    private VM30Utils vm30Utils;

    @Autowired
    private VmChainUtil vmChainUtil;


    @Autowired
    private CoinContractDao coinContractDao;

    /**
     * 初始化web3j
     */
    @PostConstruct
    private void initWeb3j() {
        HashMap web3jMaps = new HashMap<Long,Web3j>();
        HashMap<Long, String> webMap = new HashMap<>();
        webMap.put(chainIds.get(0),web3Urls.get(0));
        webMap.put(chainIds.get(1),web3Urls.get(1));
        ArrayList<Web3j> web3jsList = new ArrayList<>();
        try {
            for (Long chainId : webMap.keySet()) {
                String url = webMap.get(chainId);
                OkHttpClient.Builder builder = new OkHttpClient.Builder();
                builder.connectTimeout(30 * 1000, TimeUnit.MILLISECONDS);
                builder.writeTimeout(30 * 1000, TimeUnit.MILLISECONDS);
                builder.readTimeout(30 * 1000, TimeUnit.MILLISECONDS);
                OkHttpClient httpClient = builder.build();
                Web3j        web3j      = Web3j.build(new HttpService(url, httpClient, false));
                web3jMaps.put(chainId,web3j);
            }
            web3jMap =  web3jMaps;
        } catch (Exception e) {
            log.error("初始化web3j异常", e);
        }
    }


    @Override
    public void circulationSuppl() {
        if(web3jMap.isEmpty()){
            initWeb3j();
        }
        HashMap<String, BigInteger> totalSupplyMap = new HashMap<>();
        List<CoinContract> all = coinContractDao.findAll();
        for (Long chainId : chainIds) {
            List<CoinContract> coinContracts = coinContractDao.selectByChainId(chainId);
            Web3j    web3j   = web3jMap.get(chainId);
            for (CoinContract coinContract : coinContracts) {
                BigInteger totalSupply = vm30Utils.totalSupply(web3j, coinContract.getContractAddress());

                if(totalSupplyMap.get(coinContract.getName())== null){
                    totalSupplyMap.put(coinContract.getName(),totalSupply);
                }else {
                    BigInteger value = totalSupplyMap.get(coinContract.getName());
                    totalSupplyMap.put(coinContract.getName(),totalSupply.add(value));
                }
                //BigDecimal bigDecimal  = total.movePointLeft(coinContract.getContractDecimals().intValue());
            }
        }
        for (String  cionName: totalSupplyMap.keySet()) {
            coinDao.updateTotalSupply(totalSupplyMap.get(cionName),cionName);
        }
    }

    @Override
    public void totalRewards() {
        String storage = vmChainUtil.getStorage("0xaf9e78df124ddb9027c2573e5fb15e127322f546e497e413366c0e4faa8974c3", "state_subscribeStorage");
    }

    @Override
    public void totalStake() {

    }

    @Override
    public void rewardPool() {
        if(web3jMap.isEmpty()){
            initWeb3j();
        }
        HashMap<String, BigInteger> totalLockAmountMap = new HashMap<>();
        for (String coinNam : rewardCoinNams) {
            List<CoinContract> coinContracts = coinContractDao.selectByName(coinNam);
            for (CoinContract coinContract : coinContracts) {
                Web3j    web3j   = web3jMap.get(coinContract.getChainId());
                BigInteger totalLockAmount = vm30Utils.burnt(web3j, coinContract.getContractAddress());
                if(totalLockAmountMap.get(coinContract.getName())== null){
                    totalLockAmountMap.put(coinContract.getName(),totalLockAmount);
                }else {
                    BigInteger value = totalLockAmountMap.get(coinContract.getName());
                    totalLockAmountMap.put(coinContract.getName(),totalLockAmount.add(value));
                }
            }
        }
        for (String  cionName: totalLockAmountMap.keySet()) {
            coinDao.burntAmount(totalLockAmountMap.get(cionName),cionName);
        }
    }


    @Override
    public void burnt() {
        if(web3jMap.isEmpty()){
            initWeb3j();
        }
        HashMap<String, BigInteger> totalLockAmountMap = new HashMap<>();
        for (String coinNam : burntCoinNams) {
            List<CoinContract> coinContracts = coinContractDao.selectByName(coinNam);
            for (CoinContract coinContract : coinContracts) {
                Web3j    web3j   = web3jMap.get(coinContract.getChainId());
                BigInteger totalLockAmount = vm30Utils.burnt(web3j, coinContract.getContractAddress());
                if(totalLockAmountMap.get(coinContract.getName())== null){
                    totalLockAmountMap.put(coinContract.getName(),totalLockAmount);
                }else {
                    BigInteger value = totalLockAmountMap.get(coinContract.getName());
                    totalLockAmountMap.put(coinContract.getName(),totalLockAmount.add(value));
                }
            }
        }
        for (String  cionName: totalLockAmountMap.keySet()) {
            coinDao.burntAmount(totalLockAmountMap.get(cionName),cionName);
        }
    }

    @Override
    public void totalLockAmount() {
        if(web3jMap.isEmpty()){
            initWeb3j();
        }
        HashMap<String, BigInteger> totalLockAmountMap = new HashMap<>();
        for (String coinNam : lockCoinNams) {
            List<CoinContract> coinContracts = coinContractDao.selectByName(coinNam);
            for (CoinContract coinContract : coinContracts) {
                Web3j    web3j   = web3jMap.get(coinContract.getChainId());
                BigInteger totalLockAmount = vm30Utils.totalLockAmount(web3j, coinContract.getContractAddress());
                if(totalLockAmountMap.get(coinContract.getName())== null){
                    totalLockAmountMap.put(coinContract.getName(),totalLockAmount);
                }else {
                    BigInteger value = totalLockAmountMap.get(coinContract.getName());
                    totalLockAmountMap.put(coinContract.getName(),totalLockAmount.add(value));
                }
            }
        }
        for (String  cionName: totalLockAmountMap.keySet()) {
            coinDao.updateTotalLockAmount(totalLockAmountMap.get(cionName),cionName);
        }
    }
}

