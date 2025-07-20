package com.zamaz.workflow.domain.repository;

import com.zamaz.workflow.domain.entity.Workflow;
import com.zamaz.workflow.domain.valueobject.WorkflowId;
import com.zamaz.workflow.domain.valueobject.OrganizationId;

import java.util.List;
import java.util.Optional;

public interface WorkflowRepository {
    void save(Workflow workflow);
    
    Optional<Workflow> findById(WorkflowId id);
    
    List<Workflow> findByOrganization(OrganizationId organizationId);
    
    List<Workflow> findActiveWorkflows(OrganizationId organizationId);
    
    void delete(WorkflowId id);
    
    boolean exists(WorkflowId id);
}