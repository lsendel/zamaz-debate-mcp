package com.zamaz.mcp.debateengine.application.usecase;

import com.zamaz.mcp.debateengine.application.query.GetDebateQuery;
import com.zamaz.mcp.debateengine.domain.model.Debate;
import com.zamaz.mcp.debateengine.domain.port.DebateRepository;
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
        this.debateRepository = Objects.requireNonNull(debateRepository);
    }
    
    @Override
    public Optional<Debate> execute(GetDebateQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        
        return debateRepository.findByIdAndOrganization(
            query.debateId(),
            query.organizationId()
        );
    }
}