-- Constants
-- VARCHAR_DEFAULT: VARCHAR(255)
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()

-- Create databases for RAG and Template services
CREATE DATABASE rag_db;
CREATE DATABASE template_db;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE rag_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE template_db TO postgres;

-- Connect to rag_db and create schema
\c rag_db;

CREATE SCHEMA IF NOT EXISTS rag;

-- Basic tables for RAG service
CREATE TABLE IF NOT EXISTS rag.documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() /* Use UUID_DEFAULT */,
    organization_id UUID NOT NULL,
    name VARCHAR(255) /* Use VARCHAR_DEFAULT */ NOT NULL,
    content TEXT,
    metadata JSONB,
    embedding vector(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rag.collections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() /* Use UUID_DEFAULT */,
    organization_id UUID NOT NULL,
    name VARCHAR(255) /* Use VARCHAR_DEFAULT */ NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Connect to template_db and create schema
\c template_db;

CREATE SCHEMA IF NOT EXISTS template;

-- Basic tables for Template service
CREATE TABLE IF NOT EXISTS template.templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() /* Use UUID_DEFAULT */,
    organization_id UUID NOT NULL,
    name VARCHAR(255) /* Use VARCHAR_DEFAULT */ NOT NULL,
    description TEXT,
    category VARCHAR(100),
    type VARCHAR(50),
    content TEXT,
    variables JSONB,
    metadata JSONB,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS template.template_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() /* Use UUID_DEFAULT */,
    template_id UUID NOT NULL REFERENCES template.templates(id),
    version INTEGER NOT NULL,
    content TEXT,
    variables JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) /* Use VARCHAR_DEFAULT */
);