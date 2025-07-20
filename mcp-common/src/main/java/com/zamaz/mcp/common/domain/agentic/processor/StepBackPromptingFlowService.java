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

/**
 * Implementation of the Step-Back Prompting agentic flow processor.
 * Prompts the model to generalize from specific questions to underlying principles.
 */
@Service
public class StepBackPromptingFlowService implements AgenticFlowProcessor {
    
    private final LlmServicePort llmService;

    /**
     * Creates a new StepBackPromptingFlowService with the specified LLM service.
     *
     * @param llmService The LLM service to use
     */
    public StepBackPromptingFlowService(LlmServicePort llmService) {
        this.llmService = llmService;
    }

    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.STEP_BACK_PROMPTING;
    }

    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context) {
        Instant startTime = Instant.now();
        List<ProcessingStep> steps = new ArrayList<>();
        
        try {
            // Get configuration parameters
            int abstractionLevels = (Integer) configuration.getParameter("abstraction_levels", 2);
            boolean includeExamples = (Boolean) configuration.getParameter("include_examples", true);
            
            // Step 1: Identify the specific question type and context
            QuestionAnalysis analysis = analyzeQuestion(prompt, configuration);
            
            steps.add(new ProcessingStep(
                "question_analysis",
                buildAnalysisPrompt(prompt),
                formatAnalysis(analysis),
                createAnalysisMetadata(analysis)
            ));

            // Step 2: Generate step-back questions at different abstraction levels
            List<StepBackQuestion> stepBackQuestions = generateStepBackQuestions(
                prompt, analysis, abstractionLevels, configuration
            );
            
            steps.add(new ProcessingStep(
                "step_back_generation",
                buildStepBackPrompt(prompt, analysis),
                formatStepBackQuestions(stepBackQuestions),
                createStepBackMetadata(stepBackQuestions)
            ));

            // Step 3: Answer the step-back questions to establish principles
            List<PrincipleAnswer> principles = new ArrayList<>();
            
            for (StepBackQuestion sbQuestion : stepBackQuestions) {
                PrincipleAnswer principle = answerStepBackQuestion(sbQuestion, configuration);
                principles.add(principle);
                
                steps.add(new ProcessingStep(
                    String.format("principle_answer_level_%d", sbQuestion.getAbstractionLevel()),
                    sbQuestion.getQuestion(),
                    principle.getAnswer(),
                    createPrincipleMetadata(principle)
                ));
            }

            // Step 4: Apply principles to answer the original question
            String enhancedPrompt = buildEnhancedPrompt(prompt, principles, includeExamples);
            LlmResponse finalResponse = llmService.generate(enhancedPrompt, configuration.getParameters());
            
            steps.add(new ProcessingStep(
                "principle_application",
                enhancedPrompt,
                finalResponse.getText(),
                createApplicationMetadata(principles.size())
            ));

            // Build reasoning summary
            String reasoning = buildReasoningSummary(analysis, stepBackQuestions, principles);
            
            return AgenticFlowResult.builder()
                    .originalPrompt(prompt)
                    .enhancedPrompt(enhancedPrompt)
                    .fullResponse(formatFullOutput(analysis, stepBackQuestions, principles, finalResponse.getText()))
                    .finalResponse(finalResponse.getText())
                    .reasoning(reasoning)
                    .addAllProcessingSteps(steps)
                    .processingTime(finalResponse.getProcessingTime())
                    .responseChanged(true)
                    .addMetric("abstraction_levels", abstractionLevels)
                    .addMetric("step_back_questions", stepBackQuestions.size())
                    .addMetric("principles_identified", principles.size())
                    .addMetric("question_type", analysis.getQuestionType())
                    .addMetric("domain", analysis.getDomain())
                    .addMetric("visualization_type", "step_back_prompting")
                    .build();

        } catch (Exception e) {
            return buildErrorResult(prompt, steps, e);
        }
    }

    @Override
    public boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        // Validate abstraction_levels
        Object levels = configuration.getParameter("abstraction_levels");
        if (levels != null) {
            if (!(levels instanceof Integer)) {
                return false;
            }
            int value = (Integer) levels;
            if (value < 1 || value > 4) {
                return false;
            }
        }

        // Validate include_examples
        Object includeExamples = configuration.getParameter("include_examples");
        if (includeExamples != null && !(includeExamples instanceof Boolean)) {
            return false;
        }

        return true;
    }

    /**
     * Analyzes the question to understand its type and context.
     */
    private QuestionAnalysis analyzeQuestion(String prompt, AgenticFlowConfiguration configuration) {
        StringBuilder analysisPrompt = new StringBuilder();
        analysisPrompt.append("Analyze the following question:\n");
        analysisPrompt.append(prompt).append("\n\n");
        analysisPrompt.append("Identify:\n");
        analysisPrompt.append("1. Question type (e.g., how-to, why, what-if, comparison, definition)\n");
        analysisPrompt.append("2. Domain (e.g., science, technology, history, philosophy)\n");
        analysisPrompt.append("3. Key concepts involved\n");
        analysisPrompt.append("4. Level of specificity (very specific, moderately specific, general)\n");
        analysisPrompt.append("\nFormat: TYPE: [type], DOMAIN: [domain], CONCEPTS: [concepts], SPECIFICITY: [level]");
        
        LlmResponse response = llmService.generate(analysisPrompt.toString(), configuration.getParameters());
        
        return parseQuestionAnalysis(response.getText());
    }

    /**
     * Generates step-back questions at different abstraction levels.
     */
    private List<StepBackQuestion> generateStepBackQuestions(String originalPrompt, 
                                                           QuestionAnalysis analysis,
                                                           int abstractionLevels,
                                                           AgenticFlowConfiguration configuration) {
        List<StepBackQuestion> questions = new ArrayList<>();
        
        for (int level = 1; level <= abstractionLevels; level++) {
            String sbPrompt = buildStepBackGenerationPrompt(originalPrompt, analysis, level);
            LlmResponse response = llmService.generate(sbPrompt, configuration.getParameters());
            
            StepBackQuestion sbQuestion = new StepBackQuestion(
                response.getText().trim(),
                level,
                getAbstractionDescription(level)
            );
            questions.add(sbQuestion);
        }
        
        return questions;
    }

    /**
     * Answers a step-back question to establish principles.
     */
    private PrincipleAnswer answerStepBackQuestion(StepBackQuestion question,
                                                 AgenticFlowConfiguration configuration) {
        StringBuilder principlePrompt = new StringBuilder();
        principlePrompt.append("Answer the following general question to establish fundamental principles:\n\n");
        principlePrompt.append(question.getQuestion()).append("\n\n");
        principlePrompt.append("Provide a comprehensive answer that explains the underlying principles, ");
        principlePrompt.append("patterns, or general rules that apply.");
        
        LlmResponse response = llmService.generate(principlePrompt.toString(), configuration.getParameters());
        
        return new PrincipleAnswer(
            question,
            response.getText(),
            extractKeyPrinciples(response.getText())
        );
    }

    /**
     * Builds analysis prompt.
     */
    private String buildAnalysisPrompt(String prompt) {
        return "Analyzing question structure and context for: " + prompt;
    }

    /**
     * Builds step-back generation prompt.
     */
    private String buildStepBackGenerationPrompt(String originalPrompt, QuestionAnalysis analysis, int level) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Original question: ").append(originalPrompt).append("\n");
        prompt.append("Question type: ").append(analysis.getQuestionType()).append("\n");
        prompt.append("Domain: ").append(analysis.getDomain()).append("\n\n");
        
        prompt.append("Generate a step-back question at abstraction level ").append(level).append(":\n");
        
        switch (level) {
            case 1:
                prompt.append("Create a slightly more general version that covers the immediate category.\n");
                prompt.append("Example: 'How to fix X?' → 'What are common solutions for X-type problems?'");
                break;
            case 2:
                prompt.append("Create a moderately abstract version about underlying principles.\n");
                prompt.append("Example: 'How to fix X?' → 'What principles guide troubleshooting in this domain?'");
                break;
            case 3:
                prompt.append("Create a highly abstract version about fundamental concepts.\n");
                prompt.append("Example: 'How to fix X?' → 'What are the fundamental problem-solving methodologies?'");
                break;
            default:
                prompt.append("Create a philosophical or theoretical version.\n");
                prompt.append("Example: 'How to fix X?' → 'What is the nature of problems and solutions?'");
        }
        
        return prompt.toString();
    }

    /**
     * Builds enhanced prompt with principles.
     */
    private String buildEnhancedPrompt(String originalPrompt, List<PrincipleAnswer> principles, 
                                     boolean includeExamples) {
        StringBuilder enhancedPrompt = new StringBuilder();
        
        enhancedPrompt.append("Based on the following general principles, answer the specific question.\n\n");
        enhancedPrompt.append("General Principles:\n");
        
        for (int i = 0; i < principles.size(); i++) {
            PrincipleAnswer principle = principles.get(i);
            enhancedPrompt.append(String.format("\nLevel %d (%s):\n", 
                principle.getQuestion().getAbstractionLevel(),
                principle.getQuestion().getAbstractionDescription()));
            
            for (String keyPrinciple : principle.getKeyPrinciples()) {
                enhancedPrompt.append("• ").append(keyPrinciple).append("\n");
            }
        }
        
        enhancedPrompt.append("\nSpecific Question: ").append(originalPrompt).append("\n\n");
        enhancedPrompt.append("Apply the above principles to provide a comprehensive answer");
        
        if (includeExamples) {
            enhancedPrompt.append(" with concrete examples");
        }
        
        enhancedPrompt.append(":");
        
        return enhancedPrompt.toString();
    }

    /**
     * Parses question analysis from response.
     */
    private QuestionAnalysis parseQuestionAnalysis(String response) {
        QuestionAnalysis analysis = new QuestionAnalysis();
        
        // Parse TYPE
        if (response.contains("TYPE:")) {
            int start = response.indexOf("TYPE:") + 5;
            int end = response.indexOf(",", start);
            if (end == -1) end = response.indexOf("\n", start);
            if (end != -1) {
                analysis.setQuestionType(response.substring(start, end).trim());
            }
        }
        
        // Parse DOMAIN
        if (response.contains("DOMAIN:")) {
            int start = response.indexOf("DOMAIN:") + 7;
            int end = response.indexOf(",", start);
            if (end == -1) end = response.indexOf("\n", start);
            if (end != -1) {
                analysis.setDomain(response.substring(start, end).trim());
            }
        }
        
        // Parse CONCEPTS
        if (response.contains("CONCEPTS:")) {
            int start = response.indexOf("CONCEPTS:") + 9;
            int end = response.indexOf(",", start);
            if (end == -1) end = response.indexOf("\n", start);
            if (end != -1) {
                String conceptsStr = response.substring(start, end).trim();
                analysis.setKeyConcepts(List.of(conceptsStr.split("\\s*,\\s*")));
            }
        }
        
        // Parse SPECIFICITY
        if (response.contains("SPECIFICITY:")) {
            int start = response.indexOf("SPECIFICITY:") + 12;
            int end = response.indexOf("\n", start);
            if (end == -1) end = response.length();
            analysis.setSpecificityLevel(response.substring(start, end).trim());
        }
        
        return analysis;
    }

    /**
     * Extracts key principles from an answer.
     */
    private List<String> extractKeyPrinciples(String answer) {
        List<String> principles = new ArrayList<>();
        
        // Simple extraction: look for bullet points or numbered items
        String[] lines = answer.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.matches("^[•\\-\\*]\\s+.*") || trimmed.matches("^\\d+[.)\\s]+.*")) {
                String principle = trimmed.replaceAll("^[•\\-\\*\\d.)\\s]+", "").trim();
                if (principle.length() > 20) { // Filter out very short items
                    principles.add(principle);
                }
            }
        }
        
        // If no structured principles found, extract first few sentences
        if (principles.isEmpty()) {
            String[] sentences = answer.split("[.!?]+");
            for (int i = 0; i < Math.min(3, sentences.length); i++) {
                if (sentences[i].trim().length() > 20) {
                    principles.add(sentences[i].trim() + ".");
                }
            }
        }
        
        return principles;
    }

    /**
     * Gets abstraction level description.
     */
    private String getAbstractionDescription(int level) {
        switch (level) {
            case 1: return "Immediate generalization";
            case 2: return "Underlying principles";
            case 3: return "Fundamental concepts";
            default: return "Theoretical foundations";
        }
    }

    /**
     * Formats question analysis for display.
     */
    private String formatAnalysis(QuestionAnalysis analysis) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("Question Analysis:\n");
        formatted.append("- Type: ").append(analysis.getQuestionType()).append("\n");
        formatted.append("- Domain: ").append(analysis.getDomain()).append("\n");
        formatted.append("- Key Concepts: ").append(String.join(", ", analysis.getKeyConcepts())).append("\n");
        formatted.append("- Specificity: ").append(analysis.getSpecificityLevel()).append("\n");
        return formatted.toString();
    }

    /**
     * Formats step-back questions for display.
     */
    private String formatStepBackQuestions(List<StepBackQuestion> questions) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("Generated Step-Back Questions:\n\n");
        
        for (StepBackQuestion question : questions) {
            formatted.append(String.format("Level %d (%s):\n%s\n\n",
                question.getAbstractionLevel(),
                question.getAbstractionDescription(),
                question.getQuestion()));
        }
        
        return formatted.toString();
    }

    /**
     * Builds reasoning summary.
     */
    private String buildReasoningSummary(QuestionAnalysis analysis, 
                                       List<StepBackQuestion> stepBackQuestions,
                                       List<PrincipleAnswer> principles) {
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("Step-Back Prompting Process:\n\n");
        reasoning.append("1. Analyzed question as ").append(analysis.getQuestionType());
        reasoning.append(" in ").append(analysis.getDomain()).append(" domain\n");
        reasoning.append("2. Generated ").append(stepBackQuestions.size());
        reasoning.append(" step-back questions at increasing abstraction levels\n");
        reasoning.append("3. Identified ").append(
            principles.stream().mapToInt(p -> p.getKeyPrinciples().size()).sum()
        );
        reasoning.append(" key principles from general questions\n");
        reasoning.append("4. Applied principles to answer the specific question");
        
        return reasoning.toString();
    }

    /**
     * Formats full output for visualization.
     */
    private String formatFullOutput(QuestionAnalysis analysis, List<StepBackQuestion> questions,
                                  List<PrincipleAnswer> principles, String finalAnswer) {
        StringBuilder output = new StringBuilder();
        output.append("Step-Back Prompting Results:\n\n");
        output.append(formatAnalysis(analysis)).append("\n");
        output.append(formatStepBackQuestions(questions)).append("\n");
        output.append("Key Principles Identified:\n");
        
        for (PrincipleAnswer principle : principles) {
            for (String keyPrinciple : principle.getKeyPrinciples()) {
                output.append("• ").append(keyPrinciple).append("\n");
            }
        }
        
        output.append("\nFinal Answer:\n").append(finalAnswer);
        
        return output.toString();
    }

    /**
     * Creates metadata for analysis.
     */
    private Map<String, Object> createAnalysisMetadata(QuestionAnalysis analysis) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("question_type", analysis.getQuestionType());
        metadata.put("domain", analysis.getDomain());
        metadata.put("concept_count", analysis.getKeyConcepts().size());
        metadata.put("specificity", analysis.getSpecificityLevel());
        metadata.put("visualization_type", "question_analysis");
        return metadata;
    }

    /**
     * Creates metadata for step-back questions.
     */
    private Map<String, Object> createStepBackMetadata(List<StepBackQuestion> questions) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("question_count", questions.size());
        metadata.put("abstraction_levels", questions.stream()
                .mapToInt(StepBackQuestion::getAbstractionLevel)
                .max().orElse(0));
        metadata.put("visualization_type", "step_back_questions");
        return metadata;
    }

    /**
     * Creates metadata for principles.
     */
    private Map<String, Object> createPrincipleMetadata(PrincipleAnswer principle) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("abstraction_level", principle.getQuestion().getAbstractionLevel());
        metadata.put("principle_count", principle.getKeyPrinciples().size());
        metadata.put("answer_length", principle.getAnswer().length());
        metadata.put("visualization_type", "principle_identification");
        return metadata;
    }

    /**
     * Creates metadata for application.
     */
    private Map<String, Object> createApplicationMetadata(int principleCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("applied_principles", principleCount);
        metadata.put("application_method", "principle_integration");
        metadata.put("visualization_type", "principle_application");
        return metadata;
    }

    /**
     * Builds error result.
     */
    private AgenticFlowResult buildErrorResult(String prompt, List<ProcessingStep> steps, Exception e) {
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt)
                .fullResponse("Error in step-back prompting: " + e.getMessage())
                .finalResponse("Error in step-back prompting: " + e.getMessage())
                .reasoning("Processing failed: " + e.getMessage())
                .addAllProcessingSteps(steps)
                .processingTime(0L)
                .responseChanged(false)
                .addMetric("error", true)
                .addMetric("error_message", e.getMessage())
                .build();
    }

    /**
     * Inner class for question analysis results.
     */
    private static class QuestionAnalysis {
        private String questionType = "unknown";
        private String domain = "general";
        private List<String> keyConcepts = new ArrayList<>();
        private String specificityLevel = "moderate";

        // Getters and setters
        public String getQuestionType() { return questionType; }
        public void setQuestionType(String questionType) { this.questionType = questionType; }
        
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        
        public List<String> getKeyConcepts() { return keyConcepts; }
        public void setKeyConcepts(List<String> keyConcepts) { this.keyConcepts = keyConcepts; }
        
        public String getSpecificityLevel() { return specificityLevel; }
        public void setSpecificityLevel(String specificityLevel) { this.specificityLevel = specificityLevel; }
    }

    /**
     * Inner class for step-back questions.
     */
    private static class StepBackQuestion {
        private final String question;
        private final int abstractionLevel;
        private final String abstractionDescription;

        public StepBackQuestion(String question, int abstractionLevel, String abstractionDescription) {
            this.question = question;
            this.abstractionLevel = abstractionLevel;
            this.abstractionDescription = abstractionDescription;
        }

        public String getQuestion() { return question; }
        public int getAbstractionLevel() { return abstractionLevel; }
        public String getAbstractionDescription() { return abstractionDescription; }
    }

    /**
     * Inner class for principle answers.
     */
    private static class PrincipleAnswer {
        private final StepBackQuestion question;
        private final String answer;
        private final List<String> keyPrinciples;

        public PrincipleAnswer(StepBackQuestion question, String answer, List<String> keyPrinciples) {
            this.question = question;
            this.answer = answer;
            this.keyPrinciples = keyPrinciples;
        }

        public StepBackQuestion getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public List<String> getKeyPrinciples() { return keyPrinciples; }
    }
}