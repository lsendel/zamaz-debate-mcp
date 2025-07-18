package com.zamaz.mcp.controller.application.usecase;

import com.zamaz.mcp.controller.application.command.CreateDebateCommand;
import com.zamaz.mcp.controller.domain.model.Debate;
import com.zamaz.mcp.controller.domain.model.DebateId;
import com.zamaz.mcp.controller.domain.port.DebateRepository;
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
    
    public CreateDebateUseCaseImpl(DebateRepository debateRepository) {
        this.debateRepository = Objects.requireNonNull(debateRepository, "Debate repository cannot be null");
    }
    
    @Override
    public DebateId execute(CreateDebateCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        
        // Generate new debate ID
        DebateId debateId = DebateId.generate();
        
        // Create the debate
        Debate debate = Debate.create(debateId, command.topic(), command.config());
        
        // Save the debate
        debateRepository.save(debate);
        
        return debateId;
    }
}