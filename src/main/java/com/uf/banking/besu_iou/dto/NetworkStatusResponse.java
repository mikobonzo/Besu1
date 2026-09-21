package com.uf.banking.besu_iou.dto;

import java.math.BigInteger;

public record NetworkStatusResponse(BigInteger chainId, BigInteger blockNumber,
        BigInteger peerCount, boolean listening) {
}
