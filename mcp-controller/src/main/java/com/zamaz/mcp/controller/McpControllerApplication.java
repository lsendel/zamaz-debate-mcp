package com.zamaz.mcp.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class McpControllerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpControllerApplication.class, args);
    }
}