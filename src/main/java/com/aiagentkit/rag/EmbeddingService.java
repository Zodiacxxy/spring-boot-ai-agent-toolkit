package com.aiagentkit.rag;

import java.util.List;

/**
 * Interface for embedding services that convert text to vector embeddings.
 */
public interface EmbeddingService {

    /**
     * Generate an embedding vector for a single text.
     */
    List<Double> embed(String text);

    /**
     * Generate embedding vectors for multiple texts (batch).
     */
    List<List<Double>> embedBatch(List<String> texts);

    /**
     * Get the dimension of the embedding vectors.
     */
    int getDimension();

    /**
     * Get the name of the embedding model being used.
     */
    String getModelName();
}
