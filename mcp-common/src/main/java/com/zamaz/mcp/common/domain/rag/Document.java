package com.zamaz.mcp.common.domain.rag;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Domain entity representing a document in the RAG system.
 */
public class Document {
    private final String id;
    private final String title;
    private final String content;
    private final String source;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
    private final float relevanceScore;

    private Document(Builder builder) {
        this.id = builder.id;
        this.title = Objects.requireNonNull(builder.title, "Title cannot be null");
        this.content = Objects.requireNonNull(builder.content, "Content cannot be null");
        this.source = Objects.requireNonNull(builder.source, "Source cannot be null");
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.metadata = new HashMap<>(builder.metadata);
        this.relevanceScore = builder.relevanceScore;
    }

    /**
     * Creates a new builder for Document.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSource() {
        return source;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public float getRelevanceScore() {
        return relevanceScore;
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Creates a copy of this document with a new relevance score.
     *
     * @param score The new relevance score
     * @return A new document instance with the updated score
     */
    public Document withRelevanceScore(float score) {
        return builder()
                .id(this.id)
                .title(this.title)
                .content(this.content)
                .source(this.source)
                .timestamp(this.timestamp)
                .metadata(this.metadata)
                .relevanceScore(score)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Float.compare(document.relevanceScore, relevanceScore) == 0 &&
                Objects.equals(id, document.id) &&
                title.equals(document.title) &&
                content.equals(document.content) &&
                source.equals(document.source) &&
                timestamp.equals(document.timestamp) &&
                metadata.equals(document.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, content, source, timestamp, metadata, relevanceScore);
    }

    @Override
    public String toString() {
        return "Document{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                ", relevanceScore=" + relevanceScore +
                '}';
    }

    /**
     * Builder for creating Document instances.
     */
    public static class Builder {
        private String id;
        private String title;
        private String content;
        private String source;
        private Instant timestamp;
        private Map<String, Object> metadata = new HashMap<>();
        private float relevanceScore = 0.0f;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder relevanceScore(float relevanceScore) {
            this.relevanceScore = relevanceScore;
            return this;
        }

        public Document build() {
            return new Document(this);
        }
    }
}