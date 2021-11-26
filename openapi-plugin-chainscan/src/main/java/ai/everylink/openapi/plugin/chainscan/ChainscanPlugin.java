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

package ai.everylink.openapi.plugin.chainscan;

import ai.everylink.openapi.common.enums.PluginEnum;
import ai.everylink.openapi.plugin.api.OpenapiPlugin;
import ai.everylink.openapi.plugin.api.OpenapiPluginChain;
import ai.everylink.openapi.plugin.api.utils.WebFluxResultUtils;
import ai.everylink.openapi.plugin.chainscan.service.impl.ChainscanServiceImpl;
import ai.everylink.openapi.plugin.chainscan.service.impl.EtherscanServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * exchange Plugin.
 *
 * @author xiaoyu(Myth)
 */
@Slf4j
public class ChainscanPlugin implements OpenapiPlugin {

    private String chainscanHost    = "xapi.chainscan.powx.io";

    private final ChainscanServiceImpl chainscanService;
    private final EtherscanServiceImpl etherscanService;

    public ChainscanPlugin(ChainscanServiceImpl chainscanService, EtherscanServiceImpl etherscanService) {
        this.chainscanService = chainscanService;
        this.etherscanService = etherscanService;
    }

    @Override
    public Mono<Void> execute(ServerWebExchange exchange, OpenapiPluginChain chain) {
        String host = exchange.getRequest().getURI().getHost();
        log.info("ChainscanPlugin-URL:" + host);
        String action = exchange.getRequest().getQueryParams().getFirst("action");
        if (StringUtils.isNotBlank(action) && host.equals(chainscanHost)) {
        //if (StringUtils.isNotBlank(action)) {
            switch (action) {
                case "balance":
                    return WebFluxResultUtils.result(exchange, etherscanService.balance(exchange));
                case "txlist":
                    return WebFluxResultUtils.result(exchange, etherscanService.txlist(exchange));
                case "balance_layer2":
                    return WebFluxResultUtils.result(exchange, chainscanService.balance(exchange));
                case "txlist_layer2":
                    return WebFluxResultUtils.result(exchange, chainscanService.txlist(exchange));
                default:
                    return chain.execute(exchange);
            }
        }
        return chain.execute(exchange);
    }

    @Override
    public String named() {
        return PluginEnum.CHAINSCAN.getName();
    }

    @Override
    public Boolean skip(final ServerWebExchange exchange) {
        return false;
    }

    @Override
    public int getOrder() {
        return PluginEnum.CHAINSCAN.getCode();
    }

}
