package com.zamaz.mcp.rag.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Value object representing metadata associated with a document.
 */
public record DocumentMetadata(Map<String, String> properties) implements ValueObject {
    
    private static final int MAX_KEY_LENGTH = 255;
    private static final int MAX_VALUE_LENGTH = 5000;
    private static final int MAX_PROPERTIES = 100;
    
    // Common metadata keys
    public static final String AUTHOR = "author";
    public static final String TITLE = "title";
    public static final String SUBJECT = "subject";
    public static final String KEYWORDS = "keywords";
    public static final String LANGUAGE = "language";
    public static final String CREATION_DATE = "creation_date";
    public static final String MODIFICATION_DATE = "modification_date";
    public static final String FILE_SIZE = "file_size";
    public static final String PAGE_COUNT = "page_count";
    public static final String WORD_COUNT = "word_count";
    
    public DocumentMetadata {
        Objects.requireNonNull(properties, "Properties cannot be null");
        
        if (properties.size() > MAX_PROPERTIES) {
            throw new IllegalArgumentException(
                "Cannot have more than " + MAX_PROPERTIES + " metadata properties"
            );
        }
        
        // Validate keys and values
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("Metadata key cannot be null or empty");
            }
            if (key.length() > MAX_KEY_LENGTH) {
                throw new IllegalArgumentException(
                    "Metadata key cannot exceed " + MAX_KEY_LENGTH + " characters: " + key
                );
            }
            if (value != null && value.length() > MAX_VALUE_LENGTH) {
                throw new IllegalArgumentException(
                    "Metadata value cannot exceed " + MAX_VALUE_LENGTH + " characters for key: " + key
                );
            }
        }
        
        // Create immutable copy
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
    }
    
    public static DocumentMetadata empty() {
        return new DocumentMetadata(Map.of());
    }
    
    public static DocumentMetadata of(Map<String, String> properties) {
        return new DocumentMetadata(properties != null ? properties : Map.of());
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Optional<String> get(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return Optional.ofNullable(properties.get(key));
    }
    
    public String get(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }
    
    public boolean contains(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return properties.containsKey(key);
    }
    
    public DocumentMetadata with(String key, String value) {
        Objects.requireNonNull(key, "Key cannot be null");
        Map<String, String> newProperties = new HashMap<>(properties);
        if (value != null) {
            newProperties.put(key, value);
        } else {
            newProperties.remove(key);
        }
        return new DocumentMetadata(newProperties);
    }
    
    public DocumentMetadata without(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        Map<String, String> newProperties = new HashMap<>(properties);
        newProperties.remove(key);
        return new DocumentMetadata(newProperties);
    }
    
    public DocumentMetadata merge(DocumentMetadata other) {
        Objects.requireNonNull(other, "Other metadata cannot be null");
        Map<String, String> merged = new HashMap<>(this.properties);
        merged.putAll(other.properties);
        return new DocumentMetadata(merged);
    }
    
    public int size() {
        return properties.size();
    }
    
    public boolean isEmpty() {
        return properties.isEmpty();
    }
    
    public Optional<String> getAuthor() {
        return get(AUTHOR);
    }
    
    public Optional<String> getTitle() {
        return get(TITLE);
    }
    
    public Optional<String> getSubject() {
        return get(SUBJECT);
    }
    
    public Optional<String> getLanguage() {
        return get(LANGUAGE);
    }
    
    public Optional<Long> getFileSize() {
        return get(FILE_SIZE).map(Long::parseLong);
    }
    
    public Optional<Integer> getPageCount() {
        return get(PAGE_COUNT).map(Integer::parseInt);
    }
    
    public Optional<Integer> getWordCount() {
        return get(WORD_COUNT).map(Integer::parseInt);
    }
    
    public static class Builder {
        private final Map<String, String> properties = new HashMap<>();
        
        public Builder put(String key, String value) {
            if (key != null && value != null) {
                properties.put(key, value);
            }
            return this;
        }
        
        public Builder author(String author) {
            return put(AUTHOR, author);
        }
        
        public Builder title(String title) {
            return put(TITLE, title);
        }
        
        public Builder subject(String subject) {
            return put(SUBJECT, subject);
        }
        
        public Builder keywords(String keywords) {
            return put(KEYWORDS, keywords);
        }
        
        public Builder language(String language) {
            return put(LANGUAGE, language);
        }
        
        public Builder fileSize(long fileSize) {
            return put(FILE_SIZE, String.valueOf(fileSize));
        }
        
        public Builder pageCount(int pageCount) {
            return put(PAGE_COUNT, String.valueOf(pageCount));
        }
        
        public Builder wordCount(int wordCount) {
            return put(WORD_COUNT, String.valueOf(wordCount));
        }
        
        public DocumentMetadata build() {
            return new DocumentMetadata(properties);
        }
    }
    
    @Override
    public String toString() {
        return "DocumentMetadata{size=" + size() + ", properties=" + properties + "}";
    }
}