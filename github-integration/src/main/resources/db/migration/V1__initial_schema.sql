-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Initial schema for GitHub Integration

-- Repository configuration table
CREATE TABLE repository_config (
    id SERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    repository_name VARCHAR2(255) NOT NULL,
    owner_name VARCHAR2(255) NOT NULL,
    review_depth VARCHAR2(50) NOT NULL DEFAULT 'standard',
    auto_fix_enabled BOOLEAN NOT NULL DEFAULT false,
    comment_style VARCHAR2(50) NOT NULL DEFAULT 'educational',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_repository_config_repo_id UNIQUE (repository_id)
);

-- GitHub installation table
CREATE TABLE github_installation (
    id SERIAL PRIMARY KEY,
    installation_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    account_name VARCHAR2(255) NOT NULL,
    account_type VARCHAR2(50) NOT NULL,
    access_token TEXT,
    token_expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_github_installation_installation_id UNIQUE (installation_id)
);

-- Pull request review table
CREATE TABLE pull_request_review (
    id SERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    pr_number INT NOT NULL,
    pr_title VARCHAR2(255) NOT NULL,
    pr_author VARCHAR2(255) NOT NULL,
    base_branch VARCHAR2(255) NOT NULL,
    head_branch VARCHAR2(255) NOT NULL,
    status VARCHAR2(50) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    files_reviewed INT,
    lines_reviewed INT,
    critical_issues INT DEFAULT 0,
    major_issues INT DEFAULT 0,
    minor_issues INT DEFAULT 0,
    suggestions INT DEFAULT 0,
    auto_fixable INT DEFAULT 0,
    CONSTRAINT uk_pull_request_review_repo_pr UNIQUE (repository_id, pr_number)
);

-- Review issue table
CREATE TABLE review_issue (
    id SERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    file_path VARCHAR2(1024) NOT NULL,
    line_start INT NOT NULL,
    line_end INT NOT NULL,
    issue_type VARCHAR2(100) NOT NULL,
    severity VARCHAR2(50) NOT NULL,
    description TEXT NOT NULL,
    suggestion TEXT,
    auto_fixable BOOLEAN NOT NULL DEFAULT false,
    fix_description TEXT,
    comment_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_issue_review FOREIGN KEY (review_id) REFERENCES pull_request_review(id) ON DELETE CASCADE
);

-- Review comment table
CREATE TABLE review_comment (
    id SERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    github_comment_id BIGINT,
    file_path VARCHAR2(1024) NOT NULL,
    line INT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_comment_review FOREIGN KEY (review_id) REFERENCES pull_request_review(id) ON DELETE CASCADE
);

-- Feedback table
CREATE TABLE feedback (
    id SERIAL PRIMARY KEY,
    review_issue_id BIGINT NOT NULL,
    feedback_type VARCHAR2(50) NOT NULL,
    content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_review_issue FOREIGN KEY (review_issue_id) REFERENCES review_issue(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_repository_config_repo_name ON repository_config(repository_name, owner_name);
CREATE INDEX idx_pull_request_review_status ON pull_request_review(status);
CREATE INDEX idx_pull_request_review_repo_id ON pull_request_review(repository_id);
CREATE INDEX idx_review_issue_review_id ON review_issue(review_id);
CREATE INDEX idx_review_issue_severity ON review_issue(severity);
CREATE INDEX idx_review_comment_review_id ON review_comment(review_id);
CREATE INDEX idx_feedback_review_issue_id ON feedback(review_issue_id);