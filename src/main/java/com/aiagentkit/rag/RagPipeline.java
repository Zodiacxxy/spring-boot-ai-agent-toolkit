package com.aiagentkit.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) Pipeline.
 * Orchestrates document chunking, embedding, storage, and retrieval.
 */
public class RagPipeline {

    private static final Logger log = LoggerFactory.getLogger(RagPipeline.class);

    private final DocumentChunker chunker;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final int defaultTopK;

    public RagPipeline(DocumentChunker chunker, EmbeddingService embeddingService,
                       VectorStore vectorStore) {
        this(chunker, embeddingService, vectorStore, 5);
    }

    public RagPipeline(DocumentChunker chunker, EmbeddingService embeddingService,
                       VectorStore vectorStore, int defaultTopK) {
        this.chunker = chunker;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.defaultTopK = defaultTopK;
    }

    /**
     * Index a single document into the vector store.
     */
    public void indexDocument(Document document) {
        List<Document> chunks = chunker.chunkByCharacterCount(document);
        indexChunks(chunks);
        log.info("Indexed document '{}' ({} chunks)", document.getId(), chunks.size());
    }

    /**
     * Index multiple documents into the vector store.
     */
    public void indexDocuments(List<Document> documents) {
        List<Document> allChunks = new ArrayList<>();
        for (Document doc : documents) {
            allChunks.addAll(chunker.chunkByCharacterCount(doc));
        }
        indexChunks(allChunks);
        log.info("Indexed {} documents ({} total chunks)", documents.size(), allChunks.size());
    }

    /**
     * Index pre-chunked documents.
     */
    public void indexChunks(List<Document> chunks) {
        if (chunks.isEmpty()) return;

        List<String> texts = chunks.stream()
            .map(Document::getContent)
            .collect(Collectors.toList());

        List<List<Double>> embeddings = embeddingService.embedBatch(texts);

        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setEmbedding(embeddings.get(i));
        }

        vectorStore.addDocuments(chunks);
    }

    /**
     * Query the RAG pipeline: embed the query and retrieve relevant documents.
     *
     * @param query the user query
     * @return relevant context passages with similarity scores
     */
    public RagResult query(String query) {
        return query(query, defaultTopK);
    }

    /**
     * Query with a custom topK.
     */
    public RagResult query(String query, int topK) {
        List<Double> queryEmbedding = embeddingService.embed(query);
        List<VectorStore.SimilarityResult> results = vectorStore.search(queryEmbedding, topK);

        String context = results.stream()
            .map(r -> r.getDocument().getContent())
            .collect(Collectors.joining("\n\n---\n\n"));

        log.info("RAG query returned {} results", results.size());

        return new RagResult(context, results);
    }

    /**
     * Build a prompt with RAG context injected.
     */
    public String buildContextPrompt(String query, String userQuestion) {
        RagResult result = query(query);
        if (result.getContext().isEmpty()) {
            return userQuestion;
        }

        return "Context information:\n" +
            "----------------------\n" +
            result.getContext() +
            "\n----------------------\n" +
            "Based on the above context, please answer: " + userQuestion;
    }

    public VectorStore getVectorStore() { return vectorStore; }
    public EmbeddingService getEmbeddingService() { return embeddingService; }
    public DocumentChunker getChunker() { return chunker; }

    /**
     * Result of a RAG query.
     */
    public static class RagResult {
        private final String context;
        private final List<VectorStore.SimilarityResult> results;

        public RagResult(String context, List<VectorStore.SimilarityResult> results) {
            this.context = context;
            this.results = results;
        }

        public String getContext() { return context; }
        public List<VectorStore.SimilarityResult> getResults() { return results; }
    }
}
