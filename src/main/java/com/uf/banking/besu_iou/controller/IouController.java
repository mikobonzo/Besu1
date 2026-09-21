package com.uf.banking.besu_iou.controller;

import com.uf.banking.besu_iou.dto.IouResponse;
import com.uf.banking.besu_iou.dto.IssueIouRequest;
import com.uf.banking.besu_iou.dto.TransactionResponse;
import com.uf.banking.besu_iou.service.IouService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ious")
public class IouController {
    private final IouService service;

    public IouController(IouService service) { this.service = service; }

    @PostMapping("/deploy")
    public ResponseEntity<TransactionResponse> deploy() throws Exception {
        return ResponseEntity.ok(service.deploy());
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> issue(@Valid @RequestBody IssueIouRequest request)
            throws Exception {
        return ResponseEntity.ok(service.issue(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IouResponse> get(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<TransactionResponse> settle(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(service.settle(id));
    }
}
