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

package ai.everylink.openapi.springboot.starter.plugin.chainscan;

import ai.everylink.openapi.plugin.chainscan.ChainscanPlugin;
import ai.everylink.openapi.plugin.chainscan.service.impl.ChainscanServiceImpl;
import ai.everylink.openapi.plugin.chainscan.service.impl.EtherscanServiceImpl;
import ai.everylink.openapi.plugin.chainscan.util.l2.L2Config;
import ai.everylink.openapi.springboot.starter.plugin.config.L2ChainProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * The type Http client plugin configuration.
 *
 * @author brett
 */
@Configuration
@EnableConfigurationProperties({L2ChainProperties.class})
public class ChainscanPluginConfiguration {

    @Resource
    private L2ChainProperties l2ChainProperties;

    @Bean
    @ConditionalOnMissingBean(value = L2Config.class)
    public L2Config l2Config() {
        L2Config l2Config = new L2Config();
        l2Config.setL2ChainUrl(l2ChainProperties.getChainUrl());
        return l2Config;
    }

    @Bean
    @ConditionalOnMissingBean(value = ChainscanServiceImpl.class)
    public ChainscanServiceImpl chainscanService(final L2Config l2Config) {
        return new ChainscanServiceImpl(l2Config);
    }

    @Bean
    @ConditionalOnMissingBean(value = EtherscanServiceImpl.class)
    public EtherscanServiceImpl etherscanService() {
        return new EtherscanServiceImpl();
    }

    /**
     * Http client properties http client properties.
     *
     * @return the http client properties
     */
    @Bean
    public ChainscanPlugin chainscanPlugin(final ChainscanServiceImpl chainscanService,final EtherscanServiceImpl etherscanService) {
        return new ChainscanPlugin(chainscanService,etherscanService);
    }

}
