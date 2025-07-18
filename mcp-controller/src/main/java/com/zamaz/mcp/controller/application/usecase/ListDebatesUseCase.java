package com.zamaz.mcp.controller.application.usecase;

import com.zamaz.mcp.controller.application.query.ListDebatesQuery;
import com.zamaz.mcp.controller.domain.model.Debate;
import java.util.List;

/**
 * Use case for listing debates with optional filtering.
 */
public interface ListDebatesUseCase {
    
    /**
     * Lists debates based on the query criteria.
     * 
     * @param query the list debates query
     * @return list of debates matching the criteria
     */
    List<Debate> execute(ListDebatesQuery query);
}