// Neo4j initialization script for workflow constraints and indexes
// This script creates necessary constraints and indexes for optimal performance

// Workflow constraints
CREATE CONSTRAINT workflow_id_unique IF NOT EXISTS FOR (w:Workflow) REQUIRE w.id IS UNIQUE;
CREATE CONSTRAINT workflow_node_id_unique IF NOT EXISTS FOR (n:WorkflowNode) REQUIRE n.id IS UNIQUE;

// Workflow indexes for performance
CREATE INDEX workflow_organization_idx IF NOT EXISTS FOR (w:Workflow) ON (w.organizationId);
CREATE INDEX workflow_status_idx IF NOT EXISTS FOR (w:Workflow) ON (w.status);
CREATE INDEX workflow_name_idx IF NOT EXISTS FOR (w:Workflow) ON (w.name);
CREATE INDEX workflow_created_idx IF NOT EXISTS FOR (w:Workflow) ON (w.createdAt);
CREATE INDEX workflow_updated_idx IF NOT EXISTS FOR (w:Workflow) ON (w.updatedAt);

// WorkflowNode indexes
CREATE INDEX workflow_node_type_idx IF NOT EXISTS FOR (n:WorkflowNode) ON (n.type);
CREATE INDEX workflow_node_workflow_idx IF NOT EXISTS FOR (n:WorkflowNode) ON (n.workflowId);

// Composite indexes for common queries
CREATE INDEX workflow_org_status_idx IF NOT EXISTS FOR (w:Workflow) ON (w.organizationId, w.status);
CREATE INDEX workflow_org_created_idx IF NOT EXISTS FOR (w:Workflow) ON (w.organizationId, w.createdAt);
CREATE INDEX workflow_org_updated_idx IF NOT EXISTS FOR (w:Workflow) ON (w.organizationId, w.updatedAt);

// Text search indexes
CREATE FULLTEXT INDEX workflow_name_fulltext IF NOT EXISTS FOR (w:Workflow) ON EACH [w.name, w.description];
CREATE FULLTEXT INDEX workflow_node_fulltext IF NOT EXISTS FOR (n:WorkflowNode) ON EACH [n.name, n.description];