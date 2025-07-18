package com.zamaz.mcp.controller.application.usecase;

import com.zamaz.mcp.controller.application.command.SubmitResponseCommand;
import com.zamaz.mcp.controller.application.exception.DebateNotFoundException;
import com.zamaz.mcp.controller.domain.model.Debate;
import com.zamaz.mcp.controller.domain.model.ResponseId;
import com.zamaz.mcp.controller.domain.port.DebateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/**
 * Implementation of SubmitResponseUseCase.
 */
@Service
@Transactional
public class SubmitResponseUseCaseImpl implements SubmitResponseUseCase {
    
    private final DebateRepository debateRepository;
    
    public SubmitResponseUseCaseImpl(DebateRepository debateRepository) {
        this.debateRepository = Objects.requireNonNull(debateRepository, "Debate repository cannot be null");
    }
    
    @Override
    public ResponseId execute(SubmitResponseCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        
        // Find the debate
        Debate debate = debateRepository.findById(command.debateId())
            .orElseThrow(() -> new DebateNotFoundException("Debate not found: " + command.debateId()));
        
        // Generate response ID (done internally by debate)
        ResponseId responseId = ResponseId.generate();
        
        // Submit the response to the debate
        debate.submitResponse(
            command.participantId(),
            command.content(),
            command.getResponseTime().orElse(null)
        );
        
        // Save the updated debate
        debateRepository.save(debate);
        
        // Get the response ID from the latest response
        // Note: In a real implementation, you'd return the actual response ID from the domain
        return debate.getCurrentRound()
            .flatMap(round -> round.getResponseFromParticipant(command.participantId()))
            .map(response -> response.getId())
            .orElse(responseId);
    }
}