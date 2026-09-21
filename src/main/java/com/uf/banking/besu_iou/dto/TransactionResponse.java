package com.uf.banking.besu_iou.dto;

public record TransactionResponse(String iouId, String transactionHash,
        String blockNumber, String status) {
}
