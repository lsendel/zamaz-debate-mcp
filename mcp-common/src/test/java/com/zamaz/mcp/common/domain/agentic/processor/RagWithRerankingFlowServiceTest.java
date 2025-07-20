package com.zamaz.mcp.common.domain.agentic.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowResult;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.agentic.PromptContext;
import com.zamaz.mcp.common.domain.llm.LlmResponse;
import com.zamaz.mcp.common.domain.llm.LlmServicePort;
import com.zamaz.mcp.common.domain.rag.Document;
import com.zamaz.mcp.common.domain.rag.RagServicePort;

@ExtendWith(MockitoExtension.class)
class RagWithRerankingFlowServiceTest {

    @Mock
    private LlmServicePort llmService;

    @Mock
    private RagServicePort ragService;

    @InjectMocks
    private RagWithRerankingFlowService ragWithRerankingFlowService;

    private PromptContext context;
    private AgenticFlowConfiguration configuration;
    private List<Document> mockDocuments;

    @BeforeEach
    void setUp() {
        context = new PromptContext("debate-123", "participant-456");

        Map<String, Object> params = new HashMap<>();
        params.put("initial_retrieval_count", 20);
        params.put("final_document_count", 5);
        params.put("reranking_criteria", "relevance to the query, information quality, and recency");
        configuration = new AgenticFlowConfiguration(params);

        // Create mock documents
        mockDocuments = Arrays.asList(
                Document.builder()
                        .id("doc1")
                        .title("Climate Change Overview")
                        .content(
                                "Climate change refers to long-term shifts in global temperatures and weather patterns.")
                        .source("Scientific Journal A")
                        .timestamp(Instant.now())
                        .relevanceScore(0.9f)
                        .build(),
                Document.builder()
                        .id("doc2")
                        .title("Renewable Energy Solutions")
                        .content("Solar and wind energy are key renewable energy sources for combating climate change.")
                        .source("Energy Report B")
                        .timestamp(Instant.now())
                        .relevanceScore(0.8f)
                        .build(),
                Document.builder()
                        .id("doc3")
                        .title("Carbon Footprint Reduction")
                        .content(
                                "Reducing carbon footprint involves changes in transportation, energy use, and consumption.")
                        .source("Environmental Guide C")
                        .timestamp(Instant.now())
                        .relevanceScore(0.7f)
                        .build());
    }

    @Test
    void shouldReturnCorrectFlowType() {
        assertEquals(AgenticFlowType.RAG_WITH_RERANKING, ragWithRerankingFlowService.getFlowType());
    }

    @Test
    void shouldProcessPromptWithRagReranking() {
        // Given
        String prompt = "What are the main causes of climate change?";

        // Mock RAG service to return initial documents
        when(ragService.retrieveDocuments(eq(prompt), eq(20)))
                .thenReturn(mockDocuments);

        // Mock LLM service for reranking
        LlmResponse rerankingResponse = LlmResponse.builder()
                .text("Based on relevance to the query, I rank the documents as: 1,3,2")
                .processingTime(Duration.ofMillis(300))
                .build();

        when(llmService.generate(anyString(), anyMap()))
                .thenReturn(rerankingResponse)
                .thenReturn(LlmResponse.builder()
                        .text("Climate change is primarily caused by greenhouse gas emissions [1]. " +
                                "Key solutions include renewable energy adoption [2] and carbon footprint reduction [3].")
                        .processingTime(Duration.ofMillis(800))
                        .build());

        // When
        AgenticFlowResult result = ragWithRerankingFlowService.process(prompt, configuration, context);

        // Then
        assertNotNull(result);
        assertEquals(prompt, result.getOriginalPrompt());
        assertTrue(result.getEnhancedPrompt().contains(prompt));
        assertTrue(result.getEnhancedPrompt().contains("Climate Change Overview"));
        assertTrue(result.getFinalResponse().contains("greenhouse gas emissions"));
        assertTrue(result.getFinalResponse().contains("[1]"));
        assertEquals(3, result.getProcessingSteps().size());

        // Verify processing steps
        assertEquals("initial_retrieval", result.getProcessingSteps().get(0).getStepType());
        assertEquals("document_reranking", result.getProcessingSteps().get(1).getStepType());
        assertEquals("response_generation", result.getProcessingSteps().get(2).getStepType());

        // Verify metrics
        assertEquals(3, result.getMetrics().get("initial_documents"));
        assertEquals(3, result.getMetrics().get("reranked_documents"));
        assertEquals("rag_reranking", result.getMetrics().get("visualization_type"));
        assertTrue(result.isResponseChanged());
    }

