package com.zamaz.mcp.controller.service;

import com.zamaz.mcp.controller.dto.DebateDto;
import com.zamaz.mcp.controller.entity.Debate;
import com.zamaz.mcp.controller.entity.Round;
import com.zamaz.mcp.controller.exception.ResourceNotFoundException;
import com.zamaz.mcp.controller.repository.DebateRepository;
import com.zamaz.mcp.controller.repository.RoundRepository;
import com.zamaz.mcp.controller.statemachine.DebateEvents;
import com.zamaz.mcp.controller.statemachine.DebateStates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DebateLifecycleService {

    private final DebateRepository debateRepository;
    private final RoundRepository roundRepository;
    private final StateMachineFactory<DebateStates, DebateEvents> stateMachineFactory;
    private final OrchestrationService orchestrationService;

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
                .status(DebateStatus.CREATED)
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

    public DebateDto startDebate(UUID id) {
        log.debug("Starting debate with ID: {}", id);

        Debate debate = debateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found with ID: " + id));

        if (debate.getParticipants().size() < 2) {
            throw new IllegalStateException("Debate must have at least 2 participants");
        }

        StateMachine<DebateStates, DebateEvents> stateMachine = stateMachineFactory.getStateMachine(id.toString());
        stateMachine.sendEvent(DebateEvents.START);

        debate.setStatus(DebateStatus.IN_PROGRESS);
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
                .status(debate.getStatus().name())
                .settings(debate.getSettings())
                .createdAt(debate.getCreatedAt())
                .updatedAt(debate.getUpdatedAt())
                .startedAt(debate.getStartedAt())
                .completedAt(debate.getCompletedAt())
                .participantCount(debate.getParticipants().size())
                .build();
    }
}
