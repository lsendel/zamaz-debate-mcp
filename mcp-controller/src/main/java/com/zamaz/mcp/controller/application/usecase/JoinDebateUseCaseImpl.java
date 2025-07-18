package com.zamaz.mcp.controller.application.usecase;

import com.zamaz.mcp.controller.application.command.JoinDebateCommand;
import com.zamaz.mcp.controller.domain.model.Debate;
import com.zamaz.mcp.controller.domain.model.Participant;
import com.zamaz.mcp.controller.domain.model.ParticipantId;
import com.zamaz.mcp.controller.domain.port.DebateRepository;
import com.zamaz.mcp.controller.application.exception.DebateNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/**
 * Implementation of JoinDebateUseCase.
 */
@Service
@Transactional
public class JoinDebateUseCaseImpl implements JoinDebateUseCase {
    
    private final DebateRepository debateRepository;
    
    public JoinDebateUseCaseImpl(DebateRepository debateRepository) {
        this.debateRepository = Objects.requireNonNull(debateRepository, "Debate repository cannot be null");
    }
    
    @Override
    public ParticipantId execute(JoinDebateCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        
        // Find the debate
        Debate debate = debateRepository.findById(command.debateId())
            .orElseThrow(() -> new DebateNotFoundException("Debate not found: " + command.debateId()));
        
        // Generate participant ID
        ParticipantId participantId = ParticipantId.generate();
        
        // Create participant based on type
        Participant participant;
        if (command.isHuman()) {
            participant = Participant.createHuman(
                participantId,
                command.participantName(),
                command.position()
            );
        } else {
            participant = Participant.createAI(
                participantId,
                command.participantName(),
                command.position(),
                command.getProvider().orElseThrow(),
                command.getProviderConfig().orElseThrow()
            );
        }
        
        // Add participant to debate
        debate.addParticipant(participant);
        
        // Save the updated debate
        debateRepository.save(debate);
        
        return participantId;
    }
}