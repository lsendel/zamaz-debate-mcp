package com.zamaz.mcp.controller.service;

import com.zamaz.mcp.controller.dto.DebateDto;
import com.zamaz.mcp.controller.dto.ParticipantDto;
import com.zamaz.mcp.controller.dto.ResponseDto;
import com.zamaz.mcp.controller.entity.*;
import com.zamaz.mcp.controller.exception.ResourceNotFoundException;
import com.zamaz.mcp.controller.repository.DebateRepository;
import com.zamaz.mcp.controller.repository.ParticipantRepository;
import com.zamaz.mcp.controller.repository.RoundRepository;
import com.zamaz.mcp.controller.repository.ResponseRepository;
import com.zamaz.mcp.controller.statemachine.DebateEvents;
import com.zamaz.mcp.controller.statemachine.DebateStates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DebateService {
    
    private final DebateRepository debateRepository;
    private final ParticipantRepository participantRepository;
    private final RoundRepository roundRepository;
    private final ResponseRepository responseRepository;
    private final StateMachineFactory<DebateStates, DebateEvents> stateMachineFactory;
    private final OrchestrationService orchestrationService;
    
    // Map to store event sinks for each debate
    private final Map<String, Sinks.Many<Map<String, Object>>> debateEventSinks = new ConcurrentHashMap<>();
    
    public DebateDto createDebate(DebateDto.CreateDebateRequest request) {
        log.debug("Creating debate with title: {}", request.getTitle());
        
        Debate debate = Debate.builder()
                .organizationId(request.getOrganizationId())
                .title(request.getTitle())
                .description(request.getDescription())
                .topic(request.getTopic())
                .format(request.getFormat())
                .maxRounds(request.getMaxRounds() != null ? request.getMaxRounds() : 3)
                .settings(request.getSettings())
                .status(DebateStates.CREATED.name())
                .build();
        
        debate = debateRepository.save(debate);
        log.info("Created debate with ID: {}", debate.getId());
        
        // Initialize state machine
        StateMachine<DebateStates, DebateEvents> stateMachine = stateMachineFactory.getStateMachine(debate.getId().toString());
        stateMachine.start();
        stateMachine.sendEvent(DebateEvents.INITIALIZE);
        
        debate.setStatus(DebateStates.INITIALIZED.name());
        debate = debateRepository.save(debate);
        
        return toDto(debate);
    }
    
    public DebateDto getDebate(UUID id) {
        log.debug("Getting debate with ID: {}", id);
        Debate debate = debateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found with ID: " + id));
        return toDto(debate);
    }
    
    public Page<DebateDto> listDebates(UUID organizationId, String status, Pageable pageable) {
        log.debug("Listing debates for organization: {}, status: {}", organizationId, status);
        
        Page<Debate> debates;
        if (organizationId != null && status != null) {
            debates = debateRepository.findByOrganizationIdAndStatus(organizationId, status, pageable);
        } else if (organizationId != null) {
            debates = debateRepository.findByOrganizationId(organizationId, pageable);
        } else if (status != null) {
            debates = debateRepository.findByStatus(status, pageable);
        } else {
            debates = debateRepository.findAll(pageable);
        }
        
        return debates.map(this::toDto);
    }
    
    public DebateDto updateDebate(UUID id, DebateDto.UpdateDebateRequest request) {
        log.debug("Updating debate with ID: {}", id);
        
        Debate debate = debateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found with ID: " + id));
        
        if (request.getTitle() != null) {
            debate.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            debate.setDescription(request.getDescription());
        }
        if (request.getTopic() != null) {
            debate.setTopic(request.getTopic());
        }
        if (request.getMaxRounds() != null) {
            debate.setMaxRounds(request.getMaxRounds());
        }
        if (request.getSettings() != null) {
            debate.setSettings(request.getSettings());
        }
        
        debate = debateRepository.save(debate);
        log.info("Updated debate with ID: {}", debate.getId());
        
        return toDto(debate);
    }
    
    public void deleteDebate(UUID id) {
        log.debug("Deleting debate with ID: {}", id);
        
        if (!debateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Debate not found with ID: " + id);
        }
        
        debateRepository.deleteById(id);
        log.info("Deleted debate with ID: {}", id);
    }
    
    public DebateDto startDebate(UUID id) {
        log.debug("Starting debate with ID: {}", id);
        
        Debate debate = debateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found with ID: " + id));
        
        if (debate.getParticipants().size() < 2) {
            throw new IllegalStateException("Debate must have at least 2 participants");
        }
        
        StateMachine<DebateStates, DebateEvents> stateMachine = stateMachineFactory.getStateMachine(id.toString());
        stateMachine.sendEvent(DebateEvents.START);
        
        debate.setStatus(DebateStates.IN_PROGRESS.name());
        debate.setStartedAt(LocalDateTime.now());
        debate.setCurrentRound(1);
        
        // Create first round
        Round round = Round.builder()
                .debate(debate)
                .roundNumber(1)
                .status("IN_PROGRESS")
                .startedAt(LocalDateTime.now())
                .build();
        
        roundRepository.save(round);
        debate = debateRepository.save(debate);
        
        // Trigger AI responses if needed
        orchestrationService.orchestrateRound(debate.getId(), round.getId());
        
        log.info("Started debate with ID: {}", debate.getId());
        return toDto(debate);
    }
    
    public ParticipantDto addParticipant(UUID debateId, ParticipantDto.CreateParticipantRequest request) {
        log.debug("Adding participant to debate: {}", debateId);
        
        Debate debate = debateRepository.findById(debateId)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found with ID: " + debateId));
        
        if ("ai".equals(request.getType()) && (request.getProvider() == null || request.getModel() == null)) {
            throw new IllegalArgumentException("Provider and model are required for AI participants");
        }
        
        Participant participant = Participant.builder()
                .debate(debate)
                .name(request.getName())
                .type(request.getType())
                .provider(request.getProvider())
                .model(request.getModel())
                .position(request.getPosition())
                .settings(request.getSettings())
                .build();
        
        participant = participantRepository.save(participant);
        log.info("Added participant {} to debate {}", participant.getId(), debateId);
        
        return toParticipantDto(participant);
    }
    
    public void removeParticipant(UUID debateId, UUID participantId) {
        log.debug("Removing participant {} from debate {}", participantId, debateId);
        
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with ID: " + participantId));
        
        if (!participant.getDebate().getId().equals(debateId)) {
            throw new IllegalArgumentException("Participant does not belong to this debate");
        }
        
        participantRepository.delete(participant);
        log.info("Removed participant {} from debate {}", participantId, debateId);
    }
    
    public ResponseDto submitResponse(UUID debateId, UUID roundId, ResponseDto.CreateResponseRequest request) {
        log.debug("Submitting response for debate: {}, round: {}", debateId, roundId);
        
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found with ID: " + roundId));
        
        if (!round.getDebate().getId().equals(debateId)) {
            throw new IllegalArgumentException("Round does not belong to this debate");
        }
        
        Participant participant = participantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with ID: " + request.getParticipantId()));
        
        Response response = Response.builder()
                .round(round)
                .participant(participant)
                .content(request.getContent())
                .tokenCount(request.getContent().split("\\s+").length) // Simple token count
                .build();
        
        response = responseRepository.save(response);
        log.info("Submitted response {} for round {}", response.getId(), roundId);
        
        // Check if round is complete
        orchestrationService.checkRoundCompletion(debateId, roundId);
        
        return toResponseDto(response);
    }
    
    public List<Object> listRounds(UUID debateId) {
        log.debug("Listing rounds for debate: {}", debateId);
        
        return roundRepository.findByDebateIdOrderByRoundNumber(debateId).stream()
                .map(round -> {
                    var roundData = new java.util.HashMap<String, Object>();
                    roundData.put("id", round.getId());
                    roundData.put("roundNumber", round.getRoundNumber());
                    roundData.put("status", round.getStatus());
                    roundData.put("startedAt", round.getStartedAt());
                    roundData.put("completedAt", round.getCompletedAt());
                    roundData.put("responses", round.getResponses().stream()
                            .map(this::toResponseDto)
                            .collect(Collectors.toList()));
                    return roundData;
                })
                .collect(Collectors.toList());
    }
    
    public Object getResults(UUID debateId) {
        log.debug("Getting results for debate: {}", debateId);
        
        Debate debate = debateRepository.findById(debateId)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found with ID: " + debateId));
        
        // This would be enhanced with actual scoring logic
        var results = new java.util.HashMap<String, Object>();
        results.put("debateId", debateId);
        results.put("status", debate.getStatus());
        results.put("totalRounds", debate.getCurrentRound());
        results.put("participants", debate.getParticipants().stream()
                .map(p -> {
                    var pData = new java.util.HashMap<String, Object>();
                    pData.put("name", p.getName());
                    pData.put("position", p.getPosition());
                    pData.put("responseCount", p.getResponses().size());
                    return pData;
                })
                .collect(Collectors.toList()));
        
        return results;
    }
    
    private DebateDto toDto(Debate debate) {
        return DebateDto.builder()
                .id(debate.getId())
                .organizationId(debate.getOrganizationId())
                .title(debate.getTitle())
                .description(debate.getDescription())
                .topic(debate.getTopic())
                .format(debate.getFormat())
                .maxRounds(debate.getMaxRounds())
                .currentRound(debate.getCurrentRound())
                .status(debate.getStatus())
                .settings(debate.getSettings())
                .createdAt(debate.getCreatedAt())
                .updatedAt(debate.getUpdatedAt())
                .startedAt(debate.getStartedAt())
                .completedAt(debate.getCompletedAt())
                .participantCount(debate.getParticipants().size())
                .build();
    }
    
    private ParticipantDto toParticipantDto(Participant participant) {
        return ParticipantDto.builder()
                .id(participant.getId())
                .debateId(participant.getDebate().getId())
                .name(participant.getName())
                .type(participant.getType())
                .provider(participant.getProvider())
                .model(participant.getModel())
                .position(participant.getPosition())
                .settings(participant.getSettings())
                .createdAt(participant.getCreatedAt())
                .build();
    }
    
    private ResponseDto toResponseDto(Response response) {
        return ResponseDto.builder()
                .id(response.getId())
                .roundId(response.getRound().getId())
                .participantId(response.getParticipant().getId())
                .participantName(response.getParticipant().getName())
                .content(response.getContent())
                .tokenCount(response.getTokenCount())
                .createdAt(response.getCreatedAt())
                .build();
    }
    
    /**
     * Subscribe to debate events for WebSocket updates
     */
    public Flux<Map<String, Object>> subscribeToDebateEvents(String debateId) {
        log.debug("Creating event subscription for debate: {}", debateId);
        
        Sinks.Many<Map<String, Object>> sink = debateEventSinks.computeIfAbsent(
            debateId, 
            k -> Sinks.many().multicast().onBackpressureBuffer()
        );
        
        return sink.asFlux()
            .doOnSubscribe(subscription -> 
                log.info("New subscription created for debate: {}", debateId))
            .doOnCancel(() -> 
                log.info("Subscription cancelled for debate: {}", debateId))
            .timeout(Duration.ofHours(1)) // Auto-cleanup after 1 hour
            .doOnError(error -> 
                log.error("Error in debate event stream for {}: {}", debateId, error.getMessage()));
    }
    
    /**
     * Publish event to debate subscribers
     */
    public void publishDebateEvent(String debateId, Map<String, Object> event) {
        Sinks.Many<Map<String, Object>> sink = debateEventSinks.get(debateId);
        if (sink != null) {
            sink.tryEmitNext(event);
            log.debug("Published event to debate {}: {}", debateId, event.get("type"));
        }
    }
    
    /**
     * Clean up event subscription when debate is complete
     */
    public void cleanupDebateEvents(String debateId) {
        Sinks.Many<Map<String, Object>> sink = debateEventSinks.remove(debateId);
        if (sink != null) {
            sink.tryEmitComplete();
            log.info("Cleaned up event subscription for debate: {}", debateId);
        }
    }
    
    /**
     * Get active subscription count for monitoring
     */
    public int getActiveSubscriptionCount() {
        return debateEventSinks.size();
    }
}