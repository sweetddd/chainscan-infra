package com.everylink.chainscan.watcher.plugin.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vm.secret")
@Data
public class VMSecret {

    private String rpcApi;

    private String rpcSecret;
}