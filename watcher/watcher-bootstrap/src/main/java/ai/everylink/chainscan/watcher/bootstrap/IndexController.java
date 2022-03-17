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

package ai.everylink.chainscan.watcher.bootstrap;

import ai.everylink.chainscan.watcher.core.util.OkHttpUtil;
import ai.everylink.chainscan.watcher.plugin.util.HexUtils;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;


/**
 * health check
 *
 * @author david.zhang@everylink.ai
 * @since 2022-03-03
 */
@Controller
public class IndexController {


    @RequestMapping("/healthz")
    @ResponseBody
    public String healthz() {
        return "";
    }

    public static void main(String[] args) throws Exception {



        String code = "08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000001a50726f706f73616c20616c72656164792063616e63656c6c6564000000000000";

        if (code.length() > 135){
            code = code.substring(134);
        }
        String s = HexUtils.toStringHex2(code);
        System.out.println(s);


//        OkHttpClient httpClient = OkHttpUtil.buildOkHttpClient();
//        HttpService httpService = new HttpService("http://vmtest.infra.powx.io", httpClient, false);
////            httpService.addHeader("Authorization", Credentials.basic("", SpringApplicationUtils.getBean(EvmConfig.class).getRinkebyRpcSecret()));
//        Web3j web3j = Web3j.build(httpService);
//
//        EthTransaction send1 = web3j.ethGetTransactionByHash("0x66b61956cab1329edfe6170a34d3df9b3d35e9b24f727cf9301d3adb9c1d2ecd").send();
//        System.out.println(send1);
//
//
//        org.web3j.protocol.core.methods.response.Transaction transaction = send1.getTransaction().get();
//        CallTransaction tr = new CallTransaction(transaction.getFrom()
//                ,transaction.getGasPrice(),transaction.getGas()
//        ,transaction.getTo()
//        ,BigInteger.ZERO
//        ,transaction.getInput());
//        DefaultBlockParameter defaultBlockParameter = DefaultBlockParameter.valueOf(transaction.getBlockNumber());
//
//
//        EthCall send = new Request<>(
//                "eth_call",
//                Arrays.asList(tr, defaultBlockParameter),
//                httpService,
//                org.web3j.protocol.core.methods.response.EthCall.class).send();
//
//        System.out.println(send);
//
//        if (null != send.getError() && !StringUtils.isEmpty(send.getError().getData())){
//            String data = send.getError().getData();
//            System.out.println(data);
//            String s = HexUtils.decode("0x"+data);
//            System.out.println(s);
//
//        }


    }
}


class CallTransaction {

    private String from;
    private String to;
    private BigInteger gas;
    private BigInteger gasPrice;
    private BigInteger value;
    private String data;


    public CallTransaction(
            String from,
            BigInteger gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data) {
        this.from = from;
        this.to = to;
        this.gas = gasLimit;
        this.gasPrice = gasPrice;
        this.value = value;

        if (data != null) {
            this.data = Numeric.prependHexPrefix(data);
        }

    }


    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getGas() {
        return convert(gas);
    }

    public String getGasPrice() {
        return convert(gasPrice);
    }

    public String getValue() {
        return convert(value);
    }

    public String getData() {
        return data;
    }


    private static String convert(BigInteger value) {
        if (value != null) {
            return Numeric.encodeQuantity(value);
        } else {
            return null; // we don't want the field to be encoded if not present
        }
    }
}

