package com.zamaz.mcp.debateengine.application.usecase;

import com.zamaz.mcp.debateengine.application.command.StartDebateCommand;
import com.zamaz.mcp.debateengine.domain.model.Debate;
import com.zamaz.mcp.debateengine.domain.port.DebateRepository;
import com.zamaz.mcp.debateengine.domain.port.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.Objects;

/**
 * Implementation of StartDebateUseCase.
 */
@Service
@Transactional
public class StartDebateUseCaseImpl implements StartDebateUseCase {
    
    private final DebateRepository debateRepository;
    private final DomainEventPublisher eventPublisher;
    
    public StartDebateUseCaseImpl(
            DebateRepository debateRepository,
            DomainEventPublisher eventPublisher) {
        this.debateRepository = Objects.requireNonNull(debateRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }
    
    @Override
    public void execute(StartDebateCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        
        // Find debate
        Debate debate = debateRepository.findById(command.debateId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Debate not found: " + command.debateId()
            ));
        
        // Start the debate
        debate.start();
        
        // Start first round with default prompt
        String firstRoundPrompt = createFirstRoundPrompt(debate);
        debate.startNewRound(firstRoundPrompt);
        
        // Save debate
        Debate savedDebate = debateRepository.save(debate);
        
        // Publish events
        eventPublisher.publishAll(savedDebate.getAndClearEvents());
    }
    
    private String createFirstRoundPrompt(Debate debate) {
        return String.format(
            "Welcome to the debate on: %s\n\n" +
            "This is round 1 of %d. Each participant should present their opening " +
            "position on the topic. Be clear, concise, and persuasive.",
            debate.getTopic().toString(),
            debate.getConfiguration().maxRounds()
        );
    }
}