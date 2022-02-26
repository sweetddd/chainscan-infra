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

package ai.everylink.chainscan.watcher.plugin.config;

import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


/**
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
@Slf4j
@Service
public class WatcherConfig {

    public WatcherConfig () {
        log.info("[WatcherConfig]constructor called.");
    }

    @Value("${dtx.rpc.api}")
    private String dtxRpcUrl;

    @Value("${dtx.rpc.api:}")
    private String dtxRpcUrlmh;

    @Value("${test.key}")
    private String testKey;

    @Value("${test.key:}")
    private String testKeymh;

    @Value("${test.key2}")
    private String testKey2;

    @Value("${test.key2:}")
    private String testKey2mh;

    @Value("${watcher.L2Url:}")
    private String L2Url;

    public String getDtxRpcUrl() {
        return dtxRpcUrl;
    }

    public void setDtxRpcUrl(String dtxRpcUrl) {
        this.dtxRpcUrl = dtxRpcUrl;
    }

    public String getDtxRpcUrlmh() {
        return dtxRpcUrlmh;
    }

    public void setDtxRpcUrlmh(String dtxRpcUrlmh) {
        this.dtxRpcUrlmh = dtxRpcUrlmh;
    }

    public String getTestKey() {
        return testKey;
    }

    public void setTestKey(String testKey) {
        this.testKey = testKey;
    }

    public String getTestKeymh() {
        return testKeymh;
    }

    public void setTestKeymh(String testKeymh) {
        this.testKeymh = testKeymh;
    }

    public String getTestKey2() {
        return testKey2;
    }

    public void setTestKey2(String testKey2) {
        this.testKey2 = testKey2;
    }

    public String getTestKey2mh() {
        return testKey2mh;
    }

    public void setTestKey2mh(String testKey2mh) {
        log.info("setTestKey2mh called");
        this.testKey2mh = testKey2mh;
    }

    public String getL2Url() {
        return L2Url;
    }

    public void setL2Url(String l2Url) {
        log.info("setL2Url called");
        L2Url = l2Url;
    }

    public void printConfig() {
        log.info("[config_network]dtx.rpc.api=", dtxRpcUrl);
        log.info("[config_network]dtx.rpc.api:=", dtxRpcUrlmh);
        log.info("[config_network]test.key=", testKey);
        log.info("[config_network]test.key:=", testKeymh);
        log.info("[config_network]test.key2=", testKey2);
        log.info("[config_network]test.key2:=", testKey2mh);
        log.info("[config_network]watcher.L2Url:=", L2Url);
    }

}