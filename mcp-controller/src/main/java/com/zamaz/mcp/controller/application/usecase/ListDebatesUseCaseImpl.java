package com.zamaz.mcp.controller.application.usecase;

import com.zamaz.mcp.controller.application.query.ListDebatesQuery;
import com.zamaz.mcp.controller.domain.model.Debate;
import com.zamaz.mcp.controller.domain.port.DebateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.List;

/**
 * Implementation of ListDebatesUseCase.
 */
@Service
@Transactional(readOnly = true)
public class ListDebatesUseCaseImpl implements ListDebatesUseCase {
    
    private final DebateRepository debateRepository;
    
    public ListDebatesUseCaseImpl(DebateRepository debateRepository) {
        this.debateRepository = Objects.requireNonNull(debateRepository, "Debate repository cannot be null");
    }
    
    @Override
    public List<Debate> execute(ListDebatesQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        
        return debateRepository.findByFilters(
            query.statuses(),
            query.getTopicFilter().orElse(null),
            query.getLimit().orElse(null),
            query.getOffset().orElse(null)
        );
    }
}