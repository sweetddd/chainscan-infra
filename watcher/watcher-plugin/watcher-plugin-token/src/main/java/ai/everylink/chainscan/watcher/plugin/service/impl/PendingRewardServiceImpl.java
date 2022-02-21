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

import ai.everylink.chainscan.watcher.core.util.VM30Utils;
import ai.everylink.chainscan.watcher.core.util.VmChainUtil;
import ai.everylink.chainscan.watcher.dao.PendingRewardDao;
import ai.everylink.chainscan.watcher.dao.TokenAccountBalanceDao;
import ai.everylink.chainscan.watcher.dao.TokenInfoDao;
import ai.everylink.chainscan.watcher.dao.TransactionDao;
import ai.everylink.chainscan.watcher.entity.PendingReward;
import ai.everylink.chainscan.watcher.entity.TokenAccountBalance;
import ai.everylink.chainscan.watcher.entity.TokenInfo;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.service.PendingRewardService;
import ai.everylink.chainscan.watcher.plugin.service.TokenInfoService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * token统计扫描
 *
 * @author brett
 * @since 2021-12-30
 */
@Slf4j
@Service
public class PendingRewardServiceImpl implements PendingRewardService {

    private Web3j web3j;

    @Value("${watcher.vmChainUrl:}")
    private String vmChainUrl;

    @Value("${coin.pendingRewardCion:}")
    private String pendingRewardCion;

    @Value("${coin.distributionReserveUnit:}")
    private String distributionReserveUnit;

    @Value("${coin.stakingReserveUnit:}")
    private String stakingReserveUnit;

    @Value("${coin.bufferRewardsUnit:}")
    private String bufferRewardsUnit;

    @Autowired
    private VM30Utils vm30Utils;

    @Autowired
    private VmChainUtil vmChainUtil;

    @Autowired
    private PendingRewardDao pendingRewardDao;

    @Autowired
    private TokenInfoDao tokenInfoDao;


    @PostConstruct
    private void initWeb3j() {
        if (web3j != null) {
            return;
        }
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            builder.writeTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            builder.readTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            OkHttpClient httpClient  = builder.build();
            HttpService  httpService = new HttpService(vmChainUrl, httpClient, false);
            web3j = Web3j.build(httpService);
        } catch (Exception e) {
            log.error("初始化web3j异常", e);
        }
    }

    @Override
    public void pendingReward() {
        PendingReward pendingReward = new PendingReward();
        TokenInfo tokenQuery = new TokenInfo();
        tokenQuery.setTokenSymbol(pendingRewardCion);
        Example<TokenInfo> exp    = Example.of(tokenQuery);
        List<TokenInfo>    tokens = tokenInfoDao.findAll(exp);
        if(tokens.size() > 0){
            String contract = tokens.get(0).getAddress();
            //获取MOBI合约交易缓冲
            BigInteger distributionReserve =   vm30Utils.distributionReserve(web3j,contract);
            pendingReward.setCposDistributionReserve(distributionReserve.longValue());
        }
        pendingReward.setDistributionReserveUnit(distributionReserveUnit);
        String pendingRewards = vmChainUtil.getPendingRewards();
        pendingReward.setStakingReserve(Long.valueOf(pendingRewards));
        pendingReward.setStakingReserveUnit(stakingReserveUnit);
        String bufferRewards = vmChainUtil.getBufferRewards();
        pendingReward.setBufferRewards(Long.valueOf(bufferRewards));
        pendingReward.setBufferRewardsUnit(bufferRewardsUnit);
        pendingReward.setCreateTime(new Date());
        pendingReward.setId(1L);
        int size = pendingRewardDao.findAll().size();
        if(size > 0){
             pendingRewardDao.updateById(pendingReward.getCposDistributionReserve(),pendingReward.getStakingReserve(),pendingReward.getBufferRewards(),pendingReward.getId());
        }else {
            pendingRewardDao.save(pendingReward);
        }
    }

    public static void main(String[] args) {

    }

}

