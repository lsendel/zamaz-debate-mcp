-- Test database initialization script
-- This script sets up the test database with necessary extensions and initial data

-- Enable UUID extension for generating unique IDs
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create test indexes for performance
CREATE INDEX IF NOT EXISTS idx_github_installation_account_login ON github_installations(account_login);
CREATE INDEX IF NOT EXISTS idx_github_installation_status ON github_installations(status);
CREATE INDEX IF NOT EXISTS idx_repository_config_installation_id ON repository_configs(installation_id);
CREATE INDEX IF NOT EXISTS idx_repository_config_repo_name ON repository_configs(repository_full_name);
CREATE INDEX IF NOT EXISTS idx_pr_review_installation_id ON pull_request_reviews(installation_id);
CREATE INDEX IF NOT EXISTS idx_pr_review_repo_name ON pull_request_reviews(repository_full_name);
CREATE INDEX IF NOT EXISTS idx_pr_review_status ON pull_request_reviews(status);
CREATE INDEX IF NOT EXISTS idx_review_comment_review_id ON review_comments(review_id);
CREATE INDEX IF NOT EXISTS idx_review_issue_review_id ON review_issues(review_id);
CREATE INDEX IF NOT EXISTS idx_review_issue_severity ON review_issues(severity);

-- Insert test data for E2E tests
INSERT INTO github_installations (id, account_login, account_type, status, access_token, created_at, updated_at) VALUES
(99999, 'test-system-user', 'User', 'ACTIVE', 'test-system-token', NOW(), NOW());

INSERT INTO repository_configs (installation_id, repository_full_name, auto_review_enabled, notifications_enabled, branch_patterns, created_at, updated_at) VALUES
(99999, 'test-system/test-repo', true, true, 'main,develop', NOW(), NOW());

-- Create test sequences for generating test data
CREATE SEQUENCE IF NOT EXISTS test_installation_id_seq START 100000;
CREATE SEQUENCE IF NOT EXISTS test_review_id_seq START 100000;

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO test_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO test_user;