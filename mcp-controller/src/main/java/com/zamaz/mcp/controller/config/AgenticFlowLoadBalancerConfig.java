package com.zamaz.mcp.controller.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load balancing configuration for distributing agentic flow processing.
 */
@Configuration
public class AgenticFlowLoadBalancerConfig {
    
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder()
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024)); // 10MB
    }
    
    @Bean
    public ReactorLoadBalancer<ServiceInstance> agenticFlowLoadBalancer(
            LoadBalancerClientFactory clientFactory) {
        
        return new WeightedResponseTimeLoadBalancer(
            clientFactory.getLazyProvider("mcp-llm", ServiceInstanceListSupplier.class)
        );
    }
    
    /**
     * Custom load balancer that considers response time and health.
     */
    public static class WeightedResponseTimeLoadBalancer implements ReactorLoadBalancer<ServiceInstance> {
        
        private final ServiceInstanceListSupplier serviceInstanceListSupplier;
        private final AtomicInteger position = new AtomicInteger(0);
        
        public WeightedResponseTimeLoadBalancer(ServiceInstanceListSupplier serviceInstanceListSupplier) {
            this.serviceInstanceListSupplier = serviceInstanceListSupplier;
        }
        
        @Override
        public Flux<Response<ServiceInstance>> choose(Request request) {
            return serviceInstanceListSupplier.get()
                .next()
                .map(instances -> getInstanceResponse(instances));
        }
        
        private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances) {
            if (instances.isEmpty()) {
                return new EmptyResponse();
            }
            
            // Simple round-robin for now, but can be enhanced with response time metrics
            int pos = Math.abs(position.incrementAndGet());
            ServiceInstance instance = instances.get(pos % instances.size());
            
            return new DefaultResponse(instance);
        }
    }
    
    private static class DefaultResponse implements ReactorLoadBalancer.Response<ServiceInstance> {
        private final ServiceInstance serviceInstance;
        
        public DefaultResponse(ServiceInstance serviceInstance) {
            this.serviceInstance = serviceInstance;
        }
        
        @Override
        public ServiceInstance getServer() {
            return serviceInstance;
        }
    }
    
    private static class EmptyResponse implements ReactorLoadBalancer.Response<ServiceInstance> {
        @Override
        public ServiceInstance getServer() {
            return null;
        }
    }
}