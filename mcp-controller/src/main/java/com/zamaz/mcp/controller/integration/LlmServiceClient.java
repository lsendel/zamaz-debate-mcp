package com.zamaz.mcp.controller.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "llm-service", url = "${services.llm.url}")
public interface LlmServiceClient {
    
    @PostMapping("/api/v1/completions")
    Map<String, Object> generateCompletion(@RequestBody Map<String, Object> request);
}