package com.zamaz.mcp.common.domain.agentic.processor;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowProcessor;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowResult;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.agentic.ProcessingStep;
import com.zamaz.mcp.common.domain.agentic.PromptContext;
import com.zamaz.mcp.common.domain.llm.LlmResponse;
import com.zamaz.mcp.common.domain.llm.LlmServicePort;
import com.zamaz.mcp.common.domain.rag.Document;
import com.zamaz.mcp.common.domain.rag.RagServicePort;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the RAG with Re-ranking agentic flow processor.
 * Uses a three-step process: initial retrieval, LLM-based re-ranking, and final
 * response generation.
 */
@Service
public class RagWithRerankingFlowService implements AgenticFlowProcessor {

    private final LlmServicePort llmService;
    private final RagServicePort ragService;

    /**
     * Creates a new RagWithRerankingFlowService with the specified services.
     *
     * @param llmService The LLM service to use
     * @param ragService The RAG service for document retrieval
     */
    public RagWithRerankingFlowService(LlmServicePort llmService, RagServicePort ragService) {
        this.llmService = llmService;
        this.ragService = ragService;
    }

    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.RAG_WITH_RERANKING;
    }

    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context) {
        Instant startTime = Instant.now();
        List<ProcessingStep> steps = new ArrayList<>();

        try {
            // Step 1: Initial document retrieval
            int initialRetrievalCount = (Integer) configuration.getParameter("initial_retrieval_count", 20);
            List<Document> initialDocuments = ragService.retrieveDocuments(prompt, initialRetrievalCount);

            steps.add(new ProcessingStep(
                    "initial_retrieval",
                    prompt,
                    String.format("Retrieved %d documents", initialDocuments.size()),
                    createRetrievalMetadata(initialDocuments)));

            if (initialDocuments.isEmpty()) {
                return buildNoDocumentsResult(prompt, steps);
            }

            // Step 2: LLM-based document re-ranking
            int finalDocumentCount = (Integer) configuration.getParameter("final_document_count", 5);
            List<Document> rerankedDocuments = rerankDocuments(
                    prompt,
                    initialDocuments,
                    finalDocumentCount,
                    configuration);

            steps.add(new ProcessingStep(
                    "document_reranking",
                    buildRerankingPrompt(prompt, initialDocuments),
                    String.format("Selected %d most relevant documents", rerankedDocuments.size()),
                    createRerankingMetadata(initialDocuments, rerankedDocuments)));

            // Step 3: Generate final response with selected documents
            String enhancedPrompt = buildEnhancedPrompt(prompt, rerankedDocuments, configuration);
            LlmResponse finalResponse = llmService.generate(enhancedPrompt, configuration.getParameters());

            steps.add(new ProcessingStep(
                    "response_generation",
                    enhancedPrompt,
                    finalResponse.getText(),
                    createResponseMetadata(rerankedDocuments, finalResponse)));

            // Extract citations from response
            Map<String, Document> citations = extractCitations(finalResponse.getText(), rerankedDocuments);

            return AgenticFlowResult.builder()
                    .originalPrompt(prompt)
                    .enhancedPrompt(enhancedPrompt)
                    .fullResponse(finalResponse.getText())
                    .finalResponse(finalResponse.getText())
                    .reasoning(buildProcessReasoning(initialDocuments.size(), rerankedDocuments.size()))
                    .processingSteps(steps)
                    .processingTime(finalResponse.getProcessingTime())
                    .responseChanged(true)
                    .addMetric("initial_documents", initialDocuments.size())
                    .addMetric("reranked_documents", rerankedDocuments.size())
                    .addMetric("citations_count", citations.size())
                    .addMetric("visualization_type", "rag_reranking")
                    .addMetric("document_sources", getDocumentSources(rerankedDocuments))
                    .build();

        } catch (Exception e) {
            return buildErrorResult(prompt, steps, e);
        }
    }

    @Override
    public boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        // Validate initial_retrieval_count
        Object initialCount = configuration.getParameter("initial_retrieval_count");
        if (initialCount != null && !(initialCount instanceof Integer)) {
            return false;
        }
        if (initialCount instanceof Integer && ((Integer) initialCount < 1 || (Integer) initialCount > 100)) {
            return false;
        }

        // Validate final_document_count
        Object finalCount = configuration.getParameter("final_document_count");
        if (finalCount != null && !(finalCount instanceof Integer)) {
            return false;
        }
        if (finalCount instanceof Integer && ((Integer) finalCount < 1 || (Integer) finalCount > 20)) {
            return false;
        }

        // Validate reranking_criteria
        Object criteria = configuration.getParameter("reranking_criteria");
        if (criteria != null && !(criteria instanceof String)) {
            return false;
        }

        return true;
    }

    /**
     * Re-ranks documents using LLM-based relevance scoring.
     */
    private List<Document> rerankDocuments(String query, List<Document> documents,
            int topK, AgenticFlowConfiguration configuration) {
        String rerankingCriteria = (String) configuration.getParameter("reranking_criteria",
                "relevance to the query, information quality, and recency");

        // Build reranking prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please rank the following documents by their relevance to the query.\n");
        prompt.append("Query: ").append(query).append("\n\n");
        prompt.append("Ranking criteria: ").append(rerankingCriteria).append("\n\n");
        prompt.append("Documents:\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            prompt.append(String.format("[%d] Title: %s\n", i + 1, doc.getTitle()));
            prompt.append(String.format("    Content: %s\n", truncateContent(doc.getContent(), 200)));
            prompt.append(String.format("    Source: %s\n\n", doc.getSource()));
        }

        prompt.append(String
                .format("Please provide the IDs of the top %d most relevant documents in order of relevance.\n", topK));
        prompt.append("Format your response as a comma-separated list of document IDs (e.g., 3,1,5,2,4)");

        // Get LLM ranking
        LlmResponse rankingResponse = llmService.generate(prompt.toString(), configuration.getParameters());

        // Parse ranking and return reranked documents
        List<Integer> rankings = parseRankings(rankingResponse.getText(), documents.size());

        return rankings.stream()
                .limit(topK)
                .map(idx -> documents.get(idx - 1))
                .collect(Collectors.toList());
    }

    /**
     * Parses document rankings from LLM response.
     */
    private List<Integer> parseRankings(String response, int documentCount) {
        List<Integer> rankings = new ArrayList<>();

        // Extract comma-separated numbers from response
        String cleanResponse = response.replaceAll("[^0-9,]", "");
        String[] parts = cleanResponse.split(",");

        for (String part : parts) {
            try {
                int docId = Integer.parseInt(part.trim());
                if (docId >= 1 && docId <= documentCount && !rankings.contains(docId)) {
                    rankings.add(docId);
                }
            } catch (NumberFormatException e) {
                // Skip invalid numbers
            }
        }

        // If parsing failed or incomplete, return documents in original order
        if (rankings.size() < Math.min(5, documentCount)) {
            rankings.clear();
            for (int i = 1; i <= documentCount; i++) {
                rankings.add(i);
            }
        }

        return rankings;
    }

    /**
     * Builds the enhanced prompt with selected documents.
     */
    private String buildEnhancedPrompt(String query, List<Document> documents,
            AgenticFlowConfiguration configuration) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Please answer the following question using the provided documents.\n");
        prompt.append("Include citations in your response using [1], [2], etc. format.\n\n");
        prompt.append("Question: ").append(query).append("\n\n");
        prompt.append("Relevant Documents:\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            prompt.append(String.format("[%d] %s\n", i + 1, doc.getTitle()));
            prompt.append(doc.getContent()).append("\n");
            prompt.append("Source: ").append(doc.getSource()).append("\n\n");
        }

        prompt.append("Please provide a comprehensive answer based on these documents:");

        return prompt.toString();
    }

    /**
     * Builds the reranking prompt for visualization.
     */
    private String buildRerankingPrompt(String query, List<Document> documents) {
        return String.format("Reranking %d documents for query: %s", documents.size(), query);
    }

    /**
     * Truncates content to specified length.
     */
    private String truncateContent(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength - 3) + "...";
    }

    /**
     * Extracts citations from the response text.
     */
    private Map<String, Document> extractCitations(String response, List<Document> documents) {
        Map<String, Document> citations = new HashMap<>();

        for (int i = 0; i < documents.size(); i++) {
            String citationPattern = "\\[" + (i + 1) + "\\]";
            if (response.matches(".*" + citationPattern + ".*")) {
                citations.put("[" + (i + 1) + "]", documents.get(i));
            }
        }

        return citations;
    }

    /**
     * Creates metadata for the retrieval step.
     */
    private Map<String, Object> createRetrievalMetadata(List<Document> documents) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_count", documents.size());
        metadata.put("sources", getDocumentSources(documents));
        metadata.put("visualization_type", "document_list");
        return metadata;
    }

    /**
     * Creates metadata for the reranking step.
     */
    private Map<String, Object> createRerankingMetadata(List<Document> initial, List<Document> reranked) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("initial_count", initial.size());
        metadata.put("reranked_count", reranked.size());
        metadata.put("selected_documents", reranked.stream()
                .map(Document::getTitle)
                .collect(Collectors.toList()));
        metadata.put("visualization_type", "reranking_results");
        return metadata;
    }

    /**
     * Creates metadata for the response generation step.
     */
    private Map<String, Object> createResponseMetadata(List<Document> documents, LlmResponse response) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_count", documents.size());
        metadata.put("response_length", response.getText().length());
        metadata.put("visualization_type", "final_response");
        return metadata;
    }

    /**
     * Gets unique document sources.
     */
    private List<String> getDocumentSources(List<Document> documents) {
        return documents.stream()
                .map(Document::getSource)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Builds process reasoning text.
     */
    private String buildProcessReasoning(int initialCount, int rerankedCount) {
        return String.format(
                "RAG with Re-ranking Process:\n" +
                        "1. Retrieved %d initial documents from the knowledge base\n" +
                        "2. Used LLM to re-rank documents by relevance\n" +
                        "3. Selected top %d most relevant documents\n" +
                        "4. Generated response with citations from selected documents",
                initialCount, rerankedCount);
    }

    /**
     * Builds result when no documents are found.
     */
    private AgenticFlowResult buildNoDocumentsResult(String prompt, List<ProcessingStep> steps) {
        String response = "I couldn't find any relevant documents to answer your question.";

        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt)
                .fullResponse(response)
                .finalResponse(response)
                .reasoning("No documents found in the knowledge base for this query.")
                .processingSteps(steps)
                .processingTime(Duration.ofMillis(0))
                .responseChanged(false)
                .addMetric("initial_documents", 0)
                .addMetric("reranked_documents", 0)
                .addMetric("citations_count", 0)
                .addMetric("visualization_type", "rag_reranking")
                .build();
    }

    /**
     * Builds error result.
     */
    private AgenticFlowResult buildErrorResult(String prompt, List<ProcessingStep> steps, Exception e) {
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt)
                .fullResponse("Error in RAG with re-ranking: " + e.getMessage())
                .finalResponse("Error in RAG with re-ranking: " + e.getMessage())
                .reasoning("Processing failed: " + e.getMessage())
                .processingSteps(steps)
                .processingTime(Duration.ofMillis(0))
                .responseChanged(false)
                .addMetric("error", true)
                .addMetric("error_message", e.getMessage())
                .build();
    }
}