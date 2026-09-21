package com.uf.banking.besu_iou.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

@Configuration
public class Web3jConfiguration {
    @Bean(destroyMethod = "shutdown")
    Web3j web3j(BesuProperties p) { return Web3j.build(new HttpService(p.rpcUrl())); }

    @Bean
    ContractGasProvider gasProvider(BesuProperties p) {
        return new StaticGasProvider(p.gasPrice(), p.gasLimit());
    }
}
