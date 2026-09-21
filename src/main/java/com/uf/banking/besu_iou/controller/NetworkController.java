package com.uf.banking.besu_iou.controller;

import com.uf.banking.besu_iou.dto.NetworkStatusResponse;
import com.uf.banking.besu_iou.service.NetworkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;

@RestController
@RequestMapping("/api/network")
public class NetworkController {
    private final NetworkService service;

    public NetworkController(NetworkService service) { this.service = service; }

    @GetMapping("/status")
    public NetworkStatusResponse status() throws IOException { return service.status(); }
}
