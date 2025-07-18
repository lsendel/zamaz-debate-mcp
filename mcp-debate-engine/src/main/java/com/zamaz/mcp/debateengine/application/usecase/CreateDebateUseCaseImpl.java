package com.zamaz.mcp.debateengine.application.usecase;

import com.zamaz.mcp.debateengine.application.command.CreateDebateCommand;
import com.zamaz.mcp.debateengine.domain.model.*;
import com.zamaz.mcp.debateengine.domain.port.ContextRepository;
import com.zamaz.mcp.debateengine.domain.port.DebateRepository;
import com.zamaz.mcp.debateengine.domain.port.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Implementation of CreateDebateUseCase.
 */
@Service
@Transactional
public class CreateDebateUseCaseImpl implements CreateDebateUseCase {
    
    private final DebateRepository debateRepository;
    private final ContextRepository contextRepository;
    private final DomainEventPublisher eventPublisher;
    
    public CreateDebateUseCaseImpl(
            DebateRepository debateRepository,
            ContextRepository contextRepository,
            DomainEventPublisher eventPublisher) {
        this.debateRepository = Objects.requireNonNull(debateRepository);
        this.contextRepository = Objects.requireNonNull(contextRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }
    
    @Override
    public DebateId execute(CreateDebateCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        
        // Create debate
        DebateId debateId = DebateId.generate();
        DebateTopic topic = DebateTopic.of(command.topic());
        
        Debate debate = Debate.create(
            debateId,
            command.organizationId(),
            command.userId(),
            topic,
            command.description(),
            command.configuration()
        );
        
        // Create associated context
        ContextId contextId = ContextId.generate();
        Context context = Context.create(
            contextId,
            debateId,
            command.organizationId(),
            command.userId(),
            "Debate: " + topic.getSummary(),
            "Context for debate on: " + topic,
            command.configuration().maxRounds() * 2000, // Estimate max tokens
            4096 // Default window size
        );
        
        // Set context on debate
        debate.setContext(contextId);
        
        // Save entities
        contextRepository.save(context);
        Debate savedDebate = debateRepository.save(debate);
        
        // Publish events
        eventPublisher.publishAll(savedDebate.getAndClearEvents());
        
        return savedDebate.getId();
    }
}