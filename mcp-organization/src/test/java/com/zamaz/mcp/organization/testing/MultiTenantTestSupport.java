package com.zamaz.mcp.organization.testing;

import com.zamaz.mcp.organization.domain.model.Organization;
import com.zamaz.mcp.organization.domain.model.User;
import com.zamaz.mcp.security.testing.SecurityTestContext;
import com.zamaz.mcp.security.domain.SecurityContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Support utilities for testing multi-tenant scenarios.
 * Provides isolation testing, tenant switching, and cross-tenant validation.
 */
public class MultiTenantTestSupport {

    private final Map<String, TenantContext> tenantContexts = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> tenantData = new ConcurrentHashMap<>();
    private final List<TenantIsolationRule> isolationRules = new ArrayList<>();
    private TenantContext currentContext;

    /**
     * Creates a new tenant context for testing.
     */
    public TenantContext createTenant(String tenantId) {
        TenantContext context = new TenantContext(tenantId);
        tenantContexts.put(tenantId, context);
        tenantData.put(tenantId, ConcurrentHashMap.newKeySet());
        return context;
    }

    /**
     * Creates a tenant with an organization hierarchy.
     */
    public TenantContext createTenantWithHierarchy(String tenantId, OrganizationTestFixtures.OrganizationHierarchy hierarchy) {
        TenantContext context = createTenant(tenantId);
        context.setHierarchy(hierarchy);
        
        // Register all organization IDs as tenant data
        hierarchy.getOrganizations().forEach(org -> 
            tenantData.get(tenantId).add(org.getId()));
        
        return context;
    }

    /**
     * Switches to a specific tenant context for testing.
     */
    public void switchToTenant(String tenantId) {
        TenantContext context = tenantContexts.get(tenantId);
        if (context == null) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }
        
        this.currentContext = context;
        
        // Set security context
        SecurityTestContext.setContext(context.getSecurityContext());
    }

    /**
     * Executes code within a specific tenant context.
     */
    public <T> T runInTenant(String tenantId, Supplier<T> action) {
        TenantContext previousContext = currentContext;
        try {
            switchToTenant(tenantId);
            return action.get();
        } finally {
            this.currentContext = previousContext;
            if (previousContext != null) {
                SecurityTestContext.setContext(previousContext.getSecurityContext());
            } else {
                SecurityTestContext.clearContext();
            }
        }
    }

    /**
     * Executes code within a specific tenant context (void return).
     */
    public void runInTenant(String tenantId, Runnable action) {
        runInTenant(tenantId, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Tests data isolation between tenants.
     */
    public TenantIsolationReport testDataIsolation() {
        TenantIsolationReport report = new TenantIsolationReport();
        
        // Test each isolation rule
        for (TenantIsolationRule rule : isolationRules) {
            try {
                boolean isolated = rule.test(this);
                report.addRuleResult(rule.getName(), isolated, null);
            } catch (Exception e) {
                report.addRuleResult(rule.getName(), false, e);
            }
        }
        
        // Test cross-tenant data access
        for (String tenant1 : tenantContexts.keySet()) {
            for (String tenant2 : tenantContexts.keySet()) {
                if (!tenant1.equals(tenant2)) {
                    boolean isolated = testCrossTenantAccess(tenant1, tenant2);
                    report.addCrossTenantResult(tenant1, tenant2, isolated);
                }
            }
        }
        
        return report;
    }

    /**
     * Tests tenant switching functionality.
     */
    public TenantSwitchingReport testTenantSwitching() {
        TenantSwitchingReport report = new TenantSwitchingReport();
        
        List<String> tenantIds = new ArrayList<>(tenantContexts.keySet());
        
        for (String fromTenant : tenantIds) {
            for (String toTenant : tenantIds) {
                if (!fromTenant.equals(toTenant)) {
                    try {
                        // Start in fromTenant
                        switchToTenant(fromTenant);
                        String originalData = getCurrentTenantData();
                        
                        // Switch to toTenant
                        switchToTenant(toTenant);
                        String newData = getCurrentTenantData();
                        
                        // Verify context changed
                        boolean switched = !Objects.equals(originalData, newData) &&
                                         getCurrentTenantId().equals(toTenant);
                        
                        report.addSwitchResult(fromTenant, toTenant, switched, null);
                        
                    } catch (Exception e) {
                        report.addSwitchResult(fromTenant, toTenant, false, e);
                    }
                }
            }
        }
        
        return report;
    }

    /**
     * Tests concurrent access across multiple tenants.
     */
    public ConcurrentAccessReport testConcurrentAccess() {
        ConcurrentAccessReport report = new ConcurrentAccessReport();
        
        List<String> tenantIds = new ArrayList<>(tenantContexts.keySet());
        List<Thread> threads = new ArrayList<>();
        Map<String, Exception> errors = new ConcurrentHashMap<>();
        Map<String, String> results = new ConcurrentHashMap<>();
        
        // Create concurrent operations for each tenant
        for (String tenantId : tenantIds) {
            Thread thread = new Thread(() -> {
                try {
                    // Perform operations in this tenant
                    runInTenant(tenantId, () -> {
                        // Simulate some work
                        String data = getCurrentTenantData();
                        Thread.sleep(100); // Simulate processing time
                        results.put(tenantId, data);
                    });
                } catch (Exception e) {
                    errors.put(tenantId, e);
                }
            });
            threads.add(thread);
        }
        
        // Start all threads
        threads.forEach(Thread::start);
        
        // Wait for completion
        threads.forEach(thread -> {
            try {
                thread.join(5000); // 5 second timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                errors.put("join-" + thread.getName(), e);
            }
        });
        
        report.setResults(results);
        report.setErrors(errors);
        
        return report;
    }

    /**
     * Adds a custom isolation rule for testing.
     */
    public void addIsolationRule(TenantIsolationRule rule) {
        isolationRules.add(rule);
    }

    /**
     * Tests performance of tenant operations.
     */
    public TenantPerformanceReport testTenantPerformance() {
        TenantPerformanceReport report = new TenantPerformanceReport();
        
        for (String tenantId : tenantContexts.keySet()) {
            long startTime = System.nanoTime();
            
            // Perform typical operations
            runInTenant(tenantId, () -> {
                // Simulate typical operations
                for (int i = 0; i < 100; i++) {
                    getCurrentTenantData();
                    // Simulate some processing
                }
            });
            
            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1_000_000.0;
            
            report.addPerformanceResult(tenantId, durationMs);
        }
        
        return report;
    }

    // Helper methods

    private boolean testCrossTenantAccess(String tenant1, String tenant2) {
        Set<String> tenant1Data = tenantData.get(tenant1);
        Set<String> tenant2Data = tenantData.get(tenant2);
        
        // Data should be completely isolated
        Set<String> intersection = new HashSet<>(tenant1Data);
        intersection.retainAll(tenant2Data);
        
        return intersection.isEmpty(); // True if properly isolated
    }

    private String getCurrentTenantId() {
        return currentContext != null ? currentContext.getTenantId() : null;
    }

    private String getCurrentTenantData() {
        if (currentContext == null) return "";
        return "tenant:" + currentContext.getTenantId() + ":data:" + 
               tenantData.get(currentContext.getTenantId()).size();
    }

    // Builder methods

    public static MultiTenantTestSupport create() {
        return new MultiTenantTestSupport();
    }

    public MultiTenantTestSupport withDefaultIsolationRules() {
        addIsolationRule(new OrganizationDataIsolationRule());
        addIsolationRule(new UserDataIsolationRule());
        addIsolationRule(new SecurityContextIsolationRule());
        return this;
    }

    // Tenant context class

    public static class TenantContext {
        private final String tenantId;
        private OrganizationTestFixtures.OrganizationHierarchy hierarchy;
        private SecurityContext securityContext;
        private final Map<String, Object> metadata = new HashMap<>();

        public TenantContext(String tenantId) {
            this.tenantId = tenantId;
            this.securityContext = SecurityTestContext.organizationAdmin(tenantId);
        }

        public String getTenantId() { return tenantId; }
        
        public OrganizationTestFixtures.OrganizationHierarchy getHierarchy() { return hierarchy; }
        public void setHierarchy(OrganizationTestFixtures.OrganizationHierarchy hierarchy) { 
            this.hierarchy = hierarchy; 
        }
        
        public SecurityContext getSecurityContext() { return securityContext; }
        public void setSecurityContext(SecurityContext securityContext) { 
            this.securityContext = securityContext; 
        }
        
        public Map<String, Object> getMetadata() { return metadata; }
        
        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }
    }

    // Isolation rule interface and implementations

    public interface TenantIsolationRule {
        String getName();
        boolean test(MultiTenantTestSupport support) throws Exception;
    }

    public static class OrganizationDataIsolationRule implements TenantIsolationRule {
        @Override
        public String getName() {
            return "Organization Data Isolation";
        }

        @Override
        public boolean test(MultiTenantTestSupport support) throws Exception {
            // Test that organization data is properly isolated
            for (String tenant1 : support.tenantContexts.keySet()) {
                for (String tenant2 : support.tenantContexts.keySet()) {
                    if (!tenant1.equals(tenant2)) {
                        TenantContext ctx1 = support.tenantContexts.get(tenant1);
                        TenantContext ctx2 = support.tenantContexts.get(tenant2);
                        
                        if (ctx1.getHierarchy() != null && ctx2.getHierarchy() != null) {
                            // Organizations should be different
                            Set<String> org1Ids = ctx1.getHierarchy().getOrganizations().stream()
                                .map(Organization::getId)
                                .collect(java.util.stream.Collectors.toSet());
                            Set<String> org2Ids = ctx2.getHierarchy().getOrganizations().stream()
                                .map(Organization::getId)
                                .collect(java.util.stream.Collectors.toSet());
                            
                            Set<String> intersection = new HashSet<>(org1Ids);
                            intersection.retainAll(org2Ids);
                            
                            if (!intersection.isEmpty()) {
                                return false; // Found shared organization IDs
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

    public static class UserDataIsolationRule implements TenantIsolationRule {
        @Override
        public String getName() {
            return "User Data Isolation";
        }

        @Override
        public boolean test(MultiTenantTestSupport support) throws Exception {
            // Test that user data is properly isolated
            for (String tenant1 : support.tenantContexts.keySet()) {
                for (String tenant2 : support.tenantContexts.keySet()) {
                    if (!tenant1.equals(tenant2)) {
                        TenantContext ctx1 = support.tenantContexts.get(tenant1);
                        TenantContext ctx2 = support.tenantContexts.get(tenant2);
                        
                        if (ctx1.getHierarchy() != null && ctx2.getHierarchy() != null) {
                            // Users should be different
                            Set<String> user1Ids = ctx1.getHierarchy().getUsers().stream()
                                .map(User::getId)
                                .collect(java.util.stream.Collectors.toSet());
                            Set<String> user2Ids = ctx2.getHierarchy().getUsers().stream()
                                .map(User::getId)
                                .collect(java.util.stream.Collectors.toSet());
                            
                            Set<String> intersection = new HashSet<>(user1Ids);
                            intersection.retainAll(user2Ids);
                            
                            if (!intersection.isEmpty()) {
                                return false; // Found shared user IDs
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

    public static class SecurityContextIsolationRule implements TenantIsolationRule {
        @Override
        public String getName() {
            return "Security Context Isolation";
        }

        @Override
        public boolean test(MultiTenantTestSupport support) throws Exception {
            // Test that security contexts are properly isolated
            for (String tenantId : support.tenantContexts.keySet()) {
                TenantContext context = support.tenantContexts.get(tenantId);
                SecurityContext secCtx = context.getSecurityContext();
                
                if (secCtx != null && !tenantId.equals(secCtx.getOrganizationId())) {
                    return false; // Security context doesn't match tenant
                }
            }
            return true;
        }
    }

    // Report classes

    public static class TenantIsolationReport {
        private final Map<String, RuleResult> ruleResults = new HashMap<>();
        private final Map<String, Map<String, Boolean>> crossTenantResults = new HashMap<>();

        public void addRuleResult(String ruleName, boolean passed, Exception error) {
            ruleResults.put(ruleName, new RuleResult(passed, error));
        }

        public void addCrossTenantResult(String tenant1, String tenant2, boolean isolated) {
            crossTenantResults.computeIfAbsent(tenant1, k -> new HashMap<>())
                .put(tenant2, isolated);
        }

        public boolean allRulesPassed() {
            return ruleResults.values().stream().allMatch(r -> r.passed);
        }

        public boolean allTenantsIsolated() {
            return crossTenantResults.values().stream()
                .flatMap(map -> map.values().stream())
                .allMatch(Boolean::booleanValue);
        }

        public Map<String, RuleResult> getRuleResults() { return ruleResults; }
        public Map<String, Map<String, Boolean>> getCrossTenantResults() { return crossTenantResults; }

        public static class RuleResult {
            public final boolean passed;
            public final Exception error;

            public RuleResult(boolean passed, Exception error) {
                this.passed = passed;
                this.error = error;
            }
        }
    }

    public static class TenantSwitchingReport {
        private final List<SwitchResult> switchResults = new ArrayList<>();

        public void addSwitchResult(String fromTenant, String toTenant, boolean success, Exception error) {
            switchResults.add(new SwitchResult(fromTenant, toTenant, success, error));
        }

        public boolean allSwitchesSuccessful() {
            return switchResults.stream().allMatch(r -> r.success);
        }

        public List<SwitchResult> getSwitchResults() { return switchResults; }

        public static class SwitchResult {
            public final String fromTenant;
            public final String toTenant;
            public final boolean success;
            public final Exception error;

            public SwitchResult(String fromTenant, String toTenant, boolean success, Exception error) {
                this.fromTenant = fromTenant;
                this.toTenant = toTenant;
                this.success = success;
                this.error = error;
            }
        }
    }

    public static class ConcurrentAccessReport {
        private Map<String, String> results = new HashMap<>();
        private Map<String, Exception> errors = new HashMap<>();

        public void setResults(Map<String, String> results) { this.results = results; }
        public void setErrors(Map<String, Exception> errors) { this.errors = errors; }

        public Map<String, String> getResults() { return results; }
        public Map<String, Exception> getErrors() { return errors; }

        public boolean hasErrors() { return !errors.isEmpty(); }
        public int getSuccessCount() { return results.size(); }
        public int getErrorCount() { return errors.size(); }
    }

    public static class TenantPerformanceReport {
        private final Map<String, Double> performanceResults = new HashMap<>();

        public void addPerformanceResult(String tenantId, double durationMs) {
            performanceResults.put(tenantId, durationMs);
        }

        public Map<String, Double> getPerformanceResults() { return performanceResults; }

        public double getAverageDuration() {
            return performanceResults.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        }

        public double getMaxDuration() {
            return performanceResults.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
        }

        public double getMinDuration() {
            return performanceResults.values().stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);
        }
    }
}