package com.aiagentkit.rag;

import java.util.List;
import java.util.Map;

/**
 * Interface for vector stores that store and retrieve document embeddings.
 */
public interface VectorStore {

    /**
     * Add documents with their embeddings to the store.
     */
    void addDocuments(List<Document> documents);

    /**
     * Add a single document with its embedding.
     */
    void addDocument(Document document);

    /**
     * Search for the most similar documents to a query embedding.
     *
     * @param embedding the query embedding vector
     * @param topK      the number of results to return
     * @return the top-k most similar documents with similarity scores
     */
    List<SimilarityResult> search(List<Double> embedding, int topK);

    /**
     * Delete a document by ID.
     */
    void deleteDocument(String documentId);

    /**
     * Get the total number of documents in the store.
     */
    int getDocumentCount();

    /**
     * Clear all documents from the store.
     */
    void clear();

    /**
     * Result of a similarity search.
     */
    class SimilarityResult {
        private final Document document;
        private final double score;

        public SimilarityResult(Document document, double score) {
            this.document = document;
            this.score = score;
        }

        public Document getDocument() { return document; }
        public double getScore() { return score; }
    }
}
