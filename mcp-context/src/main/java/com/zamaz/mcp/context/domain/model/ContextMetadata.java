package com.zamaz.mcp.context.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Value object representing extensible metadata for a Context.
 * Immutable map of key-value pairs.
 */
public class ContextMetadata implements ValueObject {
    
    private final Map<String, Object> data;
    
    private ContextMetadata(Map<String, Object> data) {
        this.data = Collections.unmodifiableMap(new HashMap<>(data));
    }
    
    public static ContextMetadata empty() {
        return new ContextMetadata(Collections.emptyMap());
    }
    
    public static ContextMetadata of(Map<String, Object> data) {
        Objects.requireNonNull(data, "Metadata map cannot be null");
        return new ContextMetadata(data);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Optional<Object> get(String key) {
        return Optional.ofNullable(data.get(key));
    }
    
    public <T> Optional<T> get(String key, Class<T> type) {
        return get(key)
            .filter(type::isInstance)
            .map(type::cast);
    }
    
    public boolean contains(String key) {
        return data.containsKey(key);
    }
    
    public boolean isEmpty() {
        return data.isEmpty();
    }
    
    public int size() {
        return data.size();
    }
    
    public Map<String, Object> asMap() {
        return new HashMap<>(data);
    }
    
    public ContextMetadata with(String key, Object value) {
        Map<String, Object> newData = new HashMap<>(data);
        newData.put(key, value);
        return new ContextMetadata(newData);
    }
    
    public ContextMetadata without(String key) {
        if (!data.containsKey(key)) {
            return this;
        }
        Map<String, Object> newData = new HashMap<>(data);
        newData.remove(key);
        return new ContextMetadata(newData);
    }
    
    public ContextMetadata merge(ContextMetadata other) {
        Objects.requireNonNull(other, "Other metadata cannot be null");
        Map<String, Object> newData = new HashMap<>(data);
        newData.putAll(other.data);
        return new ContextMetadata(newData);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextMetadata that = (ContextMetadata) o;
        return Objects.equals(data, that.data);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
    
    @Override
    public String toString() {
        return "ContextMetadata" + data;
    }
    
    public static class Builder {
        private final Map<String, Object> data = new HashMap<>();
        
        public Builder put(String key, Object value) {
            Objects.requireNonNull(key, "Key cannot be null");
            data.put(key, value);
            return this;
        }
        
        public Builder putAll(Map<String, Object> map) {
            Objects.requireNonNull(map, "Map cannot be null");
            data.putAll(map);
            return this;
        }
        
        public ContextMetadata build() {
            return new ContextMetadata(data);
        }
    }
}