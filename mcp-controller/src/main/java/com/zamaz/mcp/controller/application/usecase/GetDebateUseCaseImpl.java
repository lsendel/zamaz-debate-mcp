package com.zamaz.mcp.controller.application.usecase;

import com.zamaz.mcp.controller.application.query.GetDebateQuery;
import com.zamaz.mcp.controller.domain.model.Debate;
import com.zamaz.mcp.controller.domain.port.DebateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of GetDebateUseCase.
 */
@Service
@Transactional(readOnly = true)
public class GetDebateUseCaseImpl implements GetDebateUseCase {
    
    private final DebateRepository debateRepository;
    
    public GetDebateUseCaseImpl(DebateRepository debateRepository) {
        this.debateRepository = Objects.requireNonNull(debateRepository, "Debate repository cannot be null");
    }
    
    @Override
    public Optional<Debate> execute(GetDebateQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        
        return debateRepository.findById(query.debateId());
    }
}