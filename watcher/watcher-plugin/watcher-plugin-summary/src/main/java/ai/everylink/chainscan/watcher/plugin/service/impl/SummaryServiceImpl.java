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

import ai.everylink.chainscan.watcher.core.util.DecimalUtil;
import ai.everylink.chainscan.watcher.core.util.VM30Utils;
import ai.everylink.chainscan.watcher.core.util.VmChainUtil;
import ai.everylink.chainscan.watcher.core.util.httpUtil.HttpHeader;
import ai.everylink.chainscan.watcher.core.util.httpUtil.HttpParamers;
import ai.everylink.chainscan.watcher.core.util.httpUtil.HttpUtilService;
import ai.everylink.chainscan.watcher.dao.TokenContractDao;
import ai.everylink.chainscan.watcher.dao.TokenDao;
import ai.everylink.chainscan.watcher.entity.TokenContract;
import ai.everylink.chainscan.watcher.plugin.service.SummaryService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.api.etherscan.core.impl.EtherScanApi;
import io.api.etherscan.executor.IHttpExecutor;
import io.api.etherscan.executor.impl.HttpExecutor;
import io.api.etherscan.model.EthNetwork;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 统计发行量
 *
 * @author brett
 * @since 2021-12-14
 */
@Slf4j
@Service
public class SummaryServiceImpl implements SummaryService {

    int connectionTimeout = 10000;
    int readTimeout       = 7000;
    private EtherScanApi api = new EtherScanApi();
    ;

    @Value("#{'${coin.chainIds}'.split(',')}")
    private List<Long> chainIds;

    @Value("#{'${coin.lockCoinNams}'.split(',')}")
    private List<String> lockCoinNams;

    @Value("#{'${coin.burntCoinNams}'.split(',')}")
    private List<String> burntCoinNams;

    @Value("#{'${coin.rewardCoinNams}'.split(',')}")
    private List<String> rewardCoinNams;

    @Value("#{'${coin.web3Urls}'.split(',')}")
    private List<String> web3Urls;

    @Value("${watcher.rinkebyUrl:}")
    private String rinkebyUrl;

    @Value("${watcher.L2Url:}")
    private String L2Url;

    @Value("${coin.L1Symbol:}")
    private String L1Symbol;

    @Value("${etherScanApi.key:}")
    private String etherScanApiKey;

    private HashMap<Long, Web3j> web3jMap = new HashMap<Long, Web3j>();

    @Autowired
    private TokenDao coinDao;

    @Autowired
    private VM30Utils vm30Utils;

    @Autowired
    private VmChainUtil vmChainUtil;


    @Autowired
    private TokenContractDao coinContractDao;

    /**
     * 初始化web3j
     */
    @PostConstruct
    private void initWeb3j() {
        HashMap               web3jMaps = new HashMap<Long, Web3j>();
        HashMap<Long, String> webMap    = new HashMap<>();
        webMap.put(chainIds.get(0), web3Urls.get(0));
        webMap.put(chainIds.get(1), web3Urls.get(1));
        ArrayList<Web3j> web3jsList = new ArrayList<>();
        try {
            for (Long chainId : webMap.keySet()) {
                String               url     = webMap.get(chainId);
                OkHttpClient.Builder builder = new OkHttpClient.Builder();
                builder.connectTimeout(30 * 1000, TimeUnit.MILLISECONDS);
                builder.writeTimeout(30 * 1000, TimeUnit.MILLISECONDS);
                builder.readTimeout(30 * 1000, TimeUnit.MILLISECONDS);
                OkHttpClient httpClient = builder.build();
                Web3j        web3j      = Web3j.build(new HttpService(url, httpClient, false));
                web3jMaps.put(chainId, web3j);
            }
            web3jMap = web3jMaps;
        } catch (Exception e) {
            log.error("初始化web3j异常", e);
        }
    }

    @PostConstruct
    private void initEtherAPI() {
        Supplier<IHttpExecutor> supplier     = () -> new HttpExecutor(connectionTimeout);
        Supplier<IHttpExecutor> supplierFull = () -> new HttpExecutor(connectionTimeout, readTimeout);
        api = new EtherScanApi(EthNetwork.RINKEBY, supplier);
        EtherScanApi apiKovan = new EtherScanApi(etherScanApiKey, EthNetwork.KOVAN, supplier);
    }


    @Override
    public void circulationSuppl() {
        if (web3jMap.isEmpty()) {
            initWeb3j();
        }
        HashMap<String, BigInteger> totalSupplyMap = new HashMap<>();
        List<TokenContract>         all            = coinContractDao.findAll();
        for (Long chainId : chainIds) {
            List<TokenContract> coinContracts = coinContractDao.selectByChainId(chainId);
            Web3j               web3j         = web3jMap.get(chainId);
            for (TokenContract coinContract : coinContracts) {
                BigInteger totalSupply = vm30Utils.totalSupply(web3j, coinContract.getContractAddress());

                if (totalSupplyMap.get(coinContract.getName()) == null) {
                    totalSupplyMap.put(coinContract.getName(), totalSupply);
                } else {
                    BigInteger value = totalSupplyMap.get(coinContract.getName());
                    totalSupplyMap.put(coinContract.getName(), totalSupply.add(value));
                }
                //BigDecimal bigDecimal  = total.movePointLeft(coinContract.getContractDecimals().intValue());
            }
        }
        for (String coinName : totalSupplyMap.keySet()) {
            coinDao.updateTotalSupply(totalSupplyMap.get(coinName), coinName);
        }
    }

    @Override
    public void totalRewards() {
        String storage = vmChainUtil.getStorage(null, "state_subscribeStorage");
    }

    @Override
    public void totalStake() {
    }

    @Override
    public void rewardPool() {
        if (web3jMap.isEmpty()) {
            initWeb3j();
        }
        HashMap<String, BigInteger> totalLockAmountMap = new HashMap<>();
        for (String coinNam : rewardCoinNams) {
            List<TokenContract> coinContracts = coinContractDao.selectByName(coinNam);
            for (TokenContract coinContract : coinContracts) {
                Web3j      web3j           = web3jMap.get(coinContract.getChainId());
                BigInteger totalLockAmount = vm30Utils.burnt(web3j, coinContract.getContractAddress());
                if (totalLockAmountMap.get(coinContract.getName()) == null) {
                    totalLockAmountMap.put(coinContract.getName(), totalLockAmount);
                } else {
                    BigInteger value = totalLockAmountMap.get(coinContract.getName());
                    totalLockAmountMap.put(coinContract.getName(), totalLockAmount.add(value));
                }
            }
        }
        for (String coinName : totalLockAmountMap.keySet()) {
            coinDao.burntAmount(totalLockAmountMap.get(coinName), coinName);
        }
    }


    @Override
    public void burnt() {
        if (web3jMap.isEmpty()) {
            initWeb3j();
        }
        HashMap<String, BigInteger> totalLockAmountMap = new HashMap<>();
        for (String coinNam : burntCoinNams) {
            List<TokenContract> coinContracts = coinContractDao.selectByName(coinNam);
            for (TokenContract coinContract : coinContracts) {
                Web3j      web3j           = web3jMap.get(coinContract.getChainId());
                BigInteger totalLockAmount = vm30Utils.burnt(web3j, coinContract.getContractAddress());
                if (totalLockAmountMap.get(coinContract.getName()) == null) {
                    totalLockAmountMap.put(coinContract.getName(), totalLockAmount);
                } else {
                    BigInteger value = totalLockAmountMap.get(coinContract.getName());
                    totalLockAmountMap.put(coinContract.getName(), totalLockAmount.add(value));
                }
            }
        }
        for (String coinName : totalLockAmountMap.keySet()) {
            coinDao.burntAmount(totalLockAmountMap.get(coinName), coinName);
        }
    }

    @Override
    public void totalLockAmount() {
        if (web3jMap.isEmpty()) {
            initWeb3j();
        }
        HashMap<String, BigInteger> totalLockAmountMap = new HashMap<>();
        for (String coinNam : lockCoinNams) {
            List<TokenContract> coinContracts = coinContractDao.selectByName(coinNam);
            for (TokenContract coinContract : coinContracts) {
                Web3j      web3j           = web3jMap.get(coinContract.getChainId());
                BigInteger totalLockAmount = vm30Utils.totalLockAmount(web3j, coinContract.getContractAddress());
                if (totalLockAmountMap.get(coinContract.getName()) == null) {
                    totalLockAmountMap.put(coinContract.getName(), totalLockAmount);
                } else {
                    BigInteger value = totalLockAmountMap.get(coinContract.getName());
                    totalLockAmountMap.put(coinContract.getName(), totalLockAmount.add(value));
                }
            }
        }
        for (String coinName : totalLockAmountMap.keySet()) {
            coinDao.updateTotalLockAmount(totalLockAmountMap.get(coinName), coinName);
        }
    }

    /**
     * 2层锁定量统计
     */
    @Override
    public void l2LockAmount() {
        String       contract = getL2Contract();
        String       result   = "";
        HttpHeader   header   = new HttpHeader();
        HttpParamers paramers = HttpParamers.httpGetParamers();
        paramers.addParam("from", "latest");
        paramers.addParam("limit", "100");
        paramers.addParam("direction", "older");
        HttpUtilService httpUtilService = new HttpUtilService(L2Url);
        try {
            result = httpUtilService.service("v0.2/tokens", paramers, header);
            JSONObject resultData = JSONObject.parseObject(result);
            if (resultData.get("status").toString().equals("success")) {
                String    list = JSONObject.parseObject(resultData.get("result").toString()).get("list").toString();
                JSONArray data = JSONArray.parseArray(list);
                for (Object item : data) {
                    JSONObject token  = JSONObject.parseObject(item.toString());
                    String     symbol = token.get("symbol").toString();
                    if (symbol.equals("ETH")) {
                        Web3j        web3j           = web3jMap.get(4L);
                        BigInteger l2LockAmount=web3j.ethGetBalance(contract, DefaultBlockParameterName.LATEST).send().getBalance();
                        String       balanceOf       = DecimalUtil.toDecimal(18,l2LockAmount);
                        coinDao.updateL2LockAmount(balanceOf, symbol);
                    } else {
                        String       address         = token.get("address").toString();
                        Web3j        web3j           = web3jMap.get(4L);
                        BigInteger   l2LockAmount = vm30Utils.balanceOf(web3j,address,contract);
                        String       balanceOf       = DecimalUtil.toDecimal(18,l2LockAmount);
                        coinDao.updateL2LockAmount(balanceOf, symbol);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void l1LockAmount() {
        String l1LockAmount = vmChainUtil.getVMpledge();
        if(StringUtils.isNotBlank(l1LockAmount)){
            coinDao.updateL1LockAmount(l1LockAmount, L1Symbol);
        }
        System.out.println(vmChainUtil);
    }

    @Override
    public void erc721() {
        Web3j web3j = null;
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            builder.writeTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            builder.readTimeout(30 * 1000, TimeUnit.MILLISECONDS);
            OkHttpClient httpClient  = builder.build();
            HttpService  httpService = new HttpService("http://rinkeby.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf", httpClient, false);
            web3j = Web3j.build(httpService);
        } catch (Exception e) {
            log.error("初始化web3j异常", e);
        }
        String    symbol  = vm30Utils.symbol(web3j, "0xac430d03BDceCcD49DDD8c06B7772B3b61b26039").toString();
        String    tokenURL  = vm30Utils.tokenURL(web3j, "0xac430d03BDceCcD49DDD8c06B7772B3b61b26039", new BigInteger("1")).toString();
        System.out.println(tokenURL);
    }

    private String getL2Contract() {
        String          contract        = "";
        String          result          = "";
        HttpHeader      header          = new HttpHeader();
        HttpParamers    paramers        = HttpParamers.httpGetParamers();
        HttpUtilService httpUtilService = new HttpUtilService(L2Url);
        try {
            result = httpUtilService.service("v0.2/config", paramers, header);
            JSONObject resultData = JSONObject.parseObject(result);
            if (resultData.get("status").toString().equals("success")) {
                contract = JSONObject.parseObject(resultData.get("result").toString()).get("contract").toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contract;
    }
}

