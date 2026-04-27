package com.aiagentkit.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Splits documents into smaller chunks for embedding and retrieval.
 * Supports multiple chunking strategies.
 */
public class DocumentChunker {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunker.class);

    private final int chunkSize;
    private final int chunkOverlap;

    public DocumentChunker() {
        this(500, 50);
    }

    public DocumentChunker(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * Split a document into chunks by character count with overlap.
     */
    public List<Document> chunkByCharacterCount(Document document) {
        String content = document.getContent();
        List<Document> chunks = new ArrayList<>();

        int start = 0;
        int chunkIndex = 0;

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            String chunkContent = content.substring(start, end);

            Map<String, Object> metadata = new HashMap<>(document.getMetadata());
            metadata.put("chunk_index", chunkIndex);
            metadata.put("chunk_start", start);
            metadata.put("chunk_end", end);
            metadata.put("source_document_id", document.getId());

            Document chunk = new Document(
                document.getId() + "_chunk_" + chunkIndex,
                chunkContent,
                metadata
            );

            chunks.add(chunk);
            chunkIndex++;

            if (end >= content.length()) break;

            start = end - chunkOverlap;
            if (start >= content.length()) break;
        }

        log.debug("Split document '{}' into {} chunks (size={}, overlap={})",
            document.getId(), chunks.size(), chunkSize, chunkOverlap);
        return chunks;
    }

    /**
     * Split a document into chunks by sentence boundaries.
     */
    public List<Document> chunkBySentence(Document document) {
        String content = document.getContent();
        String[] sentences = content.split("(?<=[.!?])\\s+");
        List<Document> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                Map<String, Object> metadata = new HashMap<>(document.getMetadata());
                metadata.put("chunk_index", chunkIndex);
                metadata.put("source_document_id", document.getId());

                chunks.add(new Document(
                    document.getId() + "_chunk_" + chunkIndex,
                    currentChunk.toString(),
                    metadata
                ));
                chunkIndex++;
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence).append(" ");
        }

        if (currentChunk.length() > 0) {
            Map<String, Object> metadata = new HashMap<>(document.getMetadata());
            metadata.put("chunk_index", chunkIndex);
            metadata.put("source_document_id", document.getId());

            chunks.add(new Document(
                document.getId() + "_chunk_" + chunkIndex,
                currentChunk.toString(),
                metadata
            ));
        }

        return chunks;
    }

    /**
     * Split multiple documents into chunks.
     */
    public List<Document> chunkDocuments(List<Document> documents) {
        List<Document> allChunks = new ArrayList<>();
        for (Document doc : documents) {
            allChunks.addAll(chunkByCharacterCount(doc));
        }
        return allChunks;
    }

    public int getChunkSize() { return chunkSize; }
    public int getChunkOverlap() { return chunkOverlap; }
}
