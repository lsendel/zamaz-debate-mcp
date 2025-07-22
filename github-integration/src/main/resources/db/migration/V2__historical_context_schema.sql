-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Historical Context Awareness System Schema
-- This migration adds comprehensive historical tracking and learning capabilities

-- Developer profile and learning progress tracking
CREATE TABLE developer_profile (
    id SERIAL PRIMARY KEY,
    github_username VARCHAR2(255) NOT NULL,
    github_user_id BIGINT NOT NULL,
    email VARCHAR2(255),
    display_name VARCHAR2(255),
    experience_level VARCHAR2(50) NOT NULL DEFAULT 'intermediate', -- beginner, intermediate, advanced, expert
    primary_languages TEXT[], -- Array of programming languages
    domain_expertise TEXT[], -- Array of domain areas
    communication_style VARCHAR2(50) NOT NULL DEFAULT 'standard', -- concise, detailed, educational
    learning_preferences JSONB, -- JSON object with learning preferences
    timezone VARCHAR2(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_developer_profile_github_user_id UNIQUE (github_user_id),
    CONSTRAINT uk_developer_profile_github_username UNIQUE (github_username)
);

-- Historical PR metrics and patterns
CREATE TABLE pr_historical_metrics (
    id SERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    pr_number INT NOT NULL,
    pr_author_id BIGINT NOT NULL,
    pr_size VARCHAR2(50) NOT NULL, -- xs, small, medium, large, xl
    complexity_score DECIMAL(5,2), -- 0-100 complexity score
    test_coverage_change DECIMAL(5,2), -- Change in test coverage percentage
    code_quality_score DECIMAL(5,2), -- 0-100 quality score
    review_turnaround_hours INT, -- Time from PR creation to first review
    merge_time_hours INT, -- Time from PR creation to merge
    comment_count INT DEFAULT 0,
    approval_count INT DEFAULT 0,
    change_request_count INT DEFAULT 0,
    files_changed INT DEFAULT 0,
    lines_added INT DEFAULT 0,
    lines_deleted INT DEFAULT 0,
    commit_count INT DEFAULT 0,
    is_hotfix BOOLEAN DEFAULT FALSE,
    is_feature BOOLEAN DEFAULT FALSE,
    is_refactor BOOLEAN DEFAULT FALSE,
    is_bugfix BOOLEAN DEFAULT FALSE,
    merge_conflicts BOOLEAN DEFAULT FALSE,
    ci_failures INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pr_historical_metrics_author FOREIGN KEY (pr_author_id) REFERENCES developer_profile(github_user_id),
    CONSTRAINT uk_pr_historical_metrics_repo_pr UNIQUE (repository_id, pr_number)
);

-- Developer skill assessment and learning progress
CREATE TABLE developer_skill_assessment (
    id SERIAL PRIMARY KEY,
    developer_id BIGINT NOT NULL,
    skill_category VARCHAR2(100) NOT NULL, -- e.g., 'java', 'testing', 'security', 'performance'
    skill_level VARCHAR2(50) NOT NULL, -- novice, competent, proficient, expert
    confidence_score DECIMAL(5,2), -- 0-100 confidence in this skill
    evidence_count INT DEFAULT 0, -- Number of PRs/reviews that contributed to this assessment
    last_demonstration_date TIMESTAMP, -- Last time skill was demonstrated
    improvement_trend VARCHAR2(50), -- improving, stable, declining
    learning_goals TEXT[], -- Array of specific learning goals
    recommended_resources TEXT[], -- Array of recommended learning resources
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_developer_skill_assessment_developer FOREIGN KEY (developer_id) REFERENCES developer_profile(id),
    CONSTRAINT uk_developer_skill_assessment_dev_skill UNIQUE (developer_id, skill_category)
);

-- Team knowledge base built from reviews and feedback
CREATE TABLE knowledge_base_entry (
    id SERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    category VARCHAR2(100) NOT NULL, -- e.g., 'best_practice', 'anti_pattern', 'common_issue'
    title VARCHAR2(255) NOT NULL,
    description TEXT NOT NULL,
    content TEXT NOT NULL,
    tags TEXT[], -- Array of tags for categorization
    severity VARCHAR2(50), -- critical, high, medium, low
    frequency_count INT DEFAULT 1, -- How often this pattern appears
    effectiveness_score DECIMAL(5,2), -- 0-100 score of how effective this knowledge is
    source_review_ids BIGINT[], -- Array of review IDs that contributed to this entry
    created_by_user_id BIGINT,
    language VARCHAR2(50), -- Programming language if applicable
    framework VARCHAR2(100), -- Framework if applicable
    is_approved BOOLEAN DEFAULT FALSE,
    approval_date TIMESTAMP,
    approved_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_knowledge_base_entry_created_by FOREIGN KEY (created_by_user_id) REFERENCES developer_profile(github_user_id),
    CONSTRAINT fk_knowledge_base_entry_approved_by FOREIGN KEY (approved_by_user_id) REFERENCES developer_profile(github_user_id)
);

-- Historical trend analysis for code quality
CREATE TABLE code_quality_trends (
    id SERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    analysis_date DATE NOT NULL,
    period_type VARCHAR2(50) NOT NULL, -- daily, weekly, monthly
    metric_name VARCHAR2(100) NOT NULL, -- e.g., 'bug_rate', 'test_coverage', 'complexity'
    metric_value DECIMAL(10,4) NOT NULL,
    trend_direction VARCHAR2(50), -- improving, stable, declining
    change_percentage DECIMAL(5,2), -- Percentage change from previous period
    developer_count INT, -- Number of active developers in this period
    pr_count INT, -- Number of PRs in this period
    issue_count INT, -- Number of issues found in this period
    baseline_value DECIMAL(10,4), -- Baseline value for comparison
    target_value DECIMAL(10,4), -- Target value for this metric
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_code_quality_trends_repo_date_metric UNIQUE (repository_id, analysis_date, metric_name, period_type)
);

-- Review feedback learning system
CREATE TABLE review_feedback_learning (
    id SERIAL PRIMARY KEY,
    original_review_id BIGINT NOT NULL,
    feedback_type VARCHAR2(50) NOT NULL, -- positive, negative, neutral, suggestion
    feedback_category VARCHAR2(100) NOT NULL, -- accuracy, helpfulness, clarity, relevance
    feedback_score INT NOT NULL CHECK (feedback_score >= 1 AND feedback_score <= 5),
    feedback_text TEXT,
    reviewer_id BIGINT NOT NULL, -- Person who gave the feedback
    reviewed_by_id BIGINT NOT NULL, -- Person whose review was being evaluated
    is_actionable BOOLEAN DEFAULT FALSE,
    action_taken TEXT, -- Description of action taken based on feedback
    improvement_suggestions TEXT[], -- Array of improvement suggestions
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_feedback_learning_original_review FOREIGN KEY (original_review_id) REFERENCES pull_request_review(id),
    CONSTRAINT fk_review_feedback_learning_reviewer FOREIGN KEY (reviewer_id) REFERENCES developer_profile(github_user_id),
    CONSTRAINT fk_review_feedback_learning_reviewed_by FOREIGN KEY (reviewed_by_id) REFERENCES developer_profile(github_user_id)
);

-- Personalized suggestion patterns
CREATE TABLE personalized_suggestions (
    id SERIAL PRIMARY KEY,
    developer_id BIGINT NOT NULL,
    suggestion_type VARCHAR2(100) NOT NULL, -- code_pattern, best_practice, learning_resource
    suggestion_title VARCHAR2(255) NOT NULL,
    suggestion_content TEXT NOT NULL,
    context_data JSONB, -- JSON object with context information
    confidence_score DECIMAL(5,2), -- 0-100 confidence in this suggestion
    priority_level VARCHAR2(50), -- high, medium, low
    trigger_conditions TEXT[], -- Array of conditions that triggered this suggestion
    success_metrics JSONB, -- JSON object defining success metrics
    is_accepted BOOLEAN,
    acceptance_date TIMESTAMP,
    effectiveness_rating INT CHECK (effectiveness_rating >= 1 AND effectiveness_rating <= 5),
    feedback_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_personalized_suggestions_developer FOREIGN KEY (developer_id) REFERENCES developer_profile(id)
);

-- Machine learning training data and patterns
CREATE TABLE ml_training_data (
    id SERIAL PRIMARY KEY,
    data_type VARCHAR2(100) NOT NULL, -- pr_pattern, code_smell, review_quality
    input_features JSONB NOT NULL, -- JSON object with input features
    output_label VARCHAR2(255) NOT NULL, -- The label/classification
    confidence_score DECIMAL(5,2), -- 0-100 confidence in this label
    source_type VARCHAR2(50) NOT NULL, -- manual, automatic, hybrid
    source_id BIGINT, -- ID of source entity (PR, review, etc.)
    validation_status VARCHAR2(50), -- pending, validated, rejected
    validated_by_user_id BIGINT,
    validation_date TIMESTAMP,
    repository_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ml_training_data_validated_by FOREIGN KEY (validated_by_user_id) REFERENCES developer_profile(github_user_id)
);

-- Historical pattern recognition results
CREATE TABLE pattern_recognition_results (
    id SERIAL PRIMARY KEY,
    pattern_type VARCHAR2(100) NOT NULL, -- code_duplication, naming_convention, architecture_violation
    pattern_name VARCHAR2(255) NOT NULL,
    pattern_description TEXT NOT NULL,
    detection_algorithm VARCHAR2(100) NOT NULL, -- rule_based, ml_based, hybrid
    confidence_score DECIMAL(5,2), -- 0-100 confidence in pattern detection
    occurrence_count INT DEFAULT 1,
    first_detected_date TIMESTAMP NOT NULL,
    last_detected_date TIMESTAMP NOT NULL,
    severity_level VARCHAR2(50), -- critical, high, medium, low
    impact_assessment TEXT,
    resolution_suggestions TEXT[],
    repository_ids BIGINT[], -- Array of repository IDs where pattern was found
    affected_files TEXT[], -- Array of file paths affected by this pattern
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Knowledge transfer tracking
CREATE TABLE knowledge_transfer_sessions (
    id SERIAL PRIMARY KEY,
    session_type VARCHAR2(100) NOT NULL, -- mentoring, code_review, pair_programming
    mentor_id BIGINT NOT NULL,
    mentee_id BIGINT NOT NULL,
    repository_id BIGINT,
    pr_number INT,
    session_topics TEXT[], -- Array of topics covered
    duration_minutes INT,
    effectiveness_rating INT CHECK (effectiveness_rating >= 1 AND effectiveness_rating <= 5),
    knowledge_areas TEXT[], -- Array of knowledge areas transferred
    follow_up_required BOOLEAN DEFAULT FALSE,
    follow_up_notes TEXT,
    session_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_knowledge_transfer_sessions_mentor FOREIGN KEY (mentor_id) REFERENCES developer_profile(github_user_id),
    CONSTRAINT fk_knowledge_transfer_sessions_mentee FOREIGN KEY (mentee_id) REFERENCES developer_profile(github_user_id)
);

-- Best practice extraction and evolution
CREATE TABLE best_practices (
    id SERIAL PRIMARY KEY,
    practice_category VARCHAR2(100) NOT NULL, -- coding, testing, documentation, architecture
    practice_title VARCHAR2(255) NOT NULL,
    practice_description TEXT NOT NULL,
    practice_details TEXT NOT NULL,
    applicable_languages TEXT[], -- Array of programming languages
    applicable_frameworks TEXT[], -- Array of frameworks
    confidence_level VARCHAR2(50), -- low, medium, high
    adoption_rate DECIMAL(5,2), -- 0-100 percentage of team adoption
    evidence_count INT DEFAULT 0, -- Number of examples supporting this practice
    success_stories TEXT[], -- Array of success story descriptions
    common_pitfalls TEXT[], -- Array of common pitfalls to avoid
    related_practices BIGINT[], -- Array of related practice IDs
    source_type VARCHAR2(50), -- extracted, manual, imported
    extraction_algorithm VARCHAR2(100), -- If extracted automatically
    is_deprecated BOOLEAN DEFAULT FALSE,
    deprecation_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create comprehensive indexes for performance
CREATE INDEX idx_developer_profile_github_user_id ON developer_profile(github_user_id);
CREATE INDEX idx_developer_profile_experience_level ON developer_profile(experience_level);
CREATE INDEX idx_developer_profile_primary_languages ON developer_profile USING GIN(primary_languages);

CREATE INDEX idx_pr_historical_metrics_repository_id ON pr_historical_metrics(repository_id);
CREATE INDEX idx_pr_historical_metrics_pr_author_id ON pr_historical_metrics(pr_author_id);
CREATE INDEX idx_pr_historical_metrics_created_at ON pr_historical_metrics(created_at);
CREATE INDEX idx_pr_historical_metrics_complexity_score ON pr_historical_metrics(complexity_score);
CREATE INDEX idx_pr_historical_metrics_code_quality_score ON pr_historical_metrics(code_quality_score);

CREATE INDEX idx_developer_skill_assessment_developer_id ON developer_skill_assessment(developer_id);
CREATE INDEX idx_developer_skill_assessment_skill_category ON developer_skill_assessment(skill_category);
CREATE INDEX idx_developer_skill_assessment_skill_level ON developer_skill_assessment(skill_level);

CREATE INDEX idx_knowledge_base_entry_repository_id ON knowledge_base_entry(repository_id);
CREATE INDEX idx_knowledge_base_entry_category ON knowledge_base_entry(category);
CREATE INDEX idx_knowledge_base_entry_tags ON knowledge_base_entry USING GIN(tags);
CREATE INDEX idx_knowledge_base_entry_is_approved ON knowledge_base_entry(is_approved);

CREATE INDEX idx_code_quality_trends_repository_id ON code_quality_trends(repository_id);
CREATE INDEX idx_code_quality_trends_analysis_date ON code_quality_trends(analysis_date);
CREATE INDEX idx_code_quality_trends_metric_name ON code_quality_trends(metric_name);
CREATE INDEX idx_code_quality_trends_period_type ON code_quality_trends(period_type);

CREATE INDEX idx_review_feedback_learning_original_review_id ON review_feedback_learning(original_review_id);
CREATE INDEX idx_review_feedback_learning_reviewer_id ON review_feedback_learning(reviewer_id);
CREATE INDEX idx_review_feedback_learning_reviewed_by_id ON review_feedback_learning(reviewed_by_id);
CREATE INDEX idx_review_feedback_learning_feedback_type ON review_feedback_learning(feedback_type);

CREATE INDEX idx_personalized_suggestions_developer_id ON personalized_suggestions(developer_id);
CREATE INDEX idx_personalized_suggestions_suggestion_type ON personalized_suggestions(suggestion_type);
CREATE INDEX idx_personalized_suggestions_priority_level ON personalized_suggestions(priority_level);
CREATE INDEX idx_personalized_suggestions_is_accepted ON personalized_suggestions(is_accepted);

CREATE INDEX idx_ml_training_data_data_type ON ml_training_data(data_type);
CREATE INDEX idx_ml_training_data_source_type ON ml_training_data(source_type);
CREATE INDEX idx_ml_training_data_validation_status ON ml_training_data(validation_status);
CREATE INDEX idx_ml_training_data_repository_id ON ml_training_data(repository_id);

CREATE INDEX idx_pattern_recognition_results_pattern_type ON pattern_recognition_results(pattern_type);
CREATE INDEX idx_pattern_recognition_results_severity_level ON pattern_recognition_results(severity_level);
CREATE INDEX idx_pattern_recognition_results_confidence_score ON pattern_recognition_results(confidence_score);
CREATE INDEX idx_pattern_recognition_results_repository_ids ON pattern_recognition_results USING GIN(repository_ids);

CREATE INDEX idx_knowledge_transfer_sessions_mentor_id ON knowledge_transfer_sessions(mentor_id);
CREATE INDEX idx_knowledge_transfer_sessions_mentee_id ON knowledge_transfer_sessions(mentee_id);
CREATE INDEX idx_knowledge_transfer_sessions_repository_id ON knowledge_transfer_sessions(repository_id);
CREATE INDEX idx_knowledge_transfer_sessions_session_type ON knowledge_transfer_sessions(session_type);

CREATE INDEX idx_best_practices_practice_category ON best_practices(practice_category);
CREATE INDEX idx_best_practices_applicable_languages ON best_practices USING GIN(applicable_languages);
CREATE INDEX idx_best_practices_confidence_level ON best_practices(confidence_level);
CREATE INDEX idx_best_practices_is_deprecated ON best_practices(is_deprecated);