package com.uf.banking.besu_iou.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.math.BigInteger;

@ConfigurationProperties(prefix = "besu")
public record BesuProperties(String rpcUrl, long chainId, String privateKey,
        String contractAddress, String contractBinary, BigInteger gasPrice,
        BigInteger gasLimit, int receiptAttempts, long receiptSleepMillis) {
}
