package com.zamaz.mcp.common.domain.agentic.processor;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowProcessor;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowResult;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.agentic.ProcessingStep;
import com.zamaz.mcp.common.domain.agentic.PromptContext;
import com.zamaz.mcp.common.domain.llm.LlmResponse;
import com.zamaz.mcp.common.domain.llm.LlmServicePort;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the Tree of Thoughts agentic flow processor.
 * Explores multiple reasoning paths simultaneously like a decision tree.
 */
@Service
public class TreeOfThoughtsFlowService implements AgenticFlowProcessor {
    
    private final LlmServicePort llmService;

    /**
     * Creates a new TreeOfThoughtsFlowService with the specified LLM service.
     *
     * @param llmService The LLM service to use
     */
    public TreeOfThoughtsFlowService(LlmServicePort llmService) {
        this.llmService = llmService;
    }

    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.TREE_OF_THOUGHTS;
    }

    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context) {
        Instant startTime = Instant.now();
        List<ProcessingStep> steps = new ArrayList<>();
        
        try {
            // Get configuration parameters
            int branchingFactor = (Integer) configuration.getParameter("branching_factor", 3);
            int maxDepth = (Integer) configuration.getParameter("max_depth", 3);
            String evaluationMethod = (String) configuration.getParameter("evaluation_method", "llm_scoring");
            
            // Step 1: Generate initial thoughts (root branches)
            List<ThoughtNode> rootThoughts = generateInitialThoughts(prompt, branchingFactor, configuration);
            
            steps.add(new ProcessingStep(
                "initial_thoughts",
                buildInitialThoughtsPrompt(prompt, branchingFactor),
                formatThoughts(rootThoughts),
                createThoughtsMetadata(rootThoughts, 1)
            ));

            // Step 2: Expand the tree to specified depth
            ThoughtTree tree = new ThoughtTree(rootThoughts);
            
            for (int depth = 2; depth <= maxDepth; depth++) {
                List<ThoughtNode> currentLevel = tree.getNodesAtDepth(depth - 1);
                
                for (ThoughtNode parent : currentLevel) {
                    List<ThoughtNode> children = expandThought(
                        prompt, parent, branchingFactor, configuration
                    );
                    parent.addChildren(children);
                    
                    steps.add(new ProcessingStep(
                        String.format("expand_depth_%d_node_%s", depth, parent.getId()),
                        buildExpansionPrompt(prompt, parent),
                        formatThoughts(children),
                        createExpansionMetadata(parent, children, depth)
                    ));
                }
            }

            // Step 3: Evaluate all paths and select the best
            List<ThoughtPath> allPaths = tree.getAllPaths();
            ThoughtPath bestPath = evaluatePaths(prompt, allPaths, evaluationMethod, configuration);
            
            steps.add(new ProcessingStep(
                "path_evaluation",
                buildEvaluationPrompt(prompt, allPaths),
                formatPathEvaluation(bestPath, allPaths),
                createEvaluationMetadata(bestPath, allPaths)
            ));

            // Step 4: Generate final response based on best path
            String finalResponse = synthesizeFinalResponse(prompt, bestPath, configuration);
            
            steps.add(new ProcessingStep(
                "final_synthesis",
                buildSynthesisPrompt(prompt, bestPath),
                finalResponse,
                createSynthesisMetadata(bestPath)
            ));

            // Calculate metrics
            int totalNodes = tree.getTotalNodes();
            int totalPaths = allPaths.size();
            
            return AgenticFlowResult.builder()
                    .originalPrompt(prompt)
                    .enhancedPrompt(buildInitialThoughtsPrompt(prompt, branchingFactor))
                    .fullResponse(formatFullTreeOutput(tree, bestPath))
                    .finalResponse(finalResponse)
                    .reasoning(formatReasoningPath(bestPath))
                    .addAllProcessingSteps(steps)
                    .processingTime(System.currentTimeMillis() - startTime.toEpochMilli())
                    .responseChanged(true)
                    .addMetric("branching_factor", branchingFactor)
                    .addMetric("max_depth", maxDepth)
                    .addMetric("total_nodes", totalNodes)
                    .addMetric("total_paths", totalPaths)
                    .addMetric("best_path_score", bestPath.getScore())
                    .addMetric("best_path_depth", bestPath.getDepth())
                    .addMetric("visualization_type", "tree_of_thoughts")
                    .build();

        } catch (Exception e) {
            return buildErrorResult(prompt, steps, e);
        }
    }

    @Override
    public boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        // Validate branching_factor
        Object branchingFactor = configuration.getParameter("branching_factor");
        if (branchingFactor != null) {
            if (!(branchingFactor instanceof Integer)) {
                return false;
            }
            int factor = (Integer) branchingFactor;
            if (factor < 2 || factor > 5) {
                return false;
            }
        }

        // Validate max_depth
        Object maxDepth = configuration.getParameter("max_depth");
        if (maxDepth != null) {
            if (!(maxDepth instanceof Integer)) {
                return false;
            }
            int depth = (Integer) maxDepth;
            if (depth < 1 || depth > 5) {
                return false;
            }
        }

        // Validate evaluation_method
        Object evalMethod = configuration.getParameter("evaluation_method");
        if (evalMethod != null) {
            if (!(evalMethod instanceof String)) {
                return false;
            }
            String method = (String) evalMethod;
            if (!List.of("llm_scoring", "heuristic", "combined").contains(method)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Generates initial thoughts for the root of the tree.
     */
    private List<ThoughtNode> generateInitialThoughts(String prompt, int branchingFactor,
                                                    AgenticFlowConfiguration configuration) {
        String thoughtPrompt = buildInitialThoughtsPrompt(prompt, branchingFactor);
        
        LlmResponse response = llmService.generate(thoughtPrompt, configuration.getParameters());
        
        return parseThoughts(response.getText(), branchingFactor, null, 1);
    }

    /**
     * Expands a thought node by generating child thoughts.
     */
    private List<ThoughtNode> expandThought(String originalPrompt, ThoughtNode parent,
                                          int branchingFactor, AgenticFlowConfiguration configuration) {
        String expansionPrompt = buildExpansionPrompt(originalPrompt, parent);
        
        LlmResponse response = llmService.generate(expansionPrompt, configuration.getParameters());
        
        return parseThoughts(response.getText(), branchingFactor, parent, parent.getDepth() + 1);
    }

    /**
     * Evaluates all paths and selects the best one.
     */
    private ThoughtPath evaluatePaths(String prompt, List<ThoughtPath> paths,
                                    String evaluationMethod, AgenticFlowConfiguration configuration) {
        if ("heuristic".equals(evaluationMethod)) {
            return evaluateByHeuristic(paths);
        } else if ("combined".equals(evaluationMethod)) {
            return evaluateByCombined(prompt, paths, configuration);
        } else {
            return evaluateByLLMScoring(prompt, paths, configuration);
        }
    }

    /**
     * Evaluates paths using LLM scoring.
     */
    private ThoughtPath evaluateByLLMScoring(String prompt, List<ThoughtPath> paths,
                                           AgenticFlowConfiguration configuration) {
        StringBuilder evalPrompt = new StringBuilder();
        evalPrompt.append("Evaluate the following reasoning paths for the question:\n");
        evalPrompt.append(prompt).append("\n\n");
        
        for (int i = 0; i < paths.size(); i++) {
            evalPrompt.append(String.format("Path %d:\n", i + 1));
            evalPrompt.append(formatPath(paths.get(i)));
            evalPrompt.append("\n");
        }
        
        evalPrompt.append("Which path provides the best reasoning? Provide the path number and a score from 0-100.");
        
        LlmResponse response = llmService.generate(evalPrompt.toString(), configuration.getParameters());
        
        // Parse the best path selection
        int bestPathIndex = extractPathIndex(response.getText(), paths.size());
        float score = extractScore(response.getText());
        
        ThoughtPath bestPath = paths.get(bestPathIndex);
        bestPath.setScore(score);
        
        return bestPath;
    }

    /**
     * Evaluates paths using heuristic methods.
     */
    private ThoughtPath evaluateByHeuristic(List<ThoughtPath> paths) {
        // Simple heuristic: prefer paths that are complete and have medium depth
        ThoughtPath bestPath = null;
        float bestScore = -1;
        
        for (ThoughtPath path : paths) {
            float score = calculateHeuristicScore(path);
            if (score > bestScore) {
                bestScore = score;
                bestPath = path;
            }
        }
        
        if (bestPath != null) {
            bestPath.setScore(bestScore);
        }
        
        return bestPath != null ? bestPath : paths.get(0);
    }

    /**
     * Evaluates paths using combined LLM and heuristic scoring.
     */
    private ThoughtPath evaluateByCombined(String prompt, List<ThoughtPath> paths,
                                         AgenticFlowConfiguration configuration) {
        ThoughtPath llmBest = evaluateByLLMScoring(prompt, paths, configuration);
        ThoughtPath heuristicBest = evaluateByHeuristic(paths);
        
        // Average the scores
        if (llmBest.equals(heuristicBest)) {
            return llmBest;
        }
        
        float llmScore = llmBest.getScore();
        float heuristicScore = calculateHeuristicScore(llmBest);
        llmBest.setScore((llmScore + heuristicScore) / 2);
        
        return llmBest;
    }

    /**
     * Calculates heuristic score for a path.
     */
    private float calculateHeuristicScore(ThoughtPath path) {
        // Factors: depth (prefer 2-3), completeness, thought length
        float depthScore = 1.0f - Math.abs(path.getDepth() - 2.5f) / 2.5f;
        float lengthScore = Math.min(1.0f, path.getTotalLength() / 1000.0f);
        float coherenceScore = 0.8f; // Placeholder for coherence check
        
        return (depthScore + lengthScore + coherenceScore) / 3.0f * 100;
    }

    /**
     * Synthesizes the final response based on the best path.
     */
    private String synthesizeFinalResponse(String prompt, ThoughtPath bestPath,
                                         AgenticFlowConfiguration configuration) {
        String synthesisPrompt = buildSynthesisPrompt(prompt, bestPath);
        
        LlmResponse response = llmService.generate(synthesisPrompt, configuration.getParameters());
        
        return response.getText();
    }

    /**
     * Builds prompt for generating initial thoughts.
     */
    private String buildInitialThoughtsPrompt(String prompt, int branchingFactor) {
        StringBuilder thoughtPrompt = new StringBuilder();
        thoughtPrompt.append("For the following question, generate ");
        thoughtPrompt.append(branchingFactor);
        thoughtPrompt.append(" different initial approaches or thoughts:\n\n");
        thoughtPrompt.append(prompt).append("\n\n");
        thoughtPrompt.append("For each approach, provide:\n");
        thoughtPrompt.append("1. A brief description of the approach\n");
        thoughtPrompt.append("2. Initial reasoning or first steps\n\n");
        thoughtPrompt.append("Format each thought as: THOUGHT [number]: [content]");
        
        return thoughtPrompt.toString();
    }

    /**
     * Builds prompt for expanding a thought.
     */
    private String buildExpansionPrompt(String originalPrompt, ThoughtNode parent) {
        StringBuilder expansionPrompt = new StringBuilder();
        expansionPrompt.append("Original question: ").append(originalPrompt).append("\n\n");
        expansionPrompt.append("Current reasoning path:\n");
        expansionPrompt.append(parent.getFullPath()).append("\n\n");
        expansionPrompt.append("Continue this line of reasoning with ");
        expansionPrompt.append(parent.getChildren().size() == 0 ? "3" : parent.getChildren().size());
        expansionPrompt.append(" different next steps or developments.\n");
        expansionPrompt.append("Format each continuation as: THOUGHT [number]: [content]");
        
        return expansionPrompt.toString();
    }

    /**
     * Builds prompt for evaluating paths.
     */
    private String buildEvaluationPrompt(String prompt, List<ThoughtPath> paths) {
        return String.format("Evaluating %d reasoning paths for: %s", paths.size(), prompt);
    }

    /**
     * Builds prompt for final synthesis.
     */
    private String buildSynthesisPrompt(String prompt, ThoughtPath bestPath) {
        StringBuilder synthesisPrompt = new StringBuilder();
        synthesisPrompt.append("Based on the following reasoning path, ");
        synthesisPrompt.append("provide a comprehensive answer to the question:\n\n");
        synthesisPrompt.append("Question: ").append(prompt).append("\n\n");
        synthesisPrompt.append("Reasoning path:\n");
        synthesisPrompt.append(formatPath(bestPath)).append("\n\n");
        synthesisPrompt.append("Synthesize this reasoning into a clear, complete answer:");
        
        return synthesisPrompt.toString();
    }

    /**
     * Parses thoughts from LLM response.
     */
    private List<ThoughtNode> parseThoughts(String response, int expectedCount,
                                          ThoughtNode parent, int depth) {
        List<ThoughtNode> thoughts = new ArrayList<>();
        
        String[] lines = response.split("\n");
        StringBuilder currentThought = new StringBuilder();
        int thoughtNumber = 0;
        
        for (String line : lines) {
            if (line.trim().matches("THOUGHT \\d+:.*")) {
                if (currentThought.length() > 0 && thoughtNumber > 0) {
                    thoughts.add(new ThoughtNode(
                        String.valueOf(thoughtNumber),
                        currentThought.toString().trim(),
                        parent,
                        depth
                    ));
                }
                thoughtNumber++;
                currentThought = new StringBuilder();
                currentThought.append(line.substring(line.indexOf(":") + 1).trim());
            } else if (thoughtNumber > 0) {
                currentThought.append(" ").append(line.trim());
            }
        }
        
        // Add the last thought
        if (currentThought.length() > 0 && thoughtNumber > 0) {
            thoughts.add(new ThoughtNode(
                String.valueOf(thoughtNumber),
                currentThought.toString().trim(),
                parent,
                depth
            ));
        }
        
        // If parsing failed, create default thoughts
        while (thoughts.size() < expectedCount) {
            thoughts.add(new ThoughtNode(
                String.valueOf(thoughts.size() + 1),
                "Thought " + (thoughts.size() + 1),
                parent,
                depth
            ));
        }
        
        return thoughts.stream().limit(expectedCount).collect(Collectors.toList());
    }

    /**
     * Extracts path index from evaluation response.
     */
    private int extractPathIndex(String response, int maxPaths) {
        for (int i = 1; i <= maxPaths; i++) {
            if (response.contains("Path " + i) || response.contains("path " + i)) {
                return i - 1;
            }
        }
        return 0; // Default to first path
    }

    /**
     * Extracts score from evaluation response.
     */
    private float extractScore(String response) {
        // Look for numbers between 0 and 100
        String[] words = response.split("\\s+");
        for (String word : words) {
            try {
                float score = Float.parseFloat(word.replaceAll("[^0-9.]", ""));
                if (score >= 0 && score <= 100) {
                    return score;
                }
            } catch (NumberFormatException e) {
                // Continue searching
            }
        }
        return 75.0f; // Default score
    }

    /**
     * Formats thoughts for display.
     */
    private String formatThoughts(List<ThoughtNode> thoughts) {
        StringBuilder formatted = new StringBuilder();
        for (ThoughtNode thought : thoughts) {
            formatted.append("• ").append(thought.getContent()).append("\n");
        }
        return formatted.toString();
    }

    /**
     * Formats a path for display.
     */
    private String formatPath(ThoughtPath path) {
        StringBuilder formatted = new StringBuilder();
        List<ThoughtNode> nodes = path.getNodes();
        
        for (int i = 0; i < nodes.size(); i++) {
            formatted.append(String.format("Step %d: %s\n", i + 1, nodes.get(i).getContent()));
        }
        
        return formatted.toString();
    }

    /**
     * Formats path evaluation for display.
     */
    private String formatPathEvaluation(ThoughtPath bestPath, List<ThoughtPath> allPaths) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("Evaluated ").append(allPaths.size()).append(" paths.\n");
        formatted.append("Best path score: ").append(bestPath.getScore()).append("\n");
        formatted.append("Best path depth: ").append(bestPath.getDepth()).append("\n");
        return formatted.toString();
    }

    /**
     * Formats reasoning path for result.
     */
    private String formatReasoningPath(ThoughtPath path) {
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("Tree of Thoughts reasoning path:\n\n");
        
        List<ThoughtNode> nodes = path.getNodes();
        for (int i = 0; i < nodes.size(); i++) {
            reasoning.append(String.format("%d. %s\n", i + 1, nodes.get(i).getContent()));
        }
        
        reasoning.append("\nThis path was selected with a score of ");
        reasoning.append(String.format("%.1f/100", path.getScore()));
        
        return reasoning.toString();
    }

    /**
     * Formats full tree output for visualization.
     */
    private String formatFullTreeOutput(ThoughtTree tree, ThoughtPath bestPath) {
        StringBuilder output = new StringBuilder();
        output.append("Tree of Thoughts Exploration:\n\n");
        output.append("Total nodes explored: ").append(tree.getTotalNodes()).append("\n");
        output.append("Total paths: ").append(tree.getAllPaths().size()).append("\n\n");
        output.append("Best path:\n");
        output.append(formatPath(bestPath));
        return output.toString();
    }

    /**
     * Creates metadata for thoughts.
     */
    private Map<String, Object> createThoughtsMetadata(List<ThoughtNode> thoughts, int depth) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("thought_count", thoughts.size());
        metadata.put("depth", depth);
        metadata.put("visualization_type", "thought_generation");
        return metadata;
    }

    /**
     * Creates metadata for expansion.
     */
    private Map<String, Object> createExpansionMetadata(ThoughtNode parent, 
                                                      List<ThoughtNode> children, int depth) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("parent_id", parent.getId());
        metadata.put("children_count", children.size());
        metadata.put("depth", depth);
        metadata.put("visualization_type", "thought_expansion");
        return metadata;
    }

    /**
     * Creates metadata for evaluation.
     */
    private Map<String, Object> createEvaluationMetadata(ThoughtPath bestPath, List<ThoughtPath> allPaths) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("total_paths", allPaths.size());
        metadata.put("best_path_score", bestPath.getScore());
        metadata.put("best_path_depth", bestPath.getDepth());
        metadata.put("visualization_type", "path_evaluation");
        return metadata;
    }

    /**
     * Creates metadata for synthesis.
     */
    private Map<String, Object> createSynthesisMetadata(ThoughtPath bestPath) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("path_length", bestPath.getDepth());
        metadata.put("path_score", bestPath.getScore());
        metadata.put("visualization_type", "final_synthesis");
        return metadata;
    }

    /**
     * Builds error result.
     */
    private AgenticFlowResult buildErrorResult(String prompt, List<ProcessingStep> steps, Exception e) {
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt)
                .fullResponse("Error in tree of thoughts: " + e.getMessage())
                .finalResponse("Error in tree of thoughts: " + e.getMessage())
                .reasoning("Processing failed: " + e.getMessage())
                .addAllProcessingSteps(steps)
                .processingTime(0L)
                .responseChanged(false)
                .addMetric("error", true)
                .addMetric("error_message", e.getMessage())
                .build();
    }

    /**
     * Inner class representing a thought node in the tree.
     */
    private static class ThoughtNode {
        private final String id;
        private final String content;
        private final ThoughtNode parent;
        private final int depth;
        private final List<ThoughtNode> children;

        public ThoughtNode(String id, String content, ThoughtNode parent, int depth) {
            this.id = id;
            this.content = content;
            this.parent = parent;
            this.depth = depth;
            this.children = new ArrayList<>();
        }

        public void addChildren(List<ThoughtNode> children) {
            this.children.addAll(children);
        }

        public String getId() {
            return (parent != null ? parent.getId() + "." : "") + id;
        }

        public String getContent() {
            return content;
        }

        public int getDepth() {
            return depth;
        }

        public List<ThoughtNode> getChildren() {
            return children;
        }

        public String getFullPath() {
            if (parent == null) {
                return content;
            }
            return parent.getFullPath() + " → " + content;
        }
    }

    /**
     * Inner class representing a complete path through the tree.
     */
    private static class ThoughtPath {
        private final List<ThoughtNode> nodes;
        private float score;

        public ThoughtPath(List<ThoughtNode> nodes) {
            this.nodes = new ArrayList<>(nodes);
            this.score = 0.0f;
        }

        public List<ThoughtNode> getNodes() {
            return nodes;
        }

        public int getDepth() {
            return nodes.size();
        }

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }

        public int getTotalLength() {
            return nodes.stream()
                    .mapToInt(node -> node.getContent().length())
                    .sum();
        }
    }

    /**
     * Inner class representing the thought tree structure.
     */
    private static class ThoughtTree {
        private final List<ThoughtNode> roots;

        public ThoughtTree(List<ThoughtNode> roots) {
            this.roots = roots;
        }

        public List<ThoughtNode> getNodesAtDepth(int depth) {
            List<ThoughtNode> nodes = new ArrayList<>();
            collectNodesAtDepth(roots, depth, nodes);
            return nodes;
        }

        private void collectNodesAtDepth(List<ThoughtNode> currentNodes, int targetDepth, 
                                       List<ThoughtNode> result) {
            for (ThoughtNode node : currentNodes) {
                if (node.getDepth() == targetDepth) {
                    result.add(node);
                } else if (node.getDepth() < targetDepth) {
                    collectNodesAtDepth(node.getChildren(), targetDepth, result);
                }
            }
        }

        public List<ThoughtPath> getAllPaths() {
            List<ThoughtPath> paths = new ArrayList<>();
            for (ThoughtNode root : roots) {
                collectPaths(root, new ArrayList<>(), paths);
            }
            return paths;
        }

        private void collectPaths(ThoughtNode node, List<ThoughtNode> currentPath, 
                                List<ThoughtPath> allPaths) {
            currentPath.add(node);
            
            if (node.getChildren().isEmpty()) {
                allPaths.add(new ThoughtPath(currentPath));
            } else {
                for (ThoughtNode child : node.getChildren()) {
                    collectPaths(child, new ArrayList<>(currentPath), allPaths);
                }
            }
        }

        public int getTotalNodes() {
            return countNodes(roots);
        }

        private int countNodes(List<ThoughtNode> nodes) {
            int count = nodes.size();
            for (ThoughtNode node : nodes) {
                count += countNodes(node.getChildren());
            }
            return count;
        }
    }
}