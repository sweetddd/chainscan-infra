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
import ai.everylink.chainscan.watcher.dao.CoinContractDao;
import ai.everylink.chainscan.watcher.dao.CoinDao;
import ai.everylink.chainscan.watcher.entity.CoinContract;
import ai.everylink.chainscan.watcher.plugin.service.SummaryService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.api.etherscan.core.impl.EtherScanApi;
import io.api.etherscan.executor.IHttpExecutor;
import io.api.etherscan.executor.impl.HttpExecutor;
import io.api.etherscan.model.EthNetwork;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

    @Value("${cinfigMap.rinkebyUrl:}")
    private String rinkebyUrl;

    @Value("${cinfigMap.mobiUrl:}")
    private String mobiUrl;

    @Value("${etherScanApi.key:}")
    private String etherScanApiKey;

    private HashMap<Long, Web3j> web3jMap = new HashMap<Long, Web3j>();

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
        List<CoinContract>          all            = coinContractDao.findAll();
        for (Long chainId : chainIds) {
            List<CoinContract> coinContracts = coinContractDao.selectByChainId(chainId);
            Web3j              web3j         = web3jMap.get(chainId);
            for (CoinContract coinContract : coinContracts) {
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
        for (String cionName : totalSupplyMap.keySet()) {
            coinDao.updateTotalSupply(totalSupplyMap.get(cionName), cionName);
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
            List<CoinContract> coinContracts = coinContractDao.selectByName(coinNam);
            for (CoinContract coinContract : coinContracts) {
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
        for (String cionName : totalLockAmountMap.keySet()) {
            coinDao.burntAmount(totalLockAmountMap.get(cionName), cionName);
        }
    }


    @Override
    public void burnt() {
        if (web3jMap.isEmpty()) {
            initWeb3j();
        }
        HashMap<String, BigInteger> totalLockAmountMap = new HashMap<>();
        for (String coinNam : burntCoinNams) {
            List<CoinContract> coinContracts = coinContractDao.selectByName(coinNam);
            for (CoinContract coinContract : coinContracts) {
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
        for (String cionName : totalLockAmountMap.keySet()) {
            coinDao.burntAmount(totalLockAmountMap.get(cionName), cionName);
        }
    }

    @Override
    public void totalLockAmount() {
        if (web3jMap.isEmpty()) {
            initWeb3j();
        }
        HashMap<String, BigInteger> totalLockAmountMap = new HashMap<>();
        for (String coinNam : lockCoinNams) {
            List<CoinContract> coinContracts = coinContractDao.selectByName(coinNam);
            for (CoinContract coinContract : coinContracts) {
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
        for (String cionName : totalLockAmountMap.keySet()) {
            coinDao.updateTotalLockAmount(totalLockAmountMap.get(cionName), cionName);
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
        HttpUtilService httpUtilService = new HttpUtilService(mobiUrl);
        try {
            result = httpUtilService.service("/l2-server-api/api/v0.2/tokens", paramers, header);
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
        String vMpledge = vmChainUtil.getVMpledge();
        System.out.println(vmChainUtil);
    }

    private String getL2Contract() {
        String          contract        = "";
        String          result          = "";
        HttpHeader      header          = new HttpHeader();
        HttpParamers    paramers        = HttpParamers.httpGetParamers();
        HttpUtilService httpUtilService = new HttpUtilService(mobiUrl);
        try {
            result = httpUtilService.service("/l2-server-api/api/v0.2/config", paramers, header);
            JSONObject resultData = JSONObject.parseObject(result);
            if (resultData.get("status").toString().equals("success")) {
                contract = JSONObject.parseObject(resultData.get("result").toString()).get("contract").toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contract;
    }

//    public static void main(String[] args) {
//        Supplier<IHttpExecutor> supplier     = () -> new HttpExecutor(10000);
//        Supplier<IHttpExecutor> supplierFull = () -> new HttpExecutor(10000, 7000);
//        EtherScanApi            etherScanApi = new EtherScanApi(EthNetwork.RINKEBY, supplier);
//        TokenBalance            balance      = etherScanApi.account().balance("0xcb96cb3ad60642f693fc6ebbf5f1a1783c5dc2c9", "0x2322301db10d13e86cc5679d6700dee68be3bf43");
//        String                  s            = DecimalUtil.toDecimal(9, balance.getGwei());
//        System.out.println(balance.getGwei());
//        System.out.println(s);
//    }
}

