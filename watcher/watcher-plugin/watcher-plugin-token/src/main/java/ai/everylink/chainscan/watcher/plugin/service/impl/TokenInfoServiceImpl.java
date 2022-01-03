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
import ai.everylink.chainscan.watcher.core.util.VM30Utils;
import ai.everylink.chainscan.watcher.core.util.VmChainUtil;
import ai.everylink.chainscan.watcher.dao.TokenAccountBalanceDao;
import ai.everylink.chainscan.watcher.dao.TokenInfoDao;
import ai.everylink.chainscan.watcher.dao.TransactionDao;
import ai.everylink.chainscan.watcher.entity.TokenAccountBalance;
import ai.everylink.chainscan.watcher.entity.TokenInfo;
import ai.everylink.chainscan.watcher.entity.Transaction;
import ai.everylink.chainscan.watcher.plugin.service.TokenInfoService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
public class TokenInfoServiceImpl implements TokenInfoService {

    private Web3j web3j;

    @Value("${cinfigMap.vmChainUrl:}")
    private String vmChainUrl;

    @Autowired
    private VM30Utils vm30Utils;

    @Autowired
    private TransactionDao transactionDao;

    @Autowired
    private TokenInfoDao tokenInfoDao;

    @Autowired
    private TokenAccountBalanceDao tokenAccountBalanceDao;

    @PostConstruct
    private void initWeb3j() {
        if (web3j != null) {
            return ;
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
    public void tokenScan() {
        Transaction txQuery = new Transaction();
        txQuery.setTokenTag(0);
        Example<Transaction> example = Example.of(txQuery);
        List<Transaction>    all     = transactionDao.findAll(example);
        for (Transaction transaction : all) {
            String method = transaction.getInputMethod();
            String value = transaction.getValue();
            String toAddr = transaction.getToAddr();
            String fromAddr = transaction.getFromAddr();
            //交易value为0则为 合约方法调用;
            if(value.equals("0") && StringUtils.isNotBlank(toAddr)){
                addToken(toAddr,fromAddr); //增加合约信息;
            }
            //账户信息余额更新;
            if(method.equals("mint(") || method.equals("transfer(") || method.equals("transferFrom(")
                    || method.equals("burn(") || method.equals("burnFrom(")){
                saveOrUpdateBalance(fromAddr,toAddr); //监控此方法更新用户余额信息;
            }
            transactionDao.updateTokenTag(transaction.getId());
        }
    }


    /**
     * 触发是否 增加token信息;
     * @param toAddr
     */
    private void addToken(String toAddr,String fromAddr) {
        checkTokenType(toAddr);
        String symbol = vm30Utils.symbol(web3j, toAddr).toString();
        String name = vm30Utils.name(web3j, toAddr).toString();
        BigInteger decimals = vm30Utils.decimals(web3j, toAddr);
        if(symbol != null && name != null){
            TokenInfo tokenQuery = new TokenInfo();
            tokenQuery.setTokenName(name);
            tokenQuery.setTokenSymbol(symbol);
            tokenQuery.setDecimals(decimals);
            Example<TokenInfo> exp = Example.of(tokenQuery);
            List<TokenInfo>    tokens = tokenInfoDao.findAll(exp);
            if(tokens.size() < 1){
                //判断合约类型
                checkTokenType(toAddr);
                //增加账户与token关系数据;
                saveOrUpdateBalance(fromAddr,toAddr);
                tokenQuery.setTokenType(1);
                tokenQuery.setAddress(toAddr);
                tokenQuery.setCreateTime(new Date());
                tokenInfoDao.save(tokenQuery);
            }
        }
    }

    /**
     * 增加账户与token关联关系;
     * @param fromAddr
     * @param contract
     */
    private void saveOrUpdateBalance(String fromAddr,String contract) {
        String symbol = vm30Utils.symbol(web3j, contract).toString();
        //查询账户余额
        BigInteger amount = vm30Utils.balanceOf(web3j, contract, fromAddr);
        TokenAccountBalance balance = new TokenAccountBalance();
        balance.setAccount(fromAddr);
        balance.setToken(symbol);
        Example<TokenAccountBalance> exp = Example.of(balance);
        List<TokenAccountBalance>    balances = tokenAccountBalanceDao.findAll(exp);
        if(balances.size() < 1){
            balance.setContract(contract);
            balance.setBalance(amount.longValue());
            tokenAccountBalanceDao.save(balance);
        }else if(balances.size() == 1){
            TokenAccountBalance tokenAccountBalance = balances.get(0);
            tokenAccountBalance.setBalance(amount.longValue());
            //更新账户余额
            tokenAccountBalanceDao.updateBalance(tokenAccountBalance.getId(),amount.longValue());
        }

    }

    /**
     * 校验tokenType
     * @param contract
     */
    private void checkTokenType( String contract) {
        //ERC20:
        //function totalSupply() constant returns (uint totalSupply);
        //function balanceOf(address _owner) constant returns (uint balance);
        //function transfer(address _to, uint _value) returns (bool success);
        //function transferFrom(address _from, address _to, uint _value) returns (bool success);
        //function approve(address _spender, uint _value) returns (bool success);
        //function allowance(address _owner, address _spender) constant returns (uint remaining);

        //ERC721
        //function balanceOf(address _owner) external view returns (uint256);
        //function ownerOf(uint256 _tokenId) external view returns (address);
        //function ownerOf(uint256 _tokenId) external view returns (address);
        //function safeTransferFrom(address _from, address _to, uint256 _tokenId, bytes data) external payable;
        //function safeTransferFrom(address _from, address _to, uint256 _tokenId) external payable;
        //function transferFrom(address _from, address _to, uint256 _tokenId) external payable;
        //function approve(address _approved, uint256 _tokenId) external payable;
        //function setApprovalForAll(address _operator, bool _approved) external;
        //function getApproved(uint256 _tokenId) external view returns (address);
        //function isApprovedForAll(address _owner, address _operator) external view returns (bool);



        //判断为ERC20合约:
        String from =  "0x6Da573EEc80f63c98b88cED15D32CA270787FB5a";
        String to =  "0x6Da573EEc80f63c98b88cED15D32CA270787FB5a";
        String contract1 =  "0xcD99ad44621fa67Ea313AD5E336574eFF1641f11";
        String contract721 =  "0x3bea1B16b5343472D1FE12d941771ff0141b07CC";
        List<Type> parames = new ArrayList<>();
        parames.add(new Address(to));
        parames.add(new Uint256(1));
        try {
            boolean b = vm30Utils.querryFunction(web3j, parames, "transfer", from, contract1);
            parames.clear();
            parames.add(new Address(from));
            parames.add(new Address(to));
           // parames.add(new Uint256(1));
            boolean a = vm30Utils.querryFunction(web3j, parames, "allowance", from, contract1);
            parames.clear();
            boolean c = vm30Utils.querryFunction(web3j, parames, "totalSupply", from, contract1);
            parames.add(new Address(from));
            boolean d = vm30Utils.querryFunction(web3j, parames, "balanceOf", from, contract1);
            parames.clear();
            parames.add(new Address(from));
            parames.add(new Address(to));
             parames.add(new Uint256(1));
            //boolean e = vm30Utils.querryFunction(web3j, parames, "transferFrom", from, contract1);
            BigInteger bigInteger = vm30Utils.totalSupply(web3j, contract1);

            //ERC721
            parames.clear();
            parames.add(new Address(from));
            parames.add(new Address(to));
            parames.add(new Uint256(1));
            boolean f = vm30Utils.querryFunction(web3j, parames, "transferFrom", from, contract721);
            System.out.println(b);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

