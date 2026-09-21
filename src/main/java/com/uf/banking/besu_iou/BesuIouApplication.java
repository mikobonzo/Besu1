package com.uf.banking.besu_iou;

import com.uf.banking.besu_iou.config.BesuProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BesuProperties.class)
public class BesuIouApplication {
    public static void main(String[] args) {
        SpringApplication.run(BesuIouApplication.class, args);
    }
}
