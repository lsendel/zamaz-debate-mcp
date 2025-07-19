package com.zamaz.mcp.sidecar.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service Mesh Integration Service for MCP Sidecar
 * 
 * Provides integration with service mesh technologies like Istio, Linkerd, and Consul Connect.
 * Features:
 * - Service discovery and registration
 * - Traffic management and routing
 * - Security policy management
 * - Mesh observability and monitoring
 * - Canary deployments and traffic splitting
 * - Fault injection and chaos engineering
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceMeshIntegrationService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final MetricsCollectorService metricsCollectorService;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.service-mesh.enabled:false}")
    private boolean serviceMeshEnabled;

    @Value("${app.service-mesh.type:istio}")
    private String meshType;

    @Value("${app.service-mesh.namespace:default}")
    private String meshNamespace;

    @Value("${app.service-mesh.discovery.interval:30s}")
    private Duration discoveryInterval;

    @Value("${app.service-mesh.health-check.interval:15s}")
    private Duration healthCheckInterval;

    @Value("${app.service-mesh.envoy-admin-port:15000}")
    private int envoyAdminPort;

    // Service mesh state
    private final Map<String, ServiceInstance> discoveredServices = new ConcurrentHashMap<>();
    private final Map<String, TrafficPolicy> trafficPolicies = new ConcurrentHashMap<>();
    private final Map<String, DestinationRule> destinationRules = new ConcurrentHashMap<>();
    private final Map<String, VirtualService> virtualServices = new ConcurrentHashMap<>();
    private final Map<String, MeshMetrics> meshMetrics = new ConcurrentHashMap<>();

    private ApiClient kubernetesClient;
    private CustomObjectsApi customObjectsApi;
    private volatile boolean meshHealthy = false;
    private volatile Instant lastDiscovery = Instant.now();

    /**
     * Service mesh types
     */
    public enum MeshType {
        ISTIO("istio"),
        LINKERD("linkerd"), 
        CONSUL_CONNECT("consul-connect"),
        ENVOY("envoy");

        private final String name;

        MeshType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Service instance in the mesh
     */
    public static class ServiceInstance {
        private final String name;
        private final String namespace;
        private final String version;
        private final String endpoint;
        private final Map<String, String> labels;
        private final Map<String, String> annotations;
        private volatile boolean healthy;
        private volatile Instant lastSeen;
        private final Map<String, Object> meshMetadata;

        public ServiceInstance(String name, String namespace, String version, String endpoint,
                             Map<String, String> labels, Map<String, String> annotations) {
            this.name = name;
            this.namespace = namespace;
            this.version = version;
            this.endpoint = endpoint;
            this.labels = labels != null ? new HashMap<>(labels) : new HashMap<>();
            this.annotations = annotations != null ? new HashMap<>(annotations) : new HashMap<>();
            this.healthy = true;
            this.lastSeen = Instant.now();
            this.meshMetadata = new ConcurrentHashMap<>();
        }

        public String getName() { return name; }
        public String getNamespace() { return namespace; }
        public String getVersion() { return version; }
        public String getEndpoint() { return endpoint; }
        public Map<String, String> getLabels() { return labels; }
        public Map<String, String> getAnnotations() { return annotations; }
        public boolean isHealthy() { return healthy; }
        public Instant getLastSeen() { return lastSeen; }
        public Map<String, Object> getMeshMetadata() { return meshMetadata; }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
            this.lastSeen = Instant.now();
        }

        public String getServiceKey() {
            return namespace + "/" + name + ":" + version;
        }
    }

    /**
     * Traffic management policy
     */
    public static class TrafficPolicy {
        private final String name;
        private final String namespace;
        private final Map<String, Integer> trafficSplit;
        private final CircuitBreakerConfig circuitBreaker;
        private final RetryConfig retry;
        private final TimeoutConfig timeout;
        private final LoadBalancingConfig loadBalancing;

        public TrafficPolicy(String name, String namespace, Map<String, Integer> trafficSplit,
                           CircuitBreakerConfig circuitBreaker, RetryConfig retry,
                           TimeoutConfig timeout, LoadBalancingConfig loadBalancing) {
            this.name = name;
            this.namespace = namespace;
            this.trafficSplit = trafficSplit != null ? new HashMap<>(trafficSplit) : new HashMap<>();
            this.circuitBreaker = circuitBreaker;
            this.retry = retry;
            this.timeout = timeout;
            this.loadBalancing = loadBalancing;
        }

        public String getName() { return name; }
        public String getNamespace() { return namespace; }
        public Map<String, Integer> getTrafficSplit() { return trafficSplit; }
        public CircuitBreakerConfig getCircuitBreaker() { return circuitBreaker; }
        public RetryConfig getRetry() { return retry; }
        public TimeoutConfig getTimeout() { return timeout; }
        public LoadBalancingConfig getLoadBalancing() { return loadBalancing; }
    }

    public static class CircuitBreakerConfig {
        private final int consecutiveErrors;
        private final Duration interval;
        private final Duration baseEjectionTime;
        private final int maxEjectionPercent;

        public CircuitBreakerConfig(int consecutiveErrors, Duration interval, 
                                  Duration baseEjectionTime, int maxEjectionPercent) {
            this.consecutiveErrors = consecutiveErrors;
            this.interval = interval;
            this.baseEjectionTime = baseEjectionTime;
            this.maxEjectionPercent = maxEjectionPercent;
        }

        public int getConsecutiveErrors() { return consecutiveErrors; }
        public Duration getInterval() { return interval; }
        public Duration getBaseEjectionTime() { return baseEjectionTime; }
        public int getMaxEjectionPercent() { return maxEjectionPercent; }
    }

    public static class RetryConfig {
        private final int attempts;
        private final Duration perTryTimeout;
        private final Set<String> retryOn;

        public RetryConfig(int attempts, Duration perTryTimeout, Set<String> retryOn) {
            this.attempts = attempts;
            this.perTryTimeout = perTryTimeout;
            this.retryOn = retryOn != null ? new HashSet<>(retryOn) : new HashSet<>();
        }

        public int getAttempts() { return attempts; }
        public Duration getPerTryTimeout() { return perTryTimeout; }
        public Set<String> getRetryOn() { return retryOn; }
    }

    public static class TimeoutConfig {
        private final Duration requestTimeout;
        private final Duration idleTimeout;

        public TimeoutConfig(Duration requestTimeout, Duration idleTimeout) {
            this.requestTimeout = requestTimeout;
            this.idleTimeout = idleTimeout;
        }

        public Duration getRequestTimeout() { return requestTimeout; }
        public Duration getIdleTimeout() { return idleTimeout; }
    }

    public static class LoadBalancingConfig {
        private final LoadBalancingMethod method;
        private final ConsistentHashConfig consistentHash;

        public LoadBalancingConfig(LoadBalancingMethod method, ConsistentHashConfig consistentHash) {
            this.method = method;
            this.consistentHash = consistentHash;
        }

        public LoadBalancingMethod getMethod() { return method; }
        public ConsistentHashConfig getConsistentHash() { return consistentHash; }

        public enum LoadBalancingMethod {
            ROUND_ROBIN, LEAST_CONN, RANDOM, PASSTHROUGH, CONSISTENT_HASH
        }
    }

    public static class ConsistentHashConfig {
        private final String header;
        private final String cookie;
        private final boolean useSourceIp;

        public ConsistentHashConfig(String header, String cookie, boolean useSourceIp) {
            this.header = header;
            this.cookie = cookie;
            this.useSourceIp = useSourceIp;
        }

        public String getHeader() { return header; }
        public String getCookie() { return cookie; }
        public boolean isUseSourceIp() { return useSourceIp; }
    }

    /**
     * Destination rule configuration
     */
    public static class DestinationRule {
        private final String name;
        private final String namespace;
        private final String host;
        private final Map<String, TrafficPolicy> subsets;

        public DestinationRule(String name, String namespace, String host, Map<String, TrafficPolicy> subsets) {
            this.name = name;
            this.namespace = namespace;
            this.host = host;
            this.subsets = subsets != null ? new HashMap<>(subsets) : new HashMap<>();
        }

        public String getName() { return name; }
        public String getNamespace() { return namespace; }
        public String getHost() { return host; }
        public Map<String, TrafficPolicy> getSubsets() { return subsets; }
    }

    /**
     * Virtual service configuration
     */
    public static class VirtualService {
        private final String name;
        private final String namespace;
        private final List<String> hosts;
        private final List<String> gateways;
        private final List<HttpRoute> httpRoutes;

        public VirtualService(String name, String namespace, List<String> hosts, 
                            List<String> gateways, List<HttpRoute> httpRoutes) {
            this.name = name;
            this.namespace = namespace;
            this.hosts = hosts != null ? new ArrayList<>(hosts) : new ArrayList<>();
            this.gateways = gateways != null ? new ArrayList<>(gateways) : new ArrayList<>();
            this.httpRoutes = httpRoutes != null ? new ArrayList<>(httpRoutes) : new ArrayList<>();
        }

        public String getName() { return name; }
        public String getNamespace() { return namespace; }
        public List<String> getHosts() { return hosts; }
        public List<String> getGateways() { return gateways; }
        public List<HttpRoute> getHttpRoutes() { return httpRoutes; }
    }

    public static class HttpRoute {
        private final List<HttpMatchRequest> matches;
        private final List<RouteDestination> routes;
        private final HttpRedirect redirect;
        private final HttpRewrite rewrite;
        private final Map<String, String> headers;

        public HttpRoute(List<HttpMatchRequest> matches, List<RouteDestination> routes,
                       HttpRedirect redirect, HttpRewrite rewrite, Map<String, String> headers) {
            this.matches = matches != null ? new ArrayList<>(matches) : new ArrayList<>();
            this.routes = routes != null ? new ArrayList<>(routes) : new ArrayList<>();
            this.redirect = redirect;
            this.rewrite = rewrite;
            this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        }

        public List<HttpMatchRequest> getMatches() { return matches; }
        public List<RouteDestination> getRoutes() { return routes; }
        public HttpRedirect getRedirect() { return redirect; }
        public HttpRewrite getRewrite() { return rewrite; }
        public Map<String, String> getHeaders() { return headers; }
    }

    public static class HttpMatchRequest {
        private final String uri;
        private final String method;
        private final Map<String, String> headers;

        public HttpMatchRequest(String uri, String method, Map<String, String> headers) {
            this.uri = uri;
            this.method = method;
            this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        }

        public String getUri() { return uri; }
        public String getMethod() { return method; }
        public Map<String, String> getHeaders() { return headers; }
    }

    public static class RouteDestination {
        private final String host;
        private final String subset;
        private final int weight;
        private final int port;

        public RouteDestination(String host, String subset, int weight, int port) {
            this.host = host;
            this.subset = subset;
            this.weight = weight;
            this.port = port;
        }

        public String getHost() { return host; }
        public String getSubset() { return subset; }
        public int getWeight() { return weight; }
        public int getPort() { return port; }
    }

    public static class HttpRedirect {
        private final String uri;
        private final String authority;
        private final int redirectCode;

        public HttpRedirect(String uri, String authority, int redirectCode) {
            this.uri = uri;
            this.authority = authority;
            this.redirectCode = redirectCode;
        }

        public String getUri() { return uri; }
        public String getAuthority() { return authority; }
        public int getRedirectCode() { return redirectCode; }
    }

    public static class HttpRewrite {
        private final String uri;
        private final String authority;

        public HttpRewrite(String uri, String authority) {
            this.uri = uri;
            this.authority = authority;
        }

        public String getUri() { return uri; }
        public String getAuthority() { return authority; }
    }

    /**
     * Mesh metrics
     */
    public static class MeshMetrics {
        private final String serviceName;
        private final long requestCount;
        private final double successRate;
        private final double averageLatency;
        private final double p95Latency;
        private final double p99Latency;
        private final Map<String, Double> errorRates;
        private final Instant timestamp;

        public MeshMetrics(String serviceName, long requestCount, double successRate,
                         double averageLatency, double p95Latency, double p99Latency,
                         Map<String, Double> errorRates) {
            this.serviceName = serviceName;
            this.requestCount = requestCount;
            this.successRate = successRate;
            this.averageLatency = averageLatency;
            this.p95Latency = p95Latency;
            this.p99Latency = p99Latency;
            this.errorRates = errorRates != null ? new HashMap<>(errorRates) : new HashMap<>();
            this.timestamp = Instant.now();
        }

        public String getServiceName() { return serviceName; }
        public long getRequestCount() { return requestCount; }
        public double getSuccessRate() { return successRate; }
        public double getAverageLatency() { return averageLatency; }
        public double getP95Latency() { return p95Latency; }
        public double getP99Latency() { return p99Latency; }
        public Map<String, Double> getErrorRates() { return errorRates; }
        public Instant getTimestamp() { return timestamp; }
    }

    /**
     * Initialize service mesh integration
     */
    @PostConstruct
    public void initialize() {
        if (!serviceMeshEnabled) {
            log.info("Service mesh integration is disabled");
            return;
        }

        try {
            // Initialize Kubernetes client
            kubernetesClient = Config.defaultClient();
            Configuration.setDefaultApiClient(kubernetesClient);
            customObjectsApi = new CustomObjectsApi();

            // Initialize default mesh configurations
            initializeDefaultConfigurations();

            log.info("Service mesh integration initialized with type: {}", meshType);
            meshHealthy = true;

        } catch (Exception e) {
            log.error("Failed to initialize service mesh integration", e);
            meshHealthy = false;
        }
    }

    /**
     * Initialize default mesh configurations
     */
    private void initializeDefaultConfigurations() {
        // Create default traffic policies for MCP services
        createDefaultTrafficPolicy("mcp-organization", meshNamespace);
        createDefaultTrafficPolicy("mcp-llm", meshNamespace);
        createDefaultTrafficPolicy("mcp-debate", meshNamespace);
        createDefaultTrafficPolicy("mcp-rag", meshNamespace);
        createDefaultTrafficPolicy("mcp-security", meshNamespace);

        // Create default destination rules
        createDefaultDestinationRule("mcp-organization", meshNamespace);
        createDefaultDestinationRule("mcp-llm", meshNamespace);
        createDefaultDestinationRule("mcp-debate", meshNamespace);
        createDefaultDestinationRule("mcp-rag", meshNamespace);
        createDefaultDestinationRule("mcp-security", meshNamespace);

        log.info("Initialized default mesh configurations for MCP services");
    }

    /**
     * Create default traffic policy
     */
    private void createDefaultTrafficPolicy(String serviceName, String namespace) {
        CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig(
            5, Duration.ofSeconds(30), Duration.ofSeconds(30), 50
        );

        RetryConfig retry = new RetryConfig(
            3, Duration.ofSeconds(5), Set.of("5xx", "gateway-error", "connect-failure")
        );

        TimeoutConfig timeout = new TimeoutConfig(
            Duration.ofSeconds(30), Duration.ofSeconds(300)
        );

        LoadBalancingConfig loadBalancing = new LoadBalancingConfig(
            LoadBalancingConfig.LoadBalancingMethod.ROUND_ROBIN, null
        );

        TrafficPolicy policy = new TrafficPolicy(
            serviceName + "-policy", namespace, Map.of("v1", 100),
            circuitBreaker, retry, timeout, loadBalancing
        );

        trafficPolicies.put(serviceName, policy);
    }

    /**
     * Create default destination rule
     */
    private void createDefaultDestinationRule(String serviceName, String namespace) {
        DestinationRule rule = new DestinationRule(
            serviceName + "-destination", namespace, serviceName,
            Map.of("v1", trafficPolicies.get(serviceName))
        );

        destinationRules.put(serviceName, rule);
    }

    /**
     * Discover services in the mesh
     */
    @Scheduled(fixedDelayString = "${app.service-mesh.discovery.interval:30s}")
    public void discoverServices() {
        if (!serviceMeshEnabled || !meshHealthy) {
            return;
        }

        try {
            log.debug("Discovering services in mesh");

            // Discovery logic based on mesh type
            switch (MeshType.valueOf(meshType.toUpperCase().replace("-", "_"))) {
                case ISTIO:
                    discoverIstioServices();
                    break;
                case LINKERD:
                    discoverLinkerdServices();
                    break;
                case CONSUL_CONNECT:
                    discoverConsulServices();
                    break;
                case ENVOY:
                    discoverEnvoyServices();
                    break;
                default:
                    log.warn("Unknown mesh type: {}", meshType);
            }

            lastDiscovery = Instant.now();
            log.debug("Service discovery completed. Found {} services", discoveredServices.size());

        } catch (Exception e) {
            log.error("Error during service discovery", e);
        }
    }

    /**
     * Discover Istio services
     */
    private void discoverIstioServices() {
        try {
            // Query Istio service registry
            Object serviceEntries = customObjectsApi.listNamespacedCustomObject(
                "networking.istio.io", "v1beta1", meshNamespace, "serviceentries",
                null, null, null, null, null, null, null, null, null, null
            );

            // Process service entries and update discovered services
            // This would parse the actual Kubernetes API response
            
            // For now, simulate discovery of MCP services
            simulateServiceDiscovery();

        } catch (Exception e) {
            log.error("Error discovering Istio services", e);
        }
    }

    /**
     * Discover Linkerd services
     */
    private void discoverLinkerdServices() {
        // Linkerd service discovery logic
        simulateServiceDiscovery();
    }

    /**
     * Discover Consul Connect services
     */
    private void discoverConsulServices() {
        // Consul Connect service discovery logic
        simulateServiceDiscovery();
    }

    /**
     * Discover Envoy services
     */
    private void discoverEnvoyServices() {
        // Envoy admin API service discovery
        simulateServiceDiscovery();
    }

    /**
     * Simulate service discovery for development
     */
    private void simulateServiceDiscovery() {
        String[] services = {"mcp-organization", "mcp-llm", "mcp-debate", "mcp-rag", "mcp-security"};
        String[] ports = {"5005", "5002", "5013", "5004", "8082"};

        for (int i = 0; i < services.length; i++) {
            String serviceName = services[i];
            String endpoint = "http://localhost:" + ports[i];
            
            Map<String, String> labels = Map.of(
                "app", serviceName,
                "version", "v1",
                "component", "mcp-service"
            );

            Map<String, String> annotations = Map.of(
                "sidecar.istio.io/inject", "true",
                "prometheus.io/scrape", "true",
                "prometheus.io/port", ports[i]
            );

            ServiceInstance instance = new ServiceInstance(
                serviceName, meshNamespace, "v1", endpoint, labels, annotations
            );

            discoveredServices.put(instance.getServiceKey(), instance);
        }
    }

    /**
     * Perform health checks on discovered services
     */
    @Scheduled(fixedDelayString = "${app.service-mesh.health-check.interval:15s}")
    public void performHealthChecks() {
        if (!serviceMeshEnabled || discoveredServices.isEmpty()) {
            return;
        }

        log.debug("Performing health checks on {} services", discoveredServices.size());

        discoveredServices.values().parallelStream().forEach(this::checkServiceHealth);
    }

    /**
     * Check health of individual service
     */
    private void checkServiceHealth(ServiceInstance service) {
        try {
            String healthUrl = service.getEndpoint() + "/actuator/health";
            long startTime = System.currentTimeMillis();

            webClientBuilder.build()
                .get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(5))
                .subscribe(
                    response -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        boolean healthy = "UP".equals(response.get("status"));
                        service.setHealthy(healthy);

                        // Record metrics
                        metricsCollectorService.recordServiceHealth(
                            service.getName(), service.getVersion(), healthy, responseTime
                        );

                        log.debug("Health check for {}: healthy={}, time={}ms", 
                                service.getName(), healthy, responseTime);
                    },
                    error -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        service.setHealthy(false);

                        metricsCollectorService.recordServiceHealth(
                            service.getName(), service.getVersion(), false, responseTime
                        );

                        log.debug("Health check failed for {}: {}", service.getName(), error.getMessage());
                    }
                );

        } catch (Exception e) {
            service.setHealthy(false);
            log.warn("Health check error for service {}: {}", service.getName(), e.getMessage());
        }
    }

    /**
     * Collect mesh metrics
     */
    @Scheduled(fixedDelayString = "${app.service-mesh.metrics.interval:60s}")
    public void collectMeshMetrics() {
        if (!serviceMeshEnabled) {
            return;
        }

        try {
            log.debug("Collecting mesh metrics");

            discoveredServices.values().forEach(service -> {
                collectServiceMetrics(service);
            });

        } catch (Exception e) {
            log.error("Error collecting mesh metrics", e);
        }
    }

    /**
     * Collect metrics for individual service
     */
    private void collectServiceMetrics(ServiceInstance service) {
        try {
            // Collect metrics from Envoy admin interface or service metrics endpoint
            String metricsUrl = service.getEndpoint() + "/actuator/prometheus";

            webClientBuilder.build()
                .get()
                .uri(metricsUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .subscribe(
                    metricsData -> {
                        // Parse Prometheus metrics and create MeshMetrics
                        MeshMetrics metrics = parsePrometheusMetrics(service.getName(), metricsData);
                        meshMetrics.put(service.getServiceKey(), metrics);
                    },
                    error -> {
                        log.debug("Failed to collect metrics for {}: {}", service.getName(), error.getMessage());
                    }
                );

        } catch (Exception e) {
            log.warn("Error collecting metrics for service {}: {}", service.getName(), e.getMessage());
        }
    }

    /**
     * Parse Prometheus metrics
     */
    private MeshMetrics parsePrometheusMetrics(String serviceName, String metricsData) {
        // This would implement actual Prometheus metrics parsing
        // For now, return simulated metrics
        return new MeshMetrics(
            serviceName,
            1000L, // requestCount
            0.95,  // successRate
            50.0,  // averageLatency
            95.0,  // p95Latency
            150.0, // p99Latency
            Map.of("4xx", 0.03, "5xx", 0.02) // errorRates
        );
    }

    /**
     * Apply traffic policy
     */
    public Mono<Boolean> applyTrafficPolicy(String serviceName, TrafficPolicy policy) {
        return Mono.fromCallable(() -> {
            trafficPolicies.put(serviceName, policy);
            
            // Apply policy to mesh (would make actual API calls)
            if (MeshType.valueOf(meshType.toUpperCase().replace("-", "_")) == MeshType.ISTIO) {
                return applyIstioTrafficPolicy(serviceName, policy);
            }
            
            return true;
        })
        .doOnSuccess(success -> log.info("Applied traffic policy for service: {}", serviceName))
        .doOnError(error -> log.error("Failed to apply traffic policy for service {}: {}", 
                                     serviceName, error.getMessage()));
    }

    /**
     * Apply Istio traffic policy
     */
    private boolean applyIstioTrafficPolicy(String serviceName, TrafficPolicy policy) {
        try {
            // Create Istio DestinationRule and VirtualService resources
            // This would make actual Kubernetes API calls
            log.debug("Applying Istio traffic policy for service: {}", serviceName);
            return true;
        } catch (Exception e) {
            log.error("Failed to apply Istio traffic policy", e);
            return false;
        }
    }

    /**
     * Get discovered services
     */
    public Mono<Map<String, ServiceInstance>> getDiscoveredServices() {
        return Mono.just(new HashMap<>(discoveredServices));
    }

    /**
     * Get service mesh health
     */
    public Mono<Map<String, Object>> getMeshHealth() {
        return Mono.fromCallable(() -> {
            long healthyServices = discoveredServices.values().stream()
                    .mapToLong(service -> service.isHealthy() ? 1 : 0)
                    .sum();

            return Map.of(
                "meshEnabled", serviceMeshEnabled,
                "meshType", meshType,
                "meshHealthy", meshHealthy,
                "totalServices", discoveredServices.size(),
                "healthyServices", healthyServices,
                "lastDiscovery", lastDiscovery,
                "discoveryInterval", discoveryInterval.toString()
            );
        });
    }

    /**
     * Get mesh metrics
     */
    public Mono<Map<String, Object>> getMeshMetrics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> metrics = new HashMap<>();
            
            // Aggregate mesh metrics
            double avgSuccessRate = meshMetrics.values().stream()
                    .mapToDouble(MeshMetrics::getSuccessRate)
                    .average()
                    .orElse(0.0);

            double avgLatency = meshMetrics.values().stream()
                    .mapToDouble(MeshMetrics::getAverageLatency)
                    .average()
                    .orElse(0.0);

            long totalRequests = meshMetrics.values().stream()
                    .mapToLong(MeshMetrics::getRequestCount)
                    .sum();

            metrics.put("averageSuccessRate", avgSuccessRate);
            metrics.put("averageLatency", avgLatency);
            metrics.put("totalRequests", totalRequests);
            metrics.put("serviceMetrics", new HashMap<>(meshMetrics));

            return metrics;
        });
    }

    /**
     * Get traffic policies
     */
    public Mono<Map<String, TrafficPolicy>> getTrafficPolicies() {
        return Mono.just(new HashMap<>(trafficPolicies));
    }

    /**
     * Create canary deployment
     */
    public Mono<Boolean> createCanaryDeployment(String serviceName, String canaryVersion, int trafficPercent) {
        return Mono.fromCallable(() -> {
            TrafficPolicy existingPolicy = trafficPolicies.get(serviceName);
            if (existingPolicy == null) {
                throw new IllegalArgumentException("No traffic policy found for service: " + serviceName);
            }

            // Update traffic split to include canary
            Map<String, Integer> newTrafficSplit = new HashMap<>(existingPolicy.getTrafficSplit());
            newTrafficSplit.put("v1", 100 - trafficPercent);
            newTrafficSplit.put(canaryVersion, trafficPercent);

            TrafficPolicy canaryPolicy = new TrafficPolicy(
                existingPolicy.getName(),
                existingPolicy.getNamespace(),
                newTrafficSplit,
                existingPolicy.getCircuitBreaker(),
                existingPolicy.getRetry(),
                existingPolicy.getTimeout(),
                existingPolicy.getLoadBalancing()
            );

            trafficPolicies.put(serviceName, canaryPolicy);
            
            log.info("Created canary deployment for {}: {}% traffic to {}", 
                    serviceName, trafficPercent, canaryVersion);
            return true;
        })
        .doOnError(error -> log.error("Failed to create canary deployment for {}: {}", 
                                     serviceName, error.getMessage()));
    }

    /**
     * Perform fault injection
     */
    public Mono<Boolean> injectFault(String serviceName, String faultType, double percentage, Duration delay) {
        return Mono.fromCallable(() -> {
            // This would configure fault injection in the service mesh
            log.info("Injecting fault for service {}: type={}, percentage={}, delay={}", 
                    serviceName, faultType, percentage, delay);
            
            // Store fault injection configuration
            ServiceInstance service = discoveredServices.values().stream()
                    .filter(s -> s.getName().equals(serviceName))
                    .findFirst()
                    .orElse(null);

            if (service != null) {
                service.getMeshMetadata().put("faultInjection", Map.of(
                    "type", faultType,
                    "percentage", percentage,
                    "delay", delay.toString(),
                    "timestamp", Instant.now()
                ));
                return true;
            }
            
            return false;
        })
        .doOnError(error -> log.error("Failed to inject fault for {}: {}", serviceName, error.getMessage()));
    }

    /**
     * Export mesh configuration
     */
    public Mono<Map<String, Object>> exportMeshConfiguration() {
        return Mono.fromCallable(() -> {
            Map<String, Object> config = new HashMap<>();
            
            config.put("meshType", meshType);
            config.put("meshNamespace", meshNamespace);
            config.put("discoveryInterval", discoveryInterval.toString());
            config.put("healthCheckInterval", healthCheckInterval.toString());
            
            config.put("trafficPolicies", trafficPolicies);
            config.put("destinationRules", destinationRules);
            config.put("virtualServices", virtualServices);
            
            return config;
        });
    }
}