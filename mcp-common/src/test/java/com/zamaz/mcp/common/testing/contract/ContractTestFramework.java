package com.zamaz.mcp.common.testing.contract;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Contract testing framework using Pact for verifying service interactions.
 * Supports both consumer and provider contract testing with multi-tenant scenarios.
 */
public class ContractTestFramework {

    /**
     * Consumer contract builder for defining expected interactions.
     */
    public static class ConsumerContractBuilder {
        private final String consumerName;
        private final String providerName;
        private final List<InteractionBuilder> interactions = new ArrayList<>();
        private final Map<String, String> metadata = new HashMap<>();

        public ConsumerContractBuilder(String consumerName, String providerName) {
            this.consumerName = consumerName;
            this.providerName = providerName;
        }

        public InteractionBuilder addInteraction(String description) {
            InteractionBuilder builder = new InteractionBuilder(description);
            interactions.add(builder);
            return builder;
        }

        public ConsumerContractBuilder withMetadata(String key, String value) {
            metadata.put(key, value);
            return this;
        }

        public RequestResponsePact build() {
            PactDslWithProvider pactBuilder = au.com.dius.pact.consumer.dsl.Pact
                .consumer(consumerName)
                .hasPactWith(providerName);

            for (InteractionBuilder interaction : interactions) {
                pactBuilder = interaction.build(pactBuilder);
            }

            return pactBuilder.toPact();
        }
    }

    /**
     * Builder for individual interactions within a contract.
     */
    public static class InteractionBuilder {
        private final String description;
        private String method = "GET";
        private String path;
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, String> queryParams = new HashMap<>();
        private Object requestBody;
        private int responseStatus = 200;
        private final Map<String, String> responseHeaders = new HashMap<>();
        private DslPart responseBody;

        public InteractionBuilder(String description) {
            this.description = description;
        }

        public InteractionBuilder given(String state) {
            // State is handled by Pact framework
            return this;
        }

        public InteractionBuilder uponReceiving(String description) {
            // Description is already set in constructor
            return this;
        }

        public InteractionBuilder method(String method) {
            this.method = method;
            return this;
        }

        public InteractionBuilder get() { return method("GET"); }
        public InteractionBuilder post() { return method("POST"); }
        public InteractionBuilder put() { return method("PUT"); }
        public InteractionBuilder delete() { return method("DELETE"); }
        public InteractionBuilder patch() { return method("PATCH"); }

        public InteractionBuilder path(String path) {
            this.path = path;
            return this;
        }

        public InteractionBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public InteractionBuilder withAuth(String token) {
            return header("Authorization", "Bearer " + token);
        }

        public InteractionBuilder withOrganization(String orgId) {
            return header("X-Organization-Id", orgId);
        }

        public InteractionBuilder withJsonContent() {
            return header("Content-Type", "application/json");
        }

        public InteractionBuilder queryParam(String name, String value) {
            queryParams.put(name, value);
            return this;
        }

        public InteractionBuilder body(Object body) {
            this.requestBody = body;
            return this;
        }

        public InteractionBuilder willRespondWith() {
            // Fluent API marker
            return this;
        }

        public InteractionBuilder status(int status) {
            this.responseStatus = status;
            return this;
        }

        public InteractionBuilder responseHeader(String name, String value) {
            responseHeaders.put(name, value);
            return this;
        }

        public InteractionBuilder responseBody(DslPart body) {
            this.responseBody = body;
            return this;
        }

        public InteractionBuilder responseJsonBody(Consumer<PactDslJsonBody> bodyBuilder) {
            PactDslJsonBody jsonBody = new PactDslJsonBody();
            bodyBuilder.accept(jsonBody);
            this.responseBody = jsonBody;
            return this;
        }

        public InteractionBuilder responseJsonArray(Consumer<PactDslJsonArray> arrayBuilder) {
            PactDslJsonArray jsonArray = new PactDslJsonArray();
            arrayBuilder.accept(jsonArray);
            this.responseBody = jsonArray;
            return this;
        }

        public PactDslWithProvider build(PactDslWithProvider pactBuilder) {
            var interaction = pactBuilder
                .uponReceiving(description)
                .method(method)
                .path(path);

            // Add headers
            headers.forEach(interaction::headers);

            // Add query parameters
            if (!queryParams.isEmpty()) {
                interaction.query(queryParams);
            }

            // Add request body
            if (requestBody != null) {
                if (requestBody instanceof DslPart) {
                    interaction.body((DslPart) requestBody);
                } else {
                    interaction.body(requestBody.toString());
                }
            }

            // Configure response
            var response = interaction
                .willRespondWith()
                .status(responseStatus);

            // Add response headers
            responseHeaders.forEach(response::headers);

            // Add response body
            if (responseBody != null) {
                response.body(responseBody);
            }

            return response;
        }
    }

    /**
     * Common contract patterns for MCP services.
     */
    public static class McpContractPatterns {

        /**
         * Standard health check contract.
         */
        public static InteractionBuilder healthCheck() {
            return new InteractionBuilder("health check")
                .get()
                .path("/health")
                .willRespondWith()
                .status(200)
                .responseJsonBody(body -> body
                    .stringType("status", "UP")
                    .stringType("timestamp")
                    .object("details", details -> details
                        .stringType("database", "UP")
                        .stringType("redis", "UP")
                    )
                );
        }

        /**
         * Standard authentication contract.
         */
        public static InteractionBuilder authenticate(String validToken) {
            return new InteractionBuilder("authenticate user")
                .post()
                .path("/auth/validate")
                .withAuth(validToken)
                .willRespondWith()
                .status(200)
                .responseJsonBody(body -> body
                    .stringType("userId")
                    .stringType("organizationId")
                    .array("roles", roles -> roles.stringType())
                    .array("permissions", perms -> perms.stringType())
                );
        }

        /**
         * Standard organization context contract.
         */
        public static InteractionBuilder organizationContext(String orgId) {
            return new InteractionBuilder("get organization context")
                .get()
                .path("/organizations/" + orgId)
                .withOrganization(orgId)
                .willRespondWith()
                .status(200)
                .responseJsonBody(body -> body
                    .stringType("id", orgId)
                    .stringType("name")
                    .stringType("status", "ACTIVE")
                    .object("subscription", sub -> sub
                        .stringType("tier")
                        .numberType("maxUsers")
                        .array("features", features -> features.stringType())
                    )
                );
        }

        /**
         * Standard CRUD operation contracts.
         */
        public static List<InteractionBuilder> crudOperations(String resourcePath, String resourceName) {
            List<InteractionBuilder> interactions = new ArrayList<>();

            // Create
            interactions.add(new InteractionBuilder("create " + resourceName)
                .post()
                .path(resourcePath)
                .withJsonContent()
                .body(createSampleBody(resourceName))
                .willRespondWith()
                .status(201)
                .responseJsonBody(body -> body
                    .stringType("id")
                    .stringType("status", "CREATED")
                )
            );

            // Read
            interactions.add(new InteractionBuilder("get " + resourceName)
                .get()
                .path(resourcePath + "/1")
                .willRespondWith()
                .status(200)
                .responseJsonBody(body -> body
                    .stringType("id", "1")
                    .stringType("name")
                    .stringType("status")
                )
            );

            // Update
            interactions.add(new InteractionBuilder("update " + resourceName)
                .put()
                .path(resourcePath + "/1")
                .withJsonContent()
                .body(createSampleBody(resourceName))
                .willRespondWith()
                .status(200)
                .responseJsonBody(body -> body
                    .stringType("id", "1")
                    .stringType("status", "UPDATED")
                )
            );

            // Delete
            interactions.add(new InteractionBuilder("delete " + resourceName)
                .delete()
                .path(resourcePath + "/1")
                .willRespondWith()
                .status(204)
            );

            return interactions;
        }

        /**
         * LLM provider contract pattern.
         */
        public static InteractionBuilder llmCompletion() {
            return new InteractionBuilder("generate LLM completion")
                .post()
                .path("/llm/completion")
                .withJsonContent()
                .body(new PactDslJsonBody()
                    .stringType("model", "claude-3-sonnet")
                    .stringType("prompt", "Hello, world!")
                    .numberType("maxTokens", 100)
                    .numberType("temperature", 0.7)
                )
                .willRespondWith()
                .status(200)
                .responseJsonBody(body -> body
                    .stringType("id")
                    .stringType("model", "claude-3-sonnet")
                    .stringType("content")
                    .numberType("tokens")
                    .object("usage", usage -> usage
                        .numberType("promptTokens")
                        .numberType("completionTokens")
                        .numberType("totalTokens")
                    )
                );
        }

        /**
         * Debate service contract pattern.
         */
        public static InteractionBuilder debateOperations() {
            return new InteractionBuilder("create debate")
                .post()
                .path("/debates")
                .withJsonContent()
                .body(new PactDslJsonBody()
                    .stringType("title", "Test Debate")
                    .stringType("description", "A test debate")
                    .array("participants", participants -> participants
                        .object(p -> p
                            .stringType("name", "Claude")
                            .stringType("provider", "anthropic")
                            .stringType("model", "claude-3-sonnet")
                        )
                    )
                    .numberType("rounds", 3)
                )
                .willRespondWith()
                .status(201)
                .responseJsonBody(body -> body
                    .stringType("debateId")
                    .stringType("status", "CREATED")
                    .stringType("title", "Test Debate")
                );
        }

        private static Object createSampleBody(String resourceName) {
            return new PactDslJsonBody()
                .stringType("name", "Test " + resourceName)
                .stringType("description", "A test " + resourceName.toLowerCase());
        }
    }

    /**
     * Multi-tenant contract testing support.
     */
    public static class MultiTenantContracts {

        public static ConsumerContractBuilder forTenant(String consumerName, String providerName, String tenantId) {
            return new ConsumerContractBuilder(consumerName, providerName)
                .withMetadata("tenantId", tenantId);
        }

        /**
         * Tests tenant isolation in contracts.
         */
        public static List<InteractionBuilder> tenantIsolationTests(String basePath) {
            List<InteractionBuilder> interactions = new ArrayList<>();

            // Tenant A can access their data
            interactions.add(new InteractionBuilder("tenant A accesses their data")
                .get()
                .path(basePath + "/tenant-data")
                .withOrganization("tenant-a")
                .willRespondWith()
                .status(200)
                .responseJsonBody(body -> body
                    .stringType("tenantId", "tenant-a")
                    .array("data", data -> data.stringType())
                )
            );

            // Tenant A cannot access tenant B's data
            interactions.add(new InteractionBuilder("tenant A cannot access tenant B data")
                .get()
                .path(basePath + "/tenant-data")
                .withOrganization("tenant-a")
                .header("X-Target-Tenant", "tenant-b")
                .willRespondWith()
                .status(403)
                .responseJsonBody(body -> body
                    .stringType("error", "Access denied")
                    .stringType("code", "TENANT_ACCESS_DENIED")
                )
            );

            return interactions;
        }
    }

    /**
     * Provider verification support.
     */
    public static class ProviderVerification {

        /**
         * Configures provider states for verification.
         */
        public static Map<String, Function<Map<String, Object>, Object>> getStateHandlers() {
            Map<String, Function<Map<String, Object>, Object>> handlers = new HashMap<>();

            handlers.put("user is authenticated", params -> {
                // Setup authenticated user state
                return Map.of("userId", "test-user", "token", "valid-token");
            });

            handlers.put("organization exists", params -> {
                String orgId = (String) params.get("organizationId");
                // Setup organization state
                return Map.of("organizationId", orgId, "status", "ACTIVE");
            });

            handlers.put("debate exists", params -> {
                String debateId = (String) params.get("debateId");
                // Setup debate state
                return Map.of("debateId", debateId, "status", "CREATED");
            });

            handlers.put("LLM provider is available", params -> {
                String provider = (String) params.get("provider");
                // Setup LLM provider state
                return Map.of("provider", provider, "status", "AVAILABLE");
            });

            handlers.put("tenant data exists", params -> {
                String tenantId = (String) params.get("tenantId");
                // Setup tenant data
                return Map.of("tenantId", tenantId, "dataCount", 5);
            });

            return handlers;
        }

        /**
         * Verification configuration for Spring Boot tests.
         */
        public static class SpringBootProviderTest {
            
            @PactTestFor(providerName = "${pact.provider.name:mcp-service}")
            public void pactVerificationTest(PactVerificationContext context) {
                context.verifyInteraction();
            }
        }
    }

    /**
     * Contract test utilities and helpers.
     */
    public static class ContractTestUtils {

        /**
         * Creates a standard service contract with common patterns.
         */
        public static RequestResponsePact createServiceContract(
            String consumerName, 
            String providerName,
            String servicePath
        ) {
            return new ConsumerContractBuilder(consumerName, providerName)
                .addInteraction(McpContractPatterns.healthCheck())
                .addInteraction(McpContractPatterns.authenticate("valid-token"))
                .addInteraction(McpContractPatterns.organizationContext("test-org"))
                .build();
        }

        /**
         * Creates LLM service contract.
         */
        public static RequestResponsePact createLlmContract(String consumerName) {
            return new ConsumerContractBuilder(consumerName, "mcp-llm")
                .addInteraction(McpContractPatterns.healthCheck())
                .addInteraction(McpContractPatterns.llmCompletion())
                .build();
        }

        /**
         * Creates debate service contract.
         */
        public static RequestResponsePact createDebateContract(String consumerName) {
            return new ConsumerContractBuilder(consumerName, "mcp-debate")
                .addInteraction(McpContractPatterns.healthCheck())
                .addInteraction(McpContractPatterns.debateOperations())
                .build();
        }

        /**
         * Creates organization service contract.
         */
        public static RequestResponsePact createOrganizationContract(String consumerName) {
            ConsumerContractBuilder builder = new ConsumerContractBuilder(consumerName, "mcp-organization")
                .addInteraction(McpContractPatterns.healthCheck())
                .addInteraction(McpContractPatterns.organizationContext("test-org"));

            // Add CRUD operations for organizations
            McpContractPatterns.crudOperations("/organizations", "organization")
                .forEach(builder::addInteraction);

            return builder.build();
        }

        /**
         * Validates contract compatibility between versions.
         */
        public static ContractCompatibilityReport validateCompatibility(
            RequestResponsePact oldContract,
            RequestResponsePact newContract
        ) {
            ContractCompatibilityReport report = new ContractCompatibilityReport();

            // Compare interaction counts
            int oldCount = oldContract.getInteractions().size();
            int newCount = newContract.getInteractions().size();
            
            if (newCount < oldCount) {
                report.addIssue("Breaking change: Fewer interactions in new contract");
            }

            // Additional compatibility checks would go here
            // This is a simplified implementation

            return report;
        }
    }

    /**
     * Contract compatibility report.
     */
    public static class ContractCompatibilityReport {
        private final List<String> issues = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addIssue(String issue) {
            issues.add(issue);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isCompatible() {
            return issues.isEmpty();
        }

        public List<String> getIssues() { return issues; }
        public List<String> getWarnings() { return warnings; }
    }
}