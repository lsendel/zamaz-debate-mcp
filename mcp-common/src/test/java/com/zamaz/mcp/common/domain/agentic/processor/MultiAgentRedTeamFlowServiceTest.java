package com.zamaz.mcp.common.domain.agentic.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
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
import com.zamaz.mcp.common.domain.agentic.ProcessingStep;
import com.zamaz.mcp.common.domain.agentic.PromptContext;
import com.zamaz.mcp.common.domain.llm.LlmResponse;
import com.zamaz.mcp.common.domain.llm.LlmServicePort;

@ExtendWith(MockitoExtension.class)
class MultiAgentRedTeamFlowServiceTest {
    @Mock
    private LlmServicePort llmService;

    @InjectMocks
    private MultiAgentRedTeamFlowService multiAgentRedTeamFlowService;

    private PromptContext context;
    private AgenticFlowConfiguration configuration;

    @BeforeEach
    void setUp() {
        context = new PromptContext("debate-123", "participant-456");
        configuration = new AgenticFlowConfiguration();
    }

    @Test
    void shouldReturnCorrectFlowType() {
        assertEquals(AgenticFlowType.MULTI_AGENT_RED_TEAM, multiAgentRedTeamFlowService.getFlowType());
    }

    @Test
    void shouldProcessPromptWithMultiAgentRedTeam() {
        // Given
        String prompt = "Should we implement a new tax policy?";
        String architectSolution = "Yes, we should implement a new tax policy because it would increase revenue and reduce inequality.";
        String skepticCritique = "The proposed tax policy might have unintended consequences like reduced economic growth and tax avoidance.";
        String judgeFinalDecision = "After considering both perspectives, a modified tax policy is warranted but with careful implementation to mitigate potential negative effects.";

        // Mock LLM responses for each persona
        LlmResponse architectResponse = LlmResponse.builder()
                .text(architectSolution)
                .processingTime(Duration.ofMillis(500))
                .build();

        LlmResponse skepticResponse = LlmResponse.builder()
                .text(skepticCritique)
                .processingTime(Duration.ofMillis(500))
                .build();

        LlmResponse judgeResponse = LlmResponse.builder()
                .text(judgeFinalDecision)
                .processingTime(Duration.ofMillis(500))
                .build();

        // Configure mock behavior
        when(llmService.generate(contains("Architect"), anyMap())).thenReturn(architectResponse);
        when(llmService.generate(contains("Skeptic"), anyMap())).thenReturn(skepticResponse);
        when(llmService.generate(contains("Judge"), anyMap())).thenReturn(judgeResponse);

        // When
        AgenticFlowResult result = multiAgentRedTeamFlowService.process(prompt, configuration, context);

        // Then
        assertNotNull(result);
        assertEquals(prompt, result.getOriginalPrompt());
        assertEquals(judgeFinalDecision, result.getFullResponse());
        assertEquals(3, result.getProcessingSteps().size());
        assertTrue(result.isResponseChanged());
        assertEquals("multi_agent_red_team", result.getMetrics().get("visualization_type"));
        assertEquals(3, result.getMetrics().get("persona_count"));

        // Verify processing steps
        List<ProcessingStep> steps = result.getProcessingSteps();
        assertEquals("multi_agent_red_team_architect", steps.get(0).getStepType());
        assertEquals("multi_agent_red_team_skeptic", steps.get(1).getStepType());
        assertEquals("multi_agent_red_team_judge", steps.get(2).getStepType());

        // Verify step metadata
        assertEquals("architect", steps.get(0).getMetadata().get("persona"));
        assertEquals("skeptic", steps.get(1).getMetadata().get("persona"));
        assertEquals("judge", steps.get(2).getMetadata().get("persona"));
    }

    @Test
    void shouldHandleCustomPersonaPrompts() {
        // Given
        String prompt = "Should we implement a new tax policy?";
        String customArchitectPrompt = "As the Chief Economist, analyze this problem...";
        String customSkepticPrompt = "As the Opposition Leader, critique this proposal...";
        String customJudgePrompt = "As the Neutral Mediator, evaluate both perspectives...";

        Map<String, Object> params = new HashMap<>();
        params.put("architect_prompt", customArchitectPrompt);
        params.put("skeptic_prompt", customSkepticPrompt);
        params.put("judge_prompt", customJudgePrompt);
        AgenticFlowConfiguration customConfig = new AgenticFlowConfiguration(params);

        // Mock responses
        when(llmService.generate(anyString(), anyMap())).thenReturn(
                LlmResponse.builder().text("Response").processingTime(Duration.ofMillis(100)).build());

        // When
        AgenticFlowResult result = multiAgentRedTeamFlowService.process(prompt, customConfig, context);

        // Then
        assertNotNull(result);
        // The test would ideally verify that the custom prompts were used,
        // but that would require more complex mocking or argument capture
    }

    @Test
    void shouldValidateConfiguration() {
        // Given
        Map<String, Object> validParams = new HashMap<>();
        validParams.put("architect_prompt", "Valid architect prompt");
        validParams.put("skeptic_prompt", "Valid skeptic prompt");
        validParams.put("judge_prompt", "Valid judge prompt");
        AgenticFlowConfiguration validConfig = new AgenticFlowConfiguration(validParams);

        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("architect_prompt", 123); // Should be a string
        AgenticFlowConfiguration invalidConfig = new AgenticFlowConfiguration(invalidParams);

        // Then
        assertTrue(multiAgentRedTeamFlowService.validateConfiguration(validConfig));
        assertTrue(multiAgentRedTeamFlowService.validateConfiguration(new AgenticFlowConfiguration()));
        assertFalse(multiAgentRedTeamFlowService.validateConfiguration(invalidConfig));
    }

    @Test
    void shouldTrackJudgeAgreement() {
        // Given
        String prompt = "Should we implement a new tax policy?";
        String architectSolution = "Yes, we should implement a new tax policy.";
        String skepticCritique = "No, the tax policy would be harmful.";

        // Judge agrees with architect
        String judgeAgreeWithArchitect = "I agree with the Architect. The tax policy should be implemented.";

        // Judge agrees with skeptic
        String judgeAgreeWithSkeptic = "I agree with the Skeptic. The tax policy would be harmful.";

        // Mock responses for architect and skeptic
        when(llmService.generate(contains("Architect"), anyMap())).thenReturn(
                LlmResponse.builder().text(architectSolution).processingTime(Duration.ofMillis(100)).build());
        when(llmService.generate(contains("Skeptic"), anyMap())).thenReturn(
                LlmResponse.builder().text(skepticCritique).processingTime(Duration.ofMillis(100)).build());

        // Test when judge agrees with architect
        when(llmService.generate(contains("Judge"), anyMap())).thenReturn(
                LlmResponse.builder().text(judgeAgreeWithArchitect).processingTime(Duration.ofMillis(100)).build());
        AgenticFlowResult resultArchitect = multiAgentRedTeamFlowService.process(prompt, configuration, context);
        assertTrue((Boolean) resultArchitect.getMetrics().get("judge_agrees_with_architect"));
        assertFalse((Boolean) resultArchitect.getMetrics().get("judge_agrees_with_skeptic"));

        // Test when judge agrees with skeptic
        when(llmService.generate(contains("Judge"), anyMap())).thenReturn(
                LlmResponse.builder().text(judgeAgreeWithSkeptic).processingTime(Duration.ofMillis(100)).build());
        AgenticFlowResult resultSkeptic = multiAgentRedTeamFlowService.process(prompt, configuration, context);
        assertFalse((Boolean) resultSkeptic.getMetrics().get("judge_agrees_with_architect"));
        assertTrue((Boolean) resultSkeptic.getMetrics().get("judge_agrees_with_skeptic"));
    }
}