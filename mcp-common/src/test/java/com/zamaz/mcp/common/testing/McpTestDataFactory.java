package com.zamaz.mcp.common.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Factory for creating test data for MCP testing scenarios.
 * Provides realistic test data for all MCP services and tools.
 */
@Component
public class McpTestDataFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    /**
     * Create test organization data.
     */
    public Map<String, Object> createOrganizationData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Test Organization " + random.nextInt(1000));
        data.put("description", "Automated test organization for MCP validation");
        return data;
    }

    /**
     * Create test organization update data.
     */
    public Map<String, Object> createOrganizationUpdateData(String organizationId) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", organizationId);
        data.put("name", "Updated Test Org " + random.nextInt(1000));
        data.put("description", "Updated description for test validation");
        return data;
    }

    /**
     * Create test user membership data.
     */
    public Map<String, Object> createUserMembershipData(String organizationId) {
        Map<String, Object> data = new HashMap<>();
        data.put("organizationId", organizationId);
        data.put("userId", "test-user-" + UUID.randomUUID().toString().substring(0, 8));
        data.put("role", random.nextBoolean() ? "member" : "admin");
        return data;
    }

    /**
     * Create test LLM completion data.
     */
    public Map<String, Object> createLlmCompletionData() {
        Map<String, Object> data = new HashMap<>();
        data.put("provider", getRandomProvider());
        data.put("prompt", getRandomPrompt());
        data.put("model", getRandomModel());
        data.put("maxTokens", 100 + random.nextInt(400)); // 100-500 tokens
        data.put("temperature", Math.round((0.1 + random.nextDouble() * 0.9) * 100.0) / 100.0); // 0.1-1.0
        return data;
    }

    /**
     * Create test debate data.
     */
    public Map<String, Object> createDebateData(String organizationId) {
        Map<String, Object> data = new HashMap<>();
        data.put("topic", getRandomDebateTopic());
        data.put("format", getRandomDebateFormat());
        data.put("organizationId", organizationId);
        data.put("participants", createDebateParticipants());
        data.put("maxRounds", 2 + random.nextInt(4)); // 2-5 rounds
        return data;
    }

    /**
     * Create test debate turn data.
     */
    public Map<String, Object> createDebateTurnData(String debateId, String participantId) {
        Map<String, Object> data = new HashMap<>();
        data.put("debateId", debateId);
        data.put("participantId", participantId);
        data.put("content", getRandomDebateArgument());
        return data;
    }

    /**
     * Create test document indexing data.
     */
    public Map<String, Object> createDocumentIndexData(String organizationId) {
        Map<String, Object> data = new HashMap<>();
        data.put("organizationId", organizationId);
        data.put("documentId", "test-doc-" + UUID.randomUUID().toString().substring(0, 8));
        data.put("content", getRandomDocumentContent());
        data.put("metadata", createDocumentMetadata());
        return data;
    }

    /**
     * Create test document search data.
     */
    public Map<String, Object> createDocumentSearchData(String organizationId) {
        Map<String, Object> data = new HashMap<>();
        data.put("organizationId", organizationId);
        data.put("query", getRandomSearchQuery());
        data.put("limit", 5 + random.nextInt(10)); // 5-15 results
        return data;
    }

    /**
     * Create test context data.
     */
    public Map<String, Object> createContextData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Test Context " + random.nextInt(1000));
        data.put("description", "Automated test context for MCP validation");
        return data;
    }

    /**
     * Create test context message data.
     */
    public Map<String, Object> createContextMessageData(String contextId) {
        Map<String, Object> data = new HashMap<>();
        data.put("contextId", contextId);
        data.put("role", random.nextBoolean() ? "user" : "assistant");
        data.put("content", getRandomContextMessage());
        return data;
    }

    /**
     * Create test context window data.
     */
    public Map<String, Object> createContextWindowData(String contextId) {
        Map<String, Object> data = new HashMap<>();
        data.put("contextId", contextId);
        data.put("maxTokens", 1000 + random.nextInt(3000)); // 1000-4000 tokens
        data.put("messageLimit", 10 + random.nextInt(40)); // 10-50 messages
        return data;
    }

    /**
     * Create test context search data.
     */
    public Map<String, Object> createContextSearchData() {
        Map<String, Object> data = new HashMap<>();
        data.put("query", getRandomSearchQuery());
        data.put("page", 0);
        data.put("size", 5 + random.nextInt(15)); // 5-20 results
        return data;
    }

    /**
     * Create test context sharing data.
     */
    public Map<String, Object> createContextSharingData(String contextId) {
        Map<String, Object> data = new HashMap<>();
        data.put("contextId", contextId);
        data.put("targetOrganizationId", "target-org-" + UUID.randomUUID().toString().substring(0, 8));
        data.put("permission", random.nextBoolean() ? "read" : "write");
        return data;
    }

    /**
     * Create test provider status data.
     */
    public Map<String, Object> createProviderStatusData() {
        Map<String, Object> data = new HashMap<>();
        data.put("provider", getRandomProvider());
        return data;
    }

    // Helper methods for generating random test data

    private String getRandomProvider() {
        String[] providers = {"claude", "openai", "gemini", "ollama"};
        return providers[random.nextInt(providers.length)];
    }

    private String getRandomModel() {
        String[] models = {"claude-3-sonnet", "gpt-4", "gemini-pro", "llama2"};
        return models[random.nextInt(models.length)];
    }

    private String getRandomPrompt() {
        String[] prompts = {
            "Write a persuasive argument about renewable energy",
            "Analyze the pros and cons of remote work",
            "Discuss the impact of AI on society",
            "Explain the benefits of sustainable development",
            "Argue for or against universal basic income"
        };
        return prompts[random.nextInt(prompts.length)];
    }

    private String getRandomDebateTopic() {
        String[] topics = {
            "Should artificial intelligence be regulated?",
            "Is remote work better than office work?",
            "Should social media platforms be held responsible for content?",
            "Is universal basic income a viable solution?",
            "Should genetic engineering be allowed in humans?"
        };
        return topics[random.nextInt(topics.length)];
    }

    private String getRandomDebateFormat() {
        String[] formats = {"OXFORD", "LINCOLN_DOUGLAS", "POLICY", "PARLIAMENTARY"};
        return formats[random.nextInt(formats.length)];
    }

    private List<String> createDebateParticipants() {
        List<String> participants = new ArrayList<>();
        participants.add("test-user-" + UUID.randomUUID().toString().substring(0, 8));
        participants.add("test-user-" + UUID.randomUUID().toString().substring(0, 8));
        if (random.nextBoolean()) {
            participants.add("ai-claude");
        }
        return participants;
    }

    private String getRandomDebateArgument() {
        String[] arguments = {
            "Based on current evidence, I believe this position is supported by...",
            "The data clearly shows that we should consider...",
            "From an ethical standpoint, this approach offers...",
            "Historical precedent suggests that...",
            "The economic implications of this decision..."
        };
        return arguments[random.nextInt(arguments.length)] + " [Test argument for validation purposes]";
    }

    private String getRandomDocumentContent() {
        String[] contents = {
            "This is a research paper about artificial intelligence and its impact on society. The study examines various aspects of AI development and deployment.",
            "Climate change research document discussing the latest findings on global warming patterns and environmental impacts.",
            "Economic analysis of remote work trends and their effects on productivity and employee satisfaction.",
            "Medical research paper on the effectiveness of new treatment methodologies in modern healthcare.",
            "Technology report analyzing the future of quantum computing and its potential applications."
        };
        return contents[random.nextInt(contents.length)];
    }

    private Map<String, Object> createDocumentMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "Test Document " + random.nextInt(1000));
        metadata.put("author", "Dr. Test Author");
        metadata.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        metadata.put("category", getRandomCategory());
        metadata.put("source", "automated-test");
        return metadata;
    }

    private String getRandomCategory() {
        String[] categories = {"research", "analysis", "report", "study", "whitepaper"};
        return categories[random.nextInt(categories.length)];
    }

    private String getRandomSearchQuery() {
        String[] queries = {
            "artificial intelligence",
            "climate change impact",
            "remote work benefits",
            "sustainable development",
            "economic policy"
        };
        return queries[random.nextInt(queries.length)];
    }

    private String getRandomContextMessage() {
        String[] messages = {
            "What are your thoughts on this topic?",
            "Can you provide more details about this argument?",
            "I'd like to explore this perspective further.",
            "How does this relate to our previous discussion?",
            "What evidence supports this position?"
        };
        return messages[random.nextInt(messages.length)];
    }

    /**
     * Create test authentication context.
     */
    public Map<String, Object> createTestAuthContext() {
        Map<String, Object> authContext = new HashMap<>();
        authContext.put("userId", "test-user-" + UUID.randomUUID().toString().substring(0, 8));
        authContext.put("organizationId", "test-org-" + UUID.randomUUID().toString().substring(0, 8));
        authContext.put("tier", random.nextBoolean() ? "free" : "pro");
        authContext.put("role", random.nextBoolean() ? "user" : "admin");
        return authContext;
    }

    /**
     * Create test error scenarios for validation.
     */
    public List<Map<String, Object>> createErrorTestScenarios() {
        List<Map<String, Object>> scenarios = new ArrayList<>();
        
        // Invalid parameters
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("scenario", "invalid_parameters");
        invalidParams.put("organizationId", "invalid-uuid");
        scenarios.add(invalidParams);
        
        // Missing required fields
        Map<String, Object> missingFields = new HashMap<>();
        missingFields.put("scenario", "missing_required_fields");
        // Intentionally empty to test validation
        scenarios.add(missingFields);
        
        // Oversized data
        Map<String, Object> oversizedData = new HashMap<>();
        oversizedData.put("scenario", "oversized_data");
        oversizedData.put("content", "x".repeat(100000)); // Very long content
        scenarios.add(oversizedData);
        
        return scenarios;
    }

    /**
     * Convert test data to JSON format.
     */
    public JsonNode toJsonNode(Map<String, Object> data) {
        return objectMapper.valueToTree(data);
    }

    /**
     * Create formatted test data as ObjectNode.
     */
    public ObjectNode createTestDataNode(String type, Map<String, Object> data) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("testType", type);
        node.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        node.set("data", objectMapper.valueToTree(data));
        return node;
    }
}