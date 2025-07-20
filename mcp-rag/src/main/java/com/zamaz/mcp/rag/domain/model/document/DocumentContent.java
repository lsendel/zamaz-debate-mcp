package com.zamaz.mcp.rag.domain.model.document;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Value Object representing the content of a document.
 * Handles both text and binary content with size constraints.
 */
public class DocumentContent {
    
    private static final int MAX_SIZE_MB = 50;
    private static final int MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024;
    
    private final byte[] data;
    private final String mimeType;
    private final boolean isText;
    
    private DocumentContent(byte[] data, String mimeType) {
        Objects.requireNonNull(data, "Document content cannot be null");
        Objects.requireNonNull(mimeType, "MIME type cannot be null");
        
        if (data.length == 0) {
            throw new IllegalArgumentException("Document content cannot be empty");
        }
        
        if (data.length > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(
                String.format("Document size exceeds maximum allowed size of %d MB", MAX_SIZE_MB)
            );
        }
        
        this.data = Arrays.copyOf(data, data.length); // Defensive copy
        this.mimeType = mimeType;
        this.isText = isTextMimeType(mimeType);
    }
    
    /**
     * Factory method for text content
     */
    public static DocumentContent fromText(String text) {
        Objects.requireNonNull(text, "Text content cannot be null");
        return new DocumentContent(text.getBytes(StandardCharsets.UTF_8), "text/plain");
    }
    
    /**
     * Factory method for binary content
     */
    public static DocumentContent fromBytes(byte[] data, String mimeType) {
        return new DocumentContent(data, mimeType);
    }
    
    /**
     * Get content as text (only for text content)
     */
    public String asText() {
        if (!isText) {
            throw new UnsupportedOperationException(
                "Cannot convert binary content to text. MIME type: " + mimeType
            );
        }
        return new String(data, StandardCharsets.UTF_8);
    }
    
    /**
     * Get content as bytes
     */
    public byte[] asBytes() {
        return Arrays.copyOf(data, data.length); // Defensive copy
    }
    
    /**
     * Get content size in bytes
     */
    public int getSizeInBytes() {
        return data.length;
    }
    
    /**
     * Get content size in KB
     */
    public double getSizeInKB() {
        return data.length / 1024.0;
    }
    
    /**
     * Get MIME type
     */
    public String getMimeType() {
        return mimeType;
    }
    
    /**
     * Check if content is text
     */
    public boolean isText() {
        return isText;
    }
    
    /**
     * Check if content is of specific MIME type
     */
    public boolean hasMimeType(String mimeType) {
        return this.mimeType.equalsIgnoreCase(mimeType);
    }
    
    /**
     * Extract preview of content
     */
    public String getPreview(int maxLength) {
        if (!isText) {
            return String.format("[Binary content: %s, %.2f KB]", mimeType, getSizeInKB());
        }
        
        String text = asText();
        if (text.length() <= maxLength) {
            return text;
        }
        
        return text.substring(0, maxLength) + "...";
    }
    
    private static boolean isTextMimeType(String mimeType) {
        return mimeType.startsWith("text/") ||
               mimeType.equals("application/json") ||
               mimeType.equals("application/xml") ||
               mimeType.equals("application/javascript");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentContent that = (DocumentContent) o;
        return isText == that.isText &&
               Arrays.equals(data, that.data) &&
               Objects.equals(mimeType, that.mimeType);
    }
    
    @Override
    public int hashCode() {
        int result = Objects.hash(mimeType, isText);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}