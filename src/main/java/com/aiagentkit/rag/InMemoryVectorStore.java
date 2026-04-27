package com.aiagentkit.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * In-memory vector store implementation using cosine similarity.
 * Perfect for development, testing, and small-scale deployments.
 * For production, use ChromaVectorStore or PgVectorStore.
 */
public class InMemoryVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryVectorStore.class);

    private final Map<String, Document> documents;
    private final Map<String, List<Double>> embeddings;

    public InMemoryVectorStore() {
        this.documents = new ConcurrentHashMap<>();
        this.embeddings = new ConcurrentHashMap<>();
    }

    @Override
    public void addDocuments(List<Document> docs) {
        for (Document doc : docs) {
            addDocument(doc);
        }
    }

    @Override
    public void addDocument(Document document) {
        if (document.getEmbedding() == null || document.getEmbedding().isEmpty()) {
            log.warn("Document '{}' has no embedding, skipping", document.getId());
            return;
        }
        documents.put(document.getId(), document);
        embeddings.put(document.getId(), document.getEmbedding());
        log.debug("Added document '{}' to vector store", document.getId());
    }

    @Override
    public List<SimilarityResult> search(List<Double> queryEmbedding, int topK) {
        if (embeddings.isEmpty()) {
            return Collections.emptyList();
        }

        List<SimilarityResult> results = embeddings.entrySet().stream()
            .map(entry -> {
                double score = cosineSimilarity(queryEmbedding, entry.getValue());
                return new SimilarityResult(documents.get(entry.getKey()), score);
            })
            .filter(r -> r.getScore() > 0.0)
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(topK)
            .collect(Collectors.toList());

        log.debug("Vector search returned {} results (topK={})", results.size(), topK);
        return results;
    }

    @Override
    public void deleteDocument(String documentId) {
        documents.remove(documentId);
        embeddings.remove(documentId);
        log.debug("Deleted document '{}' from vector store", documentId);
    }

    @Override
    public int getDocumentCount() {
        return documents.size();
    }

    @Override
    public void clear() {
        documents.clear();
        embeddings.clear();
        log.info("Vector store cleared");
    }

    /**
     * Calculate cosine similarity between two vectors.
     */
    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            log.warn("Vector dimension mismatch: {} vs {}", a.size(), b.size());
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            double va = a.get(i);
            double vb = b.get(i);
            dotProduct += va * vb;
            normA += va * va;
            normB += vb * vb;
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0.0 ? 0.0 : dotProduct / denominator;
    }
}
