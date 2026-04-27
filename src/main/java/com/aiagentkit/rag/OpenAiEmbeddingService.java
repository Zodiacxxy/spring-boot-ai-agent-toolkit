package com.aiagentkit.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OpenAI-compatible embedding service.
 * Supports OpenAI, Ollama, and any API that follows the OpenAI embeddings format.
 */
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int dimension;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiEmbeddingService(String apiKey, String model, int dimension) {
        this(apiKey, "https://api.openai.com/v1", model, dimension);
    }

    public OpenAiEmbeddingService(String apiKey, String baseUrl, String model, int dimension) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.dimension = dimension;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public List<Double> embed(String text) {
        List<String> results = embedBatch(Collections.singletonList(text));
        return results.isEmpty() ? Collections.emptyList() : parseEmbedding(results.get(0));
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("encoding_format", "float");

            ArrayNode inputArray = objectMapper.createArrayNode();
            for (String text : texts) {
                inputArray.add(text);
            }
            requestBody.set("input", inputArray);

            Request.Builder requestBuilder = new Request.Builder()
                .url(baseUrl + "/embeddings")
                .post(RequestBody.create(objectMapper.writeValueAsString(requestBody), JSON));

            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
            }

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Embedding API error: {}", response.code());
                    return texts.stream().map(t -> Collections.<Double>nCopies(dimension, 0.0))
                        .collect(Collectors.toList());
                }

                String responseBody = response.body() != null ? response.body().string() : "{}";
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode data = root.get("data");

                if (data != null && data.isArray()) {
                    List<List<Double>> embeddings = new ArrayList<>();
                    for (JsonNode item : data) {
                        List<Double> vector = new ArrayList<>();
                        JsonNode embedding = item.get("embedding");
                        if (embedding != null && embedding.isArray()) {
                            for (JsonNode val : embedding) {
                                vector.add(val.asDouble());
                            }
                        }
                        embeddings.add(vector);
                    }
                    return embeddings;
                }
            }
        } catch (Exception e) {
            log.error("Embedding error: {}", e.getMessage());
        }
        return texts.stream().map(t -> Collections.<Double>nCopies(dimension, 0.0))
            .collect(Collectors.toList());
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getModelName() {
        return model;
    }

    private List<Double> parseEmbedding(String s) {
        // Placeholder - actual parsing happens in embedBatch
        return Collections.nCopies(dimension, 0.0);
    }
}
