# Database Naming Conventions

## Overview

This document establishes consistent naming conventions for database objects in the Zamaz Debate MCP Services system. All database objects should follow snake_case naming conventions for consistency and readability.

## General Rules

### 1. Case Convention
- **Use snake_case** for all database object names
- All names should be **lowercase**
- Use **underscores** to separate words
- Avoid camelCase, PascalCase, or mixed case

### 2. Character Rules
- Use only **alphanumeric characters** and **underscores**
- Start with a **letter** (not a number or underscore)
- Avoid special characters, spaces, or hyphens
- Keep names **descriptive but concise**

### 3. Pluralization
- **Table names**: Use **plural** nouns (e.g., `organizations`, `users`)
- **Column names**: Use **singular** nouns (e.g., `user_id`, `organization_name`)

## Table Naming Conventions

### Standard Tables
```sql
-- Good Examples
organizations
users
debates
debate_participants
organization_users
refresh_tokens
llm_providers
debate_messages
user_preferences

-- Bad Examples
Organization          -- PascalCase
organizationUsers     -- camelCase
debate-participants   -- hyphens
user preferences      -- spaces
```

### Junction/Mapping Tables
For many-to-many relationships, combine the two entity names:
```sql
-- Pattern: {entity1}_{entity2} (alphabetical order preferred)
organization_users    -- maps organizations to users
debate_participants   -- maps debates to users/participants
user_roles           -- maps users to roles
team_members         -- maps teams to users
```

### Audit/History Tables
```sql
-- Pattern: {table_name}_audit or {table_name}_history
organizations_audit
users_history
debates_audit
```

## Column Naming Conventions

### Primary Keys
```sql
-- Always use 'id' for primary key
id UUID PRIMARY KEY DEFAULT uuid_generate_v4()

-- For composite keys, be descriptive
organization_id UUID
user_id UUID
PRIMARY KEY (organization_id, user_id)
```

### Foreign Keys
```sql
-- Pattern: {referenced_table_singular}_id
organization_id UUID REFERENCES organizations(id)
user_id UUID REFERENCES users(id)
debate_id UUID REFERENCES debates(id)
parent_organization_id UUID REFERENCES organizations(id)
```

### Standard Columns

#### Audit Columns
```sql
-- Timestamp columns
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
deleted_at TIMESTAMP  -- for soft deletes

-- User tracking columns
created_by UUID REFERENCES users(id)
updated_by UUID REFERENCES users(id)
deleted_by UUID REFERENCES users(id)
```

#### Status/State Columns
```sql
-- Boolean flags
is_active BOOLEAN DEFAULT true
is_deleted BOOLEAN DEFAULT false
is_public BOOLEAN DEFAULT false
is_verified BOOLEAN DEFAULT false

-- Status enums
status VARCHAR(50) NOT NULL DEFAULT 'active'
debate_status VARCHAR(50) NOT NULL DEFAULT 'pending'
user_status VARCHAR(50) NOT NULL DEFAULT 'active'
```

#### Common Data Columns
```sql
-- Text fields
name VARCHAR(255) NOT NULL
description TEXT
email VARCHAR(255) UNIQUE NOT NULL
first_name VARCHAR(100)
last_name VARCHAR(100)

-- JSON/Configuration
settings JSONB DEFAULT '{}'
metadata JSONB
configuration JSONB

-- Numeric fields
sort_order INTEGER DEFAULT 0
max_participants INTEGER
round_duration_minutes INTEGER
```

### Column Naming Examples

#### Good Examples
```sql
-- Descriptive and clear
organization_name VARCHAR(255)
user_email VARCHAR(255)
debate_topic TEXT
participant_role VARCHAR(50)
round_start_time TIMESTAMP
max_round_duration INTEGER
is_debate_active BOOLEAN
created_at TIMESTAMP
updated_by UUID

-- Relationship columns
parent_organization_id UUID
assigned_user_id UUID
debate_creator_id UUID
```

#### Bad Examples
```sql
-- Avoid these patterns
orgName VARCHAR(255)        -- camelCase
UserEmail VARCHAR(255)      -- PascalCase
debate-topic TEXT           -- hyphens
participantRole VARCHAR(50) -- camelCase
roundStartTime TIMESTAMP    -- camelCase
maxRoundDuration INTEGER    -- camelCase
isDebateActive BOOLEAN      -- camelCase
createdAt TIMESTAMP         -- camelCase
updatedBy UUID              -- camelCase
```

## Index Naming Conventions

### Primary Key Indexes
```sql
-- Automatically named by database, but if explicit:
pk_organizations
pk_users
pk_debates
```

### Regular Indexes
```sql
-- Pattern: idx_{table}_{column(s)}
idx_organizations_name
idx_users_email
idx_debates_status
idx_organization_users_user_id
idx_debates_created_at
idx_users_email_status  -- composite index
```

### Unique Indexes
```sql
-- Pattern: uk_{table}_{column(s)} or uniq_{table}_{column(s)}
uk_users_email
uk_organizations_name
uniq_refresh_tokens_token
```

### Foreign Key Indexes
```sql
-- Pattern: fk_{table}_{referenced_table}
fk_organization_users_organization
fk_organization_users_user
fk_debates_organization
fk_debate_participants_debate
fk_debate_participants_user
```

## Constraint Naming Conventions

### Foreign Key Constraints
```sql
-- Pattern: fk_{table}_{column}_{referenced_table}
CONSTRAINT fk_organization_users_organization_id_organizations 
    FOREIGN KEY (organization_id) REFERENCES organizations(id)

CONSTRAINT fk_organization_users_user_id_users 
    FOREIGN KEY (user_id) REFERENCES users(id)
```

### Check Constraints
```sql
-- Pattern: chk_{table}_{column}_{condition}
CONSTRAINT chk_users_email_format 
    CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')

CONSTRAINT chk_debates_max_participants_positive 
    CHECK (max_participants > 0)

CONSTRAINT chk_organization_users_role_valid 
    CHECK (role IN ('owner', 'admin', 'member', 'viewer'))
```

### Unique Constraints
```sql
-- Pattern: uk_{table}_{column(s)}
CONSTRAINT uk_users_email UNIQUE (email)
CONSTRAINT uk_organizations_name UNIQUE (name)
CONSTRAINT uk_organization_users_org_user UNIQUE (organization_id, user_id)
```

## View Naming Conventions

```sql
-- Pattern: v_{descriptive_name} or {entity}_view
v_active_organizations
v_user_organization_summary
v_debate_statistics
organization_summary_view
user_permissions_view
```

## Function and Procedure Naming Conventions

```sql
-- Pattern: {action}_{entity} or {purpose}_function
update_updated_at_column()
calculate_debate_score()
validate_user_permissions()
cleanup_expired_tokens()
generate_organization_report()
```

## Trigger Naming Conventions

```sql
-- Pattern: {action}_{table}_{column/purpose}
update_organizations_updated_at
validate_user_email_before_insert
audit_debates_after_update
cleanup_refresh_tokens_after_delete
```

## Migration File Naming

### Flyway Migration Files
```sql
-- Pattern: V{version}__{description}.sql
V1__Initial_schema.sql
V2__Add_missing_critical_indexes.sql
V3__Add_applications_teams_preferences.sql
V4__Create_debate_tables.sql
V5__Add_user_preferences.sql
V6__Update_organization_settings.sql
```

## Schema Organization

### Schema Naming
```sql
-- Environment-based schemas
mcp_dev
mcp_test
mcp_staging
mcp_prod

-- Service-based schemas (if using schema per service)
mcp_organization
mcp_debate
mcp_llm
mcp_auth
```

## Data Type Conventions

### Standard Data Types
```sql
-- Primary Keys
id UUID PRIMARY KEY DEFAULT uuid_generate_v4()

-- Strings
name VARCHAR(255)           -- Standard text fields
description TEXT            -- Long text content
email VARCHAR(255)          -- Email addresses
slug VARCHAR(100)           -- URL-friendly identifiers

-- Numbers
sort_order INTEGER DEFAULT 0
max_participants INTEGER
price DECIMAL(10,2)         -- Currency amounts
percentage DECIMAL(5,2)     -- Percentages

-- Dates and Times
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
expires_at TIMESTAMP
date_of_birth DATE

-- JSON
settings JSONB DEFAULT '{}'
metadata JSONB
configuration JSONB

-- Booleans
is_active BOOLEAN DEFAULT true
is_deleted BOOLEAN DEFAULT false
```

## Examples by Service

### Organization Service Tables
```sql
-- Main entities
organizations
users
organization_users

-- Supporting tables
user_preferences
organization_settings
refresh_tokens
audit_logs
```

### Debate Service Tables
```sql
-- Main entities
debates
debate_participants
debate_messages
debate_rounds

-- Supporting tables
debate_templates
llm_providers
debate_statistics
message_attachments
```

### Common Patterns Across Services
```sql
-- Every service should have consistent patterns
{entity}_id UUID           -- Foreign keys
created_at TIMESTAMP       -- Audit timestamps
updated_at TIMESTAMP       -- Audit timestamps
is_active BOOLEAN          -- Soft delete flags
settings JSONB             -- Configuration storage
```

## Validation and Enforcement

### Database-Level Validation
```sql
-- Use check constraints for data validation
CONSTRAINT chk_users_email_not_empty CHECK (LENGTH(TRIM(email)) > 0)
CONSTRAINT chk_organizations_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
CONSTRAINT chk_debates_max_participants_range CHECK (max_participants BETWEEN 2 AND 50)
```

### Application-Level Validation
- JPA entity annotations should match database naming
- Use `@Column(name = "snake_case_name")` for mapping
- Ensure consistency between entity field names and database columns

## Migration Best Practices

### Column Additions
```sql
-- Always provide defaults for new NOT NULL columns
ALTER TABLE organizations 
ADD COLUMN organization_type VARCHAR(50) NOT NULL DEFAULT 'standard';

-- Add indexes for new columns that will be queried
CREATE INDEX idx_organizations_type ON organizations(organization_type);
```

### Column Renames
```sql
-- Use descriptive migration names
-- V15__Rename_org_name_to_organization_name.sql
ALTER TABLE organizations 
RENAME COLUMN org_name TO organization_name;
```

### Table Renames
```sql
-- Update all related objects (indexes, constraints, etc.)
ALTER TABLE orgs RENAME TO organizations;
ALTER INDEX idx_orgs_name RENAME TO idx_organizations_name;
```

This naming convention ensures consistency across all database objects and makes the schema self-documenting and maintainable.