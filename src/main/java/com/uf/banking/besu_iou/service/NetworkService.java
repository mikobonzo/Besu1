package com.uf.banking.besu_iou.service;

import com.uf.banking.besu_iou.dto.NetworkStatusResponse;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import java.io.IOException;

@Service
public class NetworkService {
    private final Web3j web3j;

    public NetworkService(Web3j web3j) { this.web3j = web3j; }

    public NetworkStatusResponse status() throws IOException {
        return new NetworkStatusResponse(
                web3j.ethChainId().send().getChainId(),
                web3j.ethBlockNumber().send().getBlockNumber(),
                web3j.netPeerCount().send().getQuantity(),
                web3j.netListening().send().isListening());
    }
}
