-- Create documents table
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    title VARCHAR2(255) NOT NULL,
    status VARCHAR2(50) NOT NULL,
    content TEXT,
    content_type VARCHAR2(100),
    file_name VARCHAR2(255),
    file_type VARCHAR2(100),
    file_size BIGINT,
    metadata JSONB,
    chunk_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for documents
CREATE INDEX idx_document_org ON documents(organization_id);
CREATE INDEX idx_document_status ON documents(status);
CREATE INDEX idx_document_created ON documents(created_at);
CREATE INDEX idx_document_title ON documents USING gin(to_tsvector('english', title));

-- Create document_chunks table
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    start_offset INTEGER,
    end_offset INTEGER,
    embedding vector(1536), -- Requires pgvector extension
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for chunks
CREATE INDEX idx_chunk_document ON document_chunks(document_id);
CREATE INDEX idx_chunk_number ON document_chunks(chunk_number);
CREATE INDEX idx_chunk_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops); -- Requires pgvector

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_document_chunks_updated_at BEFORE UPDATE ON document_chunks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();