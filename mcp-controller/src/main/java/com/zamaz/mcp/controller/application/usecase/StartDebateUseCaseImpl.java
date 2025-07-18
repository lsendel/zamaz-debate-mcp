package com.zamaz.mcp.controller.application.usecase;

import com.zamaz.mcp.controller.application.command.StartDebateCommand;
import com.zamaz.mcp.controller.application.exception.DebateNotFoundException;
import com.zamaz.mcp.controller.domain.model.Debate;
import com.zamaz.mcp.controller.domain.port.DebateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/**
 * Implementation of StartDebateUseCase.
 */
@Service
@Transactional
public class StartDebateUseCaseImpl implements StartDebateUseCase {
    
    private final DebateRepository debateRepository;
    
    public StartDebateUseCaseImpl(DebateRepository debateRepository) {
        this.debateRepository = Objects.requireNonNull(debateRepository, "Debate repository cannot be null");
    }
    
    @Override
    public void execute(StartDebateCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        
        // Find the debate
        Debate debate = debateRepository.findById(command.debateId())
            .orElseThrow(() -> new DebateNotFoundException("Debate not found: " + command.debateId()));
        
        // Initialize if needed
        if (debate.getStatus().canAcceptParticipants()) {
            debate.initialize();
        }
        
        // Start the debate
        debate.start();
        
        // Save the updated debate
        debateRepository.save(debate);
    }
}