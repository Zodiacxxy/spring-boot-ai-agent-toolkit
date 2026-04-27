package com.aiagentkit.rag;

import java.util.*;

/**
 * A document with content and metadata for RAG (Retrieval-Augmented Generation).
 */
public class Document {

    private final String id;
    private final String content;
    private final Map<String, Object> metadata;
    private List<Double> embedding;

    public Document(String id, String content) {
        this.id = id;
        this.content = content;
        this.metadata = new HashMap<>();
    }

    public Document(String id, String content, Map<String, Object> metadata) {
        this.id = id;
        this.content = content;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public String getId() { return id; }
    public String getContent() { return content; }
    public Map<String, Object> getMetadata() { return Collections.unmodifiableMap(metadata); }
    public List<Double> getEmbedding() { return embedding; }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    @Override
    public String toString() {
        return "Document{id='" + id + "', contentLength=" + content.length() + "}";
    }
}
