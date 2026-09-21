package com.uf.banking.besu_iou;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "besu.rpc-url=http://localhost:8545")
class BesuIouApplicationTests {
    @Test
    void contextLoads() {
    }
}
