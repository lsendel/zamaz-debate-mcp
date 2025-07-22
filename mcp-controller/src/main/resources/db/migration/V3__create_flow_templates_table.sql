-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Constants
DECLARE
  C_DEFAULT_SCHEMA CONSTANT VARCHAR2(30) := 'PUBLIC';
  C_ERROR_MSG CONSTANT VARCHAR2(100) := 'An error occurred';
END;
/

-- Create flow templates table
CREATE TABLE IF NOT EXISTS flow_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    flow_type VARCHAR(50) NOT NULL,
    configuration JSONB NOT NULL,
    category VARCHAR(100),
    is_public BOOLEAN DEFAULT false,
    organization_id UUID,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usage_count INTEGER DEFAULT 0,
    rating DECIMAL(3,2),
    tags TEXT[],
    CONSTRAINT uk_flow_templates_name_org UNIQUE (name, organization_id),
    CONSTRAINT fk_flow_templates_org FOREIGN KEY (organization_id) 
        REFERENCES organizations(id) ON DELETE CASCADE
);

-- Indexes for templates
CREATE INDEX idx_flow_templates_org ON flow_templates(organization_id);
CREATE INDEX idx_flow_templates_public ON flow_templates(is_public) WHERE is_public = true;
CREATE INDEX idx_flow_templates_category ON flow_templates(category);
CREATE INDEX idx_flow_templates_flow_type ON flow_templates(flow_type);
CREATE INDEX idx_flow_templates_rating ON flow_templates(rating DESC) WHERE rating IS NOT NULL;
CREATE INDEX idx_flow_templates_tags ON flow_templates USING gin(tags);

-- Insert default templates
INSERT INTO flow_templates (name, description, flow_type, configuration, category, is_public, created_by, tags) VALUES
(
    'Fact-Checking Debate',
    'Comprehensive fact verification for debates requiring accuracy',
    'TOOL_CALLING_VERIFICATION',
    '{
        "allowedTools": ["web_search", "calculator", "database_query"],
        "maxToolCalls": 5,
        "verificationPrompt": "Verify the following claims using available tools:",
        "timeout": 30000
    }'::jsonb,
    'factual',
    true,
    '00000000-0000-0000-0000-000000000000',
    ARRAY['fact-checking', 'verification', 'research']
),
(
    'Philosophical Discussion',
    'Deep reasoning for philosophical and ethical debates',
    'TREE_OF_THOUGHTS',
    '{
        "maxDepth": 3,
        "branchingFactor": 3,
        "evaluationMetric": "philosophical_coherence",
        "explorationStrategy": "depth_first"
    }'::jsonb,
    'philosophical',
    true,
    '00000000-0000-0000-0000-000000000000',
    ARRAY['philosophy', 'ethics', 'reasoning']
),
(
    'Technical Analysis',
    'Structured analysis for technical topics',
    'INTERNAL_MONOLOGUE',
    '{
        "prefix": "Let me analyze this technical question step by step:",
        "showReasoning": true,
        "technicalDepth": "detailed",
        "includeExamples": true
    }'::jsonb,
    'technical',
    true,
    '00000000-0000-0000-0000-000000000000',
    ARRAY['technical', 'analysis', 'engineering']
),
(
    'Balanced Perspective',
    'Multi-agent evaluation for controversial topics',
    'MULTI_AGENT_RED_TEAM',
    '{
        "agents": {
            "advocate": {
                "prompt": "Present the strongest case in favor:",
                "weight": 0.33
            },
            "critic": {
                "prompt": "Present the strongest counter-arguments:",
                "weight": 0.33
            },
            "mediator": {
                "prompt": "Find common ground and synthesize:",
                "weight": 0.34
            }
        }
    }'::jsonb,
    'balanced',
    true,
    '00000000-0000-0000-0000-000000000000',
    ARRAY['balanced', 'multi-perspective', 'controversial']
),
(
    'High-Stakes Decision',
    'Careful iteration for important decisions',
    'SELF_CRITIQUE_LOOP',
    '{
        "maxIterations": 3,
        "critiquePrompt": "Identify potential flaws or oversights in this response:",
        "improvementThreshold": 0.25,
        "focusAreas": ["accuracy", "completeness", "clarity", "implications"]
    }'::jsonb,
    'decision-making',
    true,
    '00000000-0000-0000-0000-000000000000',
    ARRAY['decision', 'critical', 'improvement']
);

-- Add comments
COMMENT ON TABLE flow_templates IS 'Reusable agentic flow configuration templates';
COMMENT ON COLUMN flow_templates.is_public IS 'Whether template is available to all organizations';
COMMENT ON COLUMN flow_templates.rating IS 'Average user rating (1-5 scale)';
COMMENT ON COLUMN flow_templates.usage_count IS 'Number of times template has been used';