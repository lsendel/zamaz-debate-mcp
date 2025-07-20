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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the Multi-Agent Red-Team agentic flow processor.
 * Simulates internal debates between different perspectives within a single AI
 * agent
 * using Architect, Skeptic, and Judge personas.
 */
@Service
public class MultiAgentRedTeamFlowService implements AgenticFlowProcessor {
    private final LlmServicePort llmService;

    private static final String STEP_TYPE = "multi_agent_red_team";
    private static final String ARCHITECT_PERSONA = "architect";
    private static final String SKEPTIC_PERSONA = "skeptic";
    private static final String JUDGE_PERSONA = "judge";
    private static final String ARCHITECT_PROMPT_PARAM = "architect_prompt";
    private static final String SKEPTIC_PROMPT_PARAM = "skeptic_prompt";
    private static final String JUDGE_PROMPT_PARAM = "judge_prompt";
    private static final String VISUALIZATION_TYPE = "visualization_type";

    /**
     * Creates a new MultiAgentRedTeamFlowService with the specified LLM service.
     *
     * @param llmService The LLM service to use
     */
    public MultiAgentRedTeamFlowService(LlmServicePort llmService) {
        this.llmService = llmService;
    }

    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.MULTI_AGENT_RED_TEAM;
    }

    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context) {
        // Record start time for processing duration
        Instant startTime = Instant.now();

        // Step 1: Architect proposes initial solution
        String architectPrompt = buildArchitectPrompt(prompt, configuration);
        LlmResponse architectResponse = llmService.generate(architectPrompt, configuration.getParameters());
        String architectSolution = architectResponse.getText();

        // Create processing step for Architect
        ProcessingStep architectStep = new ProcessingStep(
                STEP_TYPE + "_" + ARCHITECT_PERSONA,
                architectPrompt,
                architectSolution,
                createPersonaMetadata(ARCHITECT_PERSONA, 1));

        // Step 2: Skeptic critiques the solution
        String skepticPrompt = buildSkepticPrompt(prompt, architectSolution, configuration);
        LlmResponse skepticResponse = llmService.generate(skepticPrompt, configuration.getParameters());
        String skepticCritique = skepticResponse.getText();

        // Create processing step for Skeptic
        ProcessingStep skepticStep = new ProcessingStep(
                STEP_TYPE + "_" + SKEPTIC_PERSONA,
                skepticPrompt,
                skepticCritique,
                createPersonaMetadata(SKEPTIC_PERSONA, 2));

        // Step 3: Judge evaluates both perspectives and makes a final decision
        String judgePrompt = buildJudgePrompt(prompt, architectSolution, skepticCritique, configuration);
        LlmResponse judgeResponse = llmService.generate(judgePrompt, configuration.getParameters());
        String judgeFinalDecision = judgeResponse.getText();

        // Create processing step for Judge
        ProcessingStep judgeStep = new ProcessingStep(
                STEP_TYPE + "_" + JUDGE_PERSONA,
                judgePrompt,
                judgeFinalDecision,
                createPersonaMetadata(JUDGE_PERSONA, 3));

        // Calculate processing time
        Duration processingTime = Duration.between(startTime, Instant.now());

        // Determine if the Judge agreed more with the Architect or Skeptic
        boolean judgeAgreesWithArchitect = determineJudgeAgreement(judgeFinalDecision, architectSolution,
                skepticCritique);

        // Build and return the result
        List<ProcessingStep> steps = new ArrayList<>();
        steps.add(architectStep);
        steps.add(skepticStep);
        steps.add(judgeStep);

        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(architectPrompt) // Use the architect prompt as the enhanced prompt
                .fullResponse(judgeFinalDecision)
                .finalResponse(extractFinalDecision(judgeFinalDecision))
                .processingSteps(steps)
                .processingTime(processingTime)
                .responseChanged(true) // Always true for multi-agent red team
                .addMetric(VISUALIZATION_TYPE, STEP_TYPE)
                .addMetric("persona_count", 3)
                .addMetric("judge_agrees_with_architect", judgeAgreesWithArchitect)
                .addMetric("judge_agrees_with_skeptic", !judgeAgreesWithArchitect)
                .build();
    }

    @Override
    public boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        // Validate that the persona prompts are strings if present
        if (configuration.getParameter(ARCHITECT_PROMPT_PARAM) != null
                && !(configuration.getParameter(ARCHITECT_PROMPT_PARAM) instanceof String)) {
            return false;
        }

        if (configuration.getParameter(SKEPTIC_PROMPT_PARAM) != null
                && !(configuration.getParameter(SKEPTIC_PROMPT_PARAM) instanceof String)) {
            return false;
        }

        if (configuration.getParameter(JUDGE_PROMPT_PARAM) != null
                && !(configuration.getParameter(JUDGE_PROMPT_PARAM) instanceof String)) {
            return false;
        }

        return true;
    }

    /**
     * Builds the prompt for the Architect persona.
     *
     * @param prompt        The original prompt
     * @param configuration The flow configuration
     * @return The Architect prompt
     */
    private String buildArchitectPrompt(String prompt, AgenticFlowConfiguration configuration) {
        String architectPromptTemplate = (String) configuration.getParameter(ARCHITECT_PROMPT_PARAM,
                "You are the Architect, responsible for proposing a well-reasoned solution to the following problem. " +
                        "Think carefully and provide a comprehensive answer with supporting evidence and reasoning.\n\n"
                        +
                        "Problem: {prompt}\n\n" +
                        "Your solution:");

        return architectPromptTemplate.replace("{prompt}", prompt);
    }

    /**
     * Builds the prompt for the Skeptic persona.
     *
     * @param prompt            The original prompt
     * @param architectSolution The Architect's solution
     * @param configuration     The flow configuration
     * @return The Skeptic prompt
     */
    private String buildSkepticPrompt(String prompt, String architectSolution, AgenticFlowConfiguration configuration) {
        String skepticPromptTemplate = (String) configuration.getParameter(SKEPTIC_PROMPT_PARAM,
                "You are the Skeptic, responsible for critically evaluating the Architect's proposed solution. " +
                        "Identify weaknesses, unstated assumptions, logical fallacies, or alternative perspectives that were not considered. "
                        +
                        "Be thorough but fair in your critique.\n\n" +
                        "Problem: {prompt}\n\n" +
                        "Architect's Solution: {architect_solution}\n\n" +
                        "Your critique:");

        return skepticPromptTemplate
                .replace("{prompt}", prompt)
                .replace("{architect_solution}", architectSolution);
    }

    /**
     * Builds the prompt for the Judge persona.
     *
     * @param prompt            The original prompt
     * @param architectSolution The Architect's solution
     * @param skepticCritique   The Skeptic's critique
     * @param configuration     The flow configuration
     * @return The Judge prompt
     */
    private String buildJudgePrompt(String prompt, String architectSolution, String skepticCritique,
            AgenticFlowConfiguration configuration) {
        String judgePromptTemplate = (String) configuration.getParameter(JUDGE_PROMPT_PARAM,
                "You are the Judge, responsible for evaluating both the Architect's solution and the Skeptic's critique. "
                        +
                        "Consider the strengths and weaknesses of both perspectives, and provide a final decision or synthesis that "
                        +
                        "represents the most balanced and accurate response to the original problem. " +
                        "Be explicit about which points from each perspective you find most compelling.\n\n" +
                        "Problem: {prompt}\n\n" +
                        "Architect's Solution: {architect_solution}\n\n" +
                        "Skeptic's Critique: {skeptic_critique}\n\n" +
                        "Your final decision:");

        return judgePromptTemplate
                .replace("{prompt}", prompt)
                .replace("{architect_solution}", architectSolution)
                .replace("{skeptic_critique}", skepticCritique);
    }

    /**
     * Creates metadata for a persona processing step.
     *
     * @param persona   The persona name
     * @param stepOrder The step order
     * @return A map of metadata
     */
    private Map<String, Object> createPersonaMetadata(String persona, int stepOrder) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(VISUALIZATION_TYPE, STEP_TYPE);
        metadata.put("persona", persona);
        metadata.put("step_name", capitalizeFirstLetter(persona) + " Perspective");
        metadata.put("step_description", getPersonaDescription(persona));
        metadata.put("step_order", stepOrder);
        return metadata;
    }

    /**
     * Returns a description for the specified persona.
     *
     * @param persona The persona name
     * @return The persona description
     */
    private String getPersonaDescription(String persona) {
        switch (persona) {
            case ARCHITECT_PERSONA:
                return "Proposes the initial solution or argument";
            case SKEPTIC_PERSONA:
                return "Critically evaluates the proposed solution, identifying weaknesses";
            case JUDGE_PERSONA:
                return "Evaluates both perspectives and makes a final decision or synthesis";
            default:
                return "Unknown persona";
        }
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param input The input string
     * @return The string with the first letter capitalized
     */
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    /**
     * Determines whether the Judge agrees more with the Architect or the Skeptic.
     * This is a simplified implementation that could be improved with more
     * sophisticated
     * text analysis or by explicitly asking the Judge to indicate agreement.
     *
     * @param judgeFinalDecision The Judge's final decision
     * @param architectSolution  The Architect's solution
     * @param skepticCritique    The Skeptic's critique
     * @return True if the Judge agrees more with the Architect, false otherwise
     */
    private boolean determineJudgeAgreement(String judgeFinalDecision, String architectSolution,
            String skepticCritique) {
        // Look for explicit statements of agreement
        Pattern architectPattern = Pattern.compile(
                "(?i)(agree with the architect|architect is correct|architect's solution is better|architect's perspective is more)");
        Pattern skepticPattern = Pattern.compile(
                "(?i)(agree with the skeptic|skeptic is correct|skeptic's critique is better|skeptic's perspective is more)");

        Matcher architectMatcher = architectPattern.matcher(judgeFinalDecision);
        Matcher skepticMatcher = skepticPattern.matcher(judgeFinalDecision);

        if (architectMatcher.find() && !skepticMatcher.find()) {
            return true;
        } else if (!architectMatcher.find() && skepticMatcher.find()) {
            return false;
        }

        // If no explicit agreement, compare text similarity (very simplified approach)
        // A more sophisticated approach would use semantic similarity
        int architectSimilarity = calculateTextSimilarity(judgeFinalDecision, architectSolution);
        int skepticSimilarity = calculateTextSimilarity(judgeFinalDecision, skepticCritique);

        return architectSimilarity > skepticSimilarity;
    }

    /**
     * Calculates a simple text similarity score based on word overlap.
     * This is a very simplified approach and could be improved with more
     * sophisticated text similarity algorithms.
     *
     * @param text1 The first text
     * @param text2 The second text
     * @return A similarity score
     */
    private int calculateTextSimilarity(String text1, String text2) {
        // Convert to lowercase and split into words
        String[] words1 = text1.toLowerCase().split("\\W+");
        String[] words2 = text2.toLowerCase().split("\\W+");

        // Count matching words
        int matches = 0;
        for (String word1 : words1) {
            if (word1.length() > 3) { // Only consider words longer than 3 characters
                for (String word2 : words2) {
                    if (word1.equals(word2)) {
                        matches++;
                        break;
                    }
                }
            }
        }

        return matches;
    }

    /**
     * Extracts the final decision from the Judge's response.
     * This method attempts to find a conclusion section or returns the full
     * response.
     *
     * @param judgeFinalDecision The Judge's final decision
     * @return The extracted final decision
     */
    private String extractFinalDecision(String judgeFinalDecision) {
        // Try to find a conclusion section
        Pattern pattern = Pattern.compile(
                "(?i)(conclusion:|in conclusion:|therefore,|thus,|to summarize:|in summary:|final decision:|final judgment:|final synthesis:)");
        Matcher matcher = pattern.matcher(judgeFinalDecision);

        if (matcher.find()) {
            return judgeFinalDecision.substring(matcher.start()).trim();
        }

        // If no conclusion marker is found, return the full response
        return judgeFinalDecision;
    }
}