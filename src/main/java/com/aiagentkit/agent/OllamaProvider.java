package com.aiagentkit.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ollama provider for local LLM inference.
 * Connects to a local Ollama instance (default: http://localhost:11434).
 * No API key required -- perfect for development and self-hosted deployments.
 */
public class OllamaProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaProvider.class);
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaProvider() {
        this(DEFAULT_BASE_URL);
    }

    public OllamaProvider(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public AiProviderResponse chat(String model, List<Map<String, Object>> messages,
                                   String systemPrompt, double temperature,
                                   int maxTokens, List<Map<String, Object>> tools) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("num_predict", maxTokens);
            requestBody.put("stream", false);

            ArrayNode messagesArray = objectMapper.createArrayNode();

            // Add system message
            ObjectNode systemNode = objectMapper.createObjectNode();
            systemNode.put("role", "system");
            systemNode.put("content", systemPrompt);
            messagesArray.add(systemNode);

            // Add conversation messages
            for (Map<String, Object> msg : messages) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                String role = (String) msg.get("role");
                msgNode.put("role", "tool".equals(role) ? "user" : role);
                String content = (String) msg.get("content");
                if ("tool".equals(role)) {
                    content = "[Tool " + msg.get("name") + " result]: " + content;
                }
                msgNode.put("content", content);
                messagesArray.add(msgNode);
            }
            requestBody.set("messages", messagesArray);

            if (tools != null && !tools.isEmpty()) {
                ArrayNode toolsArray = objectMapper.createArrayNode();
                for (Map<String, Object> toolDef : tools) {
                    toolsArray.add(objectMapper.valueToTree(toolDef.get("function")));
                }
                requestBody.set("tools", toolsArray);
            }

            String json = objectMapper.writeValueAsString(requestBody);
            Request request = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .post(RequestBody.create(json, JSON))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Ollama error ({}): {}", response.code(), errorBody);
                    return new AiProviderResponse("Error: " + response.code(), null, model, 0, 0);
                }

                String responseBody = response.body() != null ? response.body().string() : "{}";
                JsonNode root = objectMapper.readTree(responseBody);

                JsonNode message = root.get("message");
                String content = message != null && message.get("content") != null
                    ? message.get("content").asText() : "";

                List<AiToolCall> toolCalls = new ArrayList<>();
                JsonNode toolCallsNode = root.get("message").get("tool_calls");
                if (toolCallsNode != null && toolCallsNode.isArray()) {
                    for (JsonNode tc : toolCallsNode) {
                        String name = tc.get("function").get("name").asText();
                        String args = tc.get("function").get("arguments").toString();
                        toolCalls.add(new AiToolCall(UUID.randomUUID().toString(), name, args));
                    }
                }

                return new AiProviderResponse(content, toolCalls, model, 0, 0);
            }
        } catch (Exception e) {
            log.error("Ollama chat error: {}", e.getMessage());
            return new AiProviderResponse("Error: " + e.getMessage(), null, model, 0, 0);
        }
    }

    @Override
    public void chatStream(String model, List<Map<String, Object>> messages,
                           String systemPrompt, double temperature, int maxTokens,
                           List<Map<String, Object>> tools,
                           Function<AiStreamChunk, Boolean> onChunk) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("num_predict", maxTokens);
            requestBody.put("stream", true);

            ArrayNode messagesArray = objectMapper.createArrayNode();
            ObjectNode systemNode = objectMapper.createObjectNode();
            systemNode.put("role", "system");
            systemNode.put("content", systemPrompt);
            messagesArray.add(systemNode);

            for (Map<String, Object> msg : messages) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                String role = (String) msg.get("role");
                msgNode.put("role", "tool".equals(role) ? "user" : role);
                msgNode.put("content", (String) msg.get("content"));
                messagesArray.add(msgNode);
            }
            requestBody.set("messages", messagesArray);

            String json = objectMapper.writeValueAsString(requestBody);
            Request request = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .post(RequestBody.create(json, JSON))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    onChunk.apply(new AiStreamChunk("Error: " + response.code(), null, true));
                    return;
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) return;

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8)
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        JsonNode chunk = objectMapper.readTree(line);
                        JsonNode message = chunk.get("message");
                        if (message != null && message.get("content") != null) {
                            boolean stop = !onChunk.apply(
                                new AiStreamChunk(message.get("content").asText(), null, false));
                            if (stop) return;
                        }
                        if (chunk.get("done").asBoolean(false)) {
                            onChunk.apply(new AiStreamChunk("", null, true));
                            return;
                        }
                    } catch (Exception e) {
                        log.debug("Error parsing Ollama chunk: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ollama streaming error: {}", e.getMessage());
            onChunk.apply(new AiStreamChunk("Error: " + e.getMessage(), null, true));
        }
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public boolean isAvailable() {
        try {
            Request request = new Request.Builder()
                .url(baseUrl)
                .get()
                .build();
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