    @Test
    void shouldHandleNoDocumentsFound() {
        // Given
        String prompt = "What are the main causes of climate change?";

        // Mock RAG service to return empty list
        when(ragService.retrieveDocuments(eq(prompt), eq(20)))
                .thenReturn(Arrays.asList());

        // When
        AgenticFlowResult result = ragWithRerankingFlowService.process(prompt, configuration, context);

        // Then
        assertNotNull(result);
        assertEquals(prompt, result.getOriginalPrompt());
        assertTrue(result.getFinalResponse().contains("couldn't find any relevant documents"));
        assertEquals(1, result.getProcessingSteps().size());
        assertEquals("initial_retrieval", result.getProcessingSteps().get(0).getStepType());
        assertEquals(0, result.getMetrics().get("initial_documents"));
        assertEquals(0, result.getMetrics().get("reranked_documents"));
        assertFalse(result.isResponseChanged());
    }

    @Test
    void shouldHandleRerankingWithLimitedDocuments() {
        // Given
        String prompt = "What are renewable energy sources?";
        List<Document> limitedDocs = Arrays.asList(mockDocuments.get(1)); // Only one document

        when(ragService.retrieveDocuments(eq(prompt), eq(20)))
                .thenReturn(limitedDocs);

        // Mock LLM service for reranking
        when(llmService.generate(anyString(), anyMap()))
                .thenReturn(LlmResponse.builder()
                        .text("Only one document available: 1")
                        .processingTime(Duration.ofMillis(200))
                        .build())
                .thenReturn(LlmResponse.builder()
                        .text("Renewable energy sources include solar and wind energy [1].")
                        .processingTime(Duration.ofMillis(600))
                        .build());

        // When
        AgenticFlowResult result = ragWithRerankingFlowService.process(prompt, configuration, context);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getMetrics().get("initial_documents"));
        assertEquals(1, result.getMetrics().get("reranked_documents"));
        assertTrue(result.getFinalResponse().contains("solar and wind energy"));
    }

    @Test
    void shouldValidateConfigurationCorrectly() {
        // Valid configurations
        Map<String, Object> validParams1 = new HashMap<>();
        validParams1.put("initial_retrieval_count", 15);
        validParams1.put("final_document_count", 3);
        validParams1.put("reranking_criteria", "relevance and quality");
        AgenticFlowConfiguration validConfig1 = new AgenticFlowConfiguration(validParams1);

        Map<String, Object> validParams2 = new HashMap<>(); // Empty params should be valid
        AgenticFlowConfiguration validConfig2 = new AgenticFlowConfiguration(validParams2);

        // Invalid configurations
        Map<String, Object> invalidParams1 = new HashMap<>();
        invalidParams1.put("initial_retrieval_count", "not_a_number");
        AgenticFlowConfiguration invalidConfig1 = new AgenticFlowConfiguration(invalidParams1);

        Map<String, Object> invalidParams2 = new HashMap<>();
        invalidParams2.put("initial_retrieval_count", 150); // Too high
        AgenticFlowConfiguration invalidConfig2 = new AgenticFlowConfiguration(invalidParams2);

        Map<String, Object> invalidParams3 = new HashMap<>();
        invalidParams3.put("final_document_count", 0); // Too low
        AgenticFlowConfiguration invalidConfig3 = new AgenticFlowConfiguration(invalidParams3);

        Map<String, Object> invalidParams4 = new HashMap<>();
        invalidParams4.put("reranking_criteria", 123); // Should be string
        AgenticFlowConfiguration invalidConfig4 = new AgenticFlowConfiguration(invalidParams4);

        // Test validations
        assertTrue(ragWithRerankingFlowService.validateConfiguration(validConfig1));
        assertTrue(ragWithRerankingFlowService.validateConfiguration(validConfig2));
        assertFalse(ragWithRerankingFlowService.validateConfiguration(invalidConfig1));
        assertFalse(ragWithRerankingFlowService.validateConfiguration(invalidConfig2));
        assertFalse(ragWithRerankingFlowService.validateConfiguration(invalidConfig3));
        assertFalse(ragWithRerankingFlowService.validateConfiguration(invalidConfig4));
    }

    @Test
    void shouldHandleRerankingParsingErrors() {
        // Given
        String prompt = "What are the main causes of climate change?";

        when(ragService.retrieveDocuments(eq(prompt), eq(20)))
                .thenReturn(mockDocuments);

        // Mock LLM service to return unparseable ranking
        when(llmService.generate(anyString(), anyMap()))
                .thenReturn(LlmResponse.builder()
                        .text("I cannot provide a clear ranking of these documents.")
                        .processingTime(Duration.ofMillis(300))
                        .build())
                .thenReturn(LlmResponse.builder()
                        .text("Based on the available documents, climate change is caused by various factors [1][2][3].")
                        .processingTime(Duration.ofMillis(700))
                        .build());

        // When
        AgenticFlowResult result = ragWithRerankingFlowService.process(prompt, configuration, context);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getMetrics().get("initial_documents"));
        assertEquals(3, result.getMetrics().get("reranked_documents")); // Should fall back to original order
        assertTrue(result.getFinalResponse().contains("climate change"));
    }

    @Test
    void shouldExtractCitationsCorrectly() {
        // Given
        String prompt = "What are renewable energy solutions?";

        when(ragService.retrieveDocuments(eq(prompt), eq(20)))
                .thenReturn(mockDocuments);

        when(llmService.generate(anyString(), anyMap()))
                .thenReturn(LlmResponse.builder()
                        .text("Ranking: 2,1,3")
                        .processingTime(Duration.ofMillis(300))
                        .build())
                .thenReturn(LlmResponse.builder()
                        .text("Renewable energy solutions include solar and wind power [1]. " +
                                "Climate change mitigation requires these technologies [2]. " +
                                "Additional measures include carbon reduction [3].")
                        .processingTime(Duration.ofMillis(700))
                        .build());

        // When
        AgenticFlowResult result = ragWithRerankingFlowService.process(prompt, configuration, context);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getMetrics().get("citations_count"));
        assertTrue(result.getFinalResponse().contains("[1]"));
        assertTrue(result.getFinalResponse().contains("[2]"));
        assertTrue(result.getFinalResponse().contains("[3]"));
    }

    @Test
    void shouldHandleProcessingErrors() {
        // Given
        String prompt = "What are the main causes of climate change?";

        // Mock RAG service to throw exception
        when(ragService.retrieveDocuments(eq(prompt), eq(20)))
                .thenThrow(new RuntimeException("RAG service unavailable"));

        // When
        AgenticFlowResult result = ragWithRerankingFlowService.process(prompt, configuration, context);

        // Then
        assertNotNull(result);
        assertTrue(result.getFinalResponse().contains("Error in RAG with re-ranking"));
        assertTrue(result.getReasoning().contains("Processing failed"));
        assertEquals(Boolean.TRUE, result.getMetrics().get("error"));
        assertFalse(result.isResponseChanged());
    }
}