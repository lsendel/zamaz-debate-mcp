// Neo4j initialization script for workflow constraints and indexes
// This script creates necessary constraints and indexes for optimal performance

// Primary constraints for data integrity
CREATE CONSTRAINT workflow_id_unique IF NOT EXISTS FOR (w:Workflow) REQUIRE w.id IS UNIQUE;
CREATE CONSTRAINT workflow_node_id_unique IF NOT EXISTS FOR (n:WorkflowNode) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT workflow_edge_id_unique IF NOT EXISTS FOR ()-[e:CONNECTS_TO]-() REQUIRE e.id IS UNIQUE;

// Node existence constraints
CREATE CONSTRAINT workflow_organization_required IF NOT EXISTS FOR (w:Workflow) REQUIRE w.organizationId IS NOT NULL;
CREATE CONSTRAINT workflow_name_required IF NOT EXISTS FOR (w:Workflow) REQUIRE w.name IS NOT NULL;
CREATE CONSTRAINT workflow_status_required IF NOT EXISTS FOR (w:Workflow) REQUIRE w.status IS NOT NULL;
CREATE CONSTRAINT node_type_required IF NOT EXISTS FOR (n:WorkflowNode) REQUIRE n.type IS NOT NULL;

// Performance indexes for common queries
CREATE INDEX workflow_organization_idx IF NOT EXISTS FOR (w:Workflow) ON (w.organizationId);
CREATE INDEX workflow_status_idx IF NOT EXISTS FOR (w:Workflow) ON (w.status);
CREATE INDEX workflow_name_idx IF NOT EXISTS FOR (w:Workflow) ON (w.name);
CREATE INDEX workflow_created_idx IF NOT EXISTS FOR (w:Workflow) ON (w.createdAt);
CREATE INDEX workflow_updated_idx IF NOT EXISTS FOR (w:Workflow) ON (w.updatedAt);

// WorkflowNode indexes for efficient node queries
CREATE INDEX workflow_node_type_idx IF NOT EXISTS FOR (n:WorkflowNode) ON (n.type);
CREATE INDEX workflow_node_workflow_idx IF NOT EXISTS FOR (n:WorkflowNode) ON (n.workflowId);
CREATE INDEX workflow_node_position_idx IF NOT EXISTS FOR (n:WorkflowNode) ON (n.positionX, n.positionY);

// Composite indexes for complex queries
CREATE INDEX workflow_org_status_idx IF NOT EXISTS FOR (w:Workflow) ON (w.organizationId, w.status);
CREATE INDEX workflow_org_created_idx IF NOT EXISTS FOR (w:Workflow) ON (w.organizationId, w.createdAt);
CREATE INDEX workflow_org_updated_idx IF NOT EXISTS FOR (w:Workflow) ON (w.organizationId, w.updatedAt);
CREATE INDEX workflow_org_name_idx IF NOT EXISTS FOR (w:Workflow) ON (w.organizationId, w.name);

// Node relationship indexes for graph traversal performance
CREATE INDEX node_workflow_type_idx IF NOT EXISTS FOR (n:WorkflowNode) ON (n.workflowId, n.type);

// Edge relationship indexes
CREATE INDEX edge_source_target_idx IF NOT EXISTS FOR ()-[e:CONNECTS_TO]-() ON (e.sourceId, e.targetId);
CREATE INDEX edge_workflow_idx IF NOT EXISTS FOR ()-[e:CONNECTS_TO]-() ON (e.workflowId);
CREATE INDEX edge_type_idx IF NOT EXISTS FOR ()-[e:CONNECTS_TO]-() ON (e.type);

// Full-text search indexes for advanced search capabilities
CREATE FULLTEXT INDEX workflow_search_idx IF NOT EXISTS FOR (w:Workflow) ON EACH [w.name];
CREATE FULLTEXT INDEX workflow_node_search_idx IF NOT EXISTS FOR (n:WorkflowNode) ON EACH [n.name];

// Range indexes for time-based queries
CREATE RANGE INDEX workflow_created_range_idx IF NOT EXISTS FOR (w:Workflow) ON (w.createdAt);
CREATE RANGE INDEX workflow_updated_range_idx IF NOT EXISTS FOR (w:Workflow) ON (w.updatedAt);