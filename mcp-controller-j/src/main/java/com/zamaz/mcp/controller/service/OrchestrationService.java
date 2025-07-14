package com.zamaz.mcp.controller.service;

import com.zamaz.mcp.controller.entity.*;
import com.zamaz.mcp.controller.integration.LlmServiceClient;
import com.zamaz.mcp.controller.repository.*;
import com.zamaz.mcp.controller.statemachine.DebateEvents;
import com.zamaz.mcp.controller.statemachine.DebateStates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrchestrationService {
    
    private final DebateRepository debateRepository;
    private final ParticipantRepository participantRepository;
    private final RoundRepository roundRepository;
    private final ResponseRepository responseRepository;
    private final LlmServiceClient llmServiceClient;
    private final StateMachineFactory<DebateStates, DebateEvents> stateMachineFactory;
    
    @Async
    public void orchestrateRound(UUID debateId, UUID roundId) {
        log.info("Orchestrating round {} for debate {}", roundId, debateId);
        
        try {
            Round round = roundRepository.findById(roundId).orElseThrow();
            Debate debate = round.getDebate();
            List<Participant> aiParticipants = participantRepository.findByDebateIdAndType(debateId, "ai");
            
            // Get previous responses for context
            List<Response> previousResponses = getPreviousResponses(debate);
            
            // Generate AI responses
            for (Participant participant : aiParticipants) {
                if (!responseRepository.existsByRoundIdAndParticipantId(roundId, participant.getId())) {
                    generateAiResponse(debate, round, participant, previousResponses);
                }
            }
            
        } catch (Exception e) {
            log.error("Error orchestrating round {} for debate {}", roundId, debateId, e);
        }
    }
    
    public void checkRoundCompletion(UUID debateId, UUID roundId) {
        log.debug("Checking round completion for round {}", roundId);
        
        Round round = roundRepository.findById(roundId).orElseThrow();
        Debate debate = round.getDebate();
        List<Participant> participants = debate.getParticipants();
        List<Response> responses = responseRepository.findByRoundId(roundId);
        
        // Check if all participants have responded
        if (responses.size() >= participants.size()) {
            completeRound(debate, round);
        }
    }
    
    private void completeRound(Debate debate, Round round) {
        log.info("Completing round {} for debate {}", round.getRoundNumber(), debate.getId());
        
        round.setStatus("COMPLETED");
        round.setCompletedAt(LocalDateTime.now());
        roundRepository.save(round);
        
        StateMachine<DebateStates, DebateEvents> stateMachine = stateMachineFactory.getStateMachine(debate.getId().toString());
        stateMachine.sendEvent(DebateEvents.COMPLETE_ROUND);
        
        // Check if debate is complete
        if (round.getRoundNumber() >= debate.getMaxRounds()) {
            completeDebate(debate);
        } else {
            // Start next round
            startNextRound(debate);
        }
    }
    
    private void startNextRound(Debate debate) {
        int nextRoundNumber = debate.getCurrentRound() + 1;
        log.info("Starting round {} for debate {}", nextRoundNumber, debate.getId());
        
        debate.setCurrentRound(nextRoundNumber);
        debateRepository.save(debate);
        
        Round nextRound = Round.builder()
                .debate(debate)
                .roundNumber(nextRoundNumber)
                .status("IN_PROGRESS")
                .startedAt(LocalDateTime.now())
                .build();
        
        nextRound = roundRepository.save(nextRound);
        
        StateMachine<DebateStates, DebateEvents> stateMachine = stateMachineFactory.getStateMachine(debate.getId().toString());
        stateMachine.sendEvent(DebateEvents.START);
        
        // Orchestrate AI responses for the new round
        orchestrateRound(debate.getId(), nextRound.getId());
    }
    
    private void completeDebate(Debate debate) {
        log.info("Completing debate {}", debate.getId());
        
        debate.setStatus(DebateStates.DEBATE_COMPLETE.name());
        debate.setCompletedAt(LocalDateTime.now());
        debateRepository.save(debate);
        
        StateMachine<DebateStates, DebateEvents> stateMachine = stateMachineFactory.getStateMachine(debate.getId().toString());
        stateMachine.sendEvent(DebateEvents.END_DEBATE);
        
        // Calculate and save results
        calculateDebateResults(debate);
    }
    
    private void generateAiResponse(Debate debate, Round round, Participant participant, List<Response> previousResponses) {
        log.debug("Generating AI response for participant {} in round {}", participant.getName(), round.getRoundNumber());
        
        try {
            // Build context from previous responses
            String context = buildContext(debate, participant, previousResponses);
            
            // Prepare messages for LLM
            List<Map<String, String>> messages = new ArrayList<>();
            
            // System message
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", String.format(
                "You are %s, participating in a debate about: %s. Your position is: %s. " +
                "Respond with a clear, concise argument supporting your position. " +
                "Keep your response under 300 words.",
                participant.getName(), debate.getTopic(), participant.getPosition()
            ));
            messages.add(systemMessage);
            
            // Add context as user message
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", context);
            messages.add(userMessage);
            
            // Call LLM service
            Map<String, Object> completionRequest = new HashMap<>();
            completionRequest.put("provider", participant.getProvider());
            completionRequest.put("model", participant.getModel());
            completionRequest.put("messages", messages);
            completionRequest.put("maxTokens", 500);
            completionRequest.put("temperature", 0.7);
            
            Map<String, Object> llmResponse = llmServiceClient.generateCompletion(completionRequest);
            
            // Extract response content
            List<Map<String, Object>> choices = (List<Map<String, Object>>) llmResponse.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            
            // Save response
            Response response = Response.builder()
                    .round(round)
                    .participant(participant)
                    .content(content)
                    .tokenCount(content.split("\\s+").length)
                    .build();
            
            responseRepository.save(response);
            log.info("Generated AI response for participant {} in round {}", participant.getName(), round.getRoundNumber());
            
            // Check if round is complete
            checkRoundCompletion(debate.getId(), round.getId());
            
        } catch (Exception e) {
            log.error("Error generating AI response for participant {}", participant.getName(), e);
        }
    }
    
    private String buildContext(Debate debate, Participant participant, List<Response> previousResponses) {
        StringBuilder context = new StringBuilder();
        context.append("Debate Topic: ").append(debate.getTopic()).append("\n\n");
        
        if (previousResponses.isEmpty()) {
            context.append("This is the opening round. Present your initial argument.");
        } else {
            context.append("Previous arguments:\n\n");
            
            // Group responses by round
            Map<Integer, List<Response>> responsesByRound = previousResponses.stream()
                    .collect(Collectors.groupingBy(r -> r.getRound().getRoundNumber()));
            
            for (Map.Entry<Integer, List<Response>> entry : responsesByRound.entrySet()) {
                context.append("Round ").append(entry.getKey()).append(":\n");
                
                for (Response response : entry.getValue()) {
                    context.append(response.getParticipant().getName())
                           .append(" (").append(response.getParticipant().getPosition()).append("): ")
                           .append(response.getContent())
                           .append("\n\n");
                }
            }
            
            context.append("Now provide your response for round ").append(debate.getCurrentRound()).append(".");
        }
        
        return context.toString();
    }
    
    private List<Response> getPreviousResponses(Debate debate) {
        return debate.getRounds().stream()
                .filter(round -> round.getStatus().equals("COMPLETED"))
                .flatMap(round -> round.getResponses().stream())
                .sorted(Comparator.comparing(Response::getCreatedAt))
                .collect(Collectors.toList());
    }
    
    private void calculateDebateResults(Debate debate) {
        // This is a placeholder for debate result calculation
        // In a real implementation, this would analyze responses,
        // potentially use an LLM to judge arguments, and determine a winner
        log.info("Calculating results for debate {}", debate.getId());
    }
}