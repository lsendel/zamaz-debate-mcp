package com.zamaz.mcp.rag.domain.model.document;

import com.zamaz.mcp.rag.domain.model.FileInfo;
import java.time.Instant;
import java.util.*;

/**
 * Value Object representing document metadata.
 * Immutable collection of document properties.
 */
public class DocumentMetadata {
    
    private final Map<String, String> properties;
    private final Set<String> tags;
    private final String source;
    private final Instant uploadedAt;
    
    private DocumentMetadata(Builder builder) {
        this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
        this.tags = Collections.unmodifiableSet(new HashSet<>(builder.tags));
        this.source = builder.source;
        this.uploadedAt = builder.uploadedAt != null ? builder.uploadedAt : Instant.now();
    }
    
    /**
     * Get property value by key
     */
    public Optional<String> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }
    
    /**
     * Get all properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }
    
    /**
     * Check if has property
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
    
    /**
     * Get all tags
     */
    public Set<String> getTags() {
        return tags;
    }
    
    /**
     * Check if has tag
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
    
    /**
     * Get document source
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Get upload timestamp
     */
    public Instant getUploadedAt() {
        return uploadedAt;
    }
    
    /**
     * Get file info from metadata properties
     */
    public FileInfo getFileInfo() {
        String fileName = getProperty("fileName").orElse("unknown");
        String fileType = getProperty("fileType").orElse("application/octet-stream");
        long fileSize = getProperty("fileSize")
                .map(Long::parseLong)
                .orElse(0L);
        
        return FileInfo.of(fileName, fileType, fileSize);
    }
    
    /**
     * Create a new metadata with additional property
     */
    public DocumentMetadata withProperty(String key, String value) {
        return toBuilder()
                .property(key, value)
                .build();
    }
    
    /**
     * Create a new metadata with additional tag
     */
    public DocumentMetadata withTag(String tag) {
        return toBuilder()
                .tag(tag)
                .build();
    }
    
    /**
     * Convert to builder for modifications
     */
    public Builder toBuilder() {
        return new Builder()
                .properties(this.properties)
                .tags(this.tags)
                .source(this.source)
                .uploadedAt(this.uploadedAt);
    }
    
    /**
     * Create a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create empty metadata
     */
    public static DocumentMetadata empty() {
        return new Builder().build();
    }
    
    /**
     * Builder for DocumentMetadata
     */
    public static class Builder {
        private final Map<String, String> properties = new HashMap<>();
        private final Set<String> tags = new HashSet<>();
        private String source = "manual_upload";
        private Instant uploadedAt;
        
        public Builder property(String key, String value) {
            Objects.requireNonNull(key, "Property key cannot be null");
            Objects.requireNonNull(value, "Property value cannot be null");
            this.properties.put(key, value);
            return this;
        }
        
        public Builder properties(Map<String, String> properties) {
            Objects.requireNonNull(properties, "Properties cannot be null");
            this.properties.putAll(properties);
            return this;
        }
        
        public Builder tag(String tag) {
            Objects.requireNonNull(tag, "Tag cannot be null");
            if (!tag.isBlank()) {
                this.tags.add(tag.trim().toLowerCase());
            }
            return this;
        }
        
        public Builder tags(Collection<String> tags) {
            Objects.requireNonNull(tags, "Tags cannot be null");
            tags.stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(tag -> tag.trim().toLowerCase())
                    .forEach(this.tags::add);
            return this;
        }
        
        public Builder source(String source) {
            this.source = Objects.requireNonNull(source, "Source cannot be null");
            return this;
        }
        
        public Builder uploadedAt(Instant uploadedAt) {
            this.uploadedAt = uploadedAt;
            return this;
        }
        
        public DocumentMetadata build() {
            return new DocumentMetadata(this);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentMetadata that = (DocumentMetadata) o;
        return Objects.equals(properties, that.properties) &&
               Objects.equals(tags, that.tags) &&
               Objects.equals(source, that.source) &&
               Objects.equals(uploadedAt, that.uploadedAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(properties, tags, source, uploadedAt);
    }
}