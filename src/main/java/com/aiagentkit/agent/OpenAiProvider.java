package com.aiagentkit.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OpenAI-compatible provider integration.
 * Supports GPT-4, GPT-4o, GPT-3.5-turbo, and any OpenAI-compatible API.
 */
public class OpenAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiProvider(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL);
    }

    public OpenAiProvider(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
                return chain.proceed(request);
            })
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
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("stream", false);

            // Build messages array with system prompt
            ArrayNode messagesArray = objectMapper.createArrayNode();
            ObjectNode systemNode = objectMapper.createObjectNode();
            systemNode.put("role", "system");
            systemNode.put("content", systemPrompt);
            messagesArray.add(systemNode);

            for (Map<String, Object> msg : messages) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msg.put("role", msg.get("role").toString());
                msg.put("content", msg.get("content").toString());
                messagesArray.add(msgNode);
            }
            requestBody.set("messages", messagesArray);

            // Add tools if present
            if (tools != null && !tools.isEmpty()) {
                ArrayNode toolsArray = objectMapper.createArrayNode();
                for (Map<String, Object> toolDef : tools) {
                    JsonNode toolNode = objectMapper.valueToTree(toolDef);
                    toolsArray.add(toolNode);
                }
                requestBody.set("tools", toolsArray);
            }

            String json = objectMapper.writeValueAsString(requestBody);
            Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .post(RequestBody.create(json, JSON))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("OpenAI API error ({}): {}", response.code(), errorBody);
                    return new AiProviderResponse(
                        "Error: API returned status " + response.code(),
                        null, model, 0, 0
                    );
                }

                String responseBody = response.body() != null ? response.body().string() : "{}";
                JsonNode root = objectMapper.readTree(responseBody);

                // Parse content
                String content = null;
                List<AiToolCall> toolCalls = new ArrayList<>();

                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null) {
                        JsonNode contentNode = message.get("content");
                        if (contentNode != null && !contentNode.isNull()) {
                            content = contentNode.asText();
                        }

                        JsonNode toolCallsNode = message.get("tool_calls");
                        if (toolCallsNode != null && toolCallsNode.isArray()) {
                            for (JsonNode tc : toolCallsNode) {
                                String id = tc.get("id").asText();
                                JsonNode function = tc.get("function");
                                String name = function.get("name").asText();
                                String args = function.get("arguments").asText();
                                toolCalls.add(new AiToolCall(id, name, args));
                            }
                        }
                    }
                }

                // Parse usage
                int promptTokens = 0;
                int completionTokens = 0;
                JsonNode usage = root.get("usage");
                if (usage != null) {
                    promptTokens = usage.get("prompt_tokens").asInt();
                    completionTokens = usage.get("completion_tokens").asInt();
                }

                return new AiProviderResponse(content, toolCalls, model, promptTokens, completionTokens);
            }
        } catch (Exception e) {
            log.error("OpenAI chat error: {}", e.getMessage());
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
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("stream", true);

            ArrayNode messagesArray = objectMapper.createArrayNode();
            ObjectNode systemNode = objectMapper.createObjectNode();
            systemNode.put("role", "system");
            systemNode.put("content", systemPrompt);
            messagesArray.add(systemNode);

            for (Map<String, Object> msg : messages) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", (String) msg.get("role"));
                msgNode.put("content", (String) msg.get("content"));
                messagesArray.add(msgNode);
            }
            requestBody.set("messages", messagesArray);

            if (tools != null && !tools.isEmpty()) {
                ArrayNode toolsArray = objectMapper.createArrayNode();
                for (Map<String, Object> toolDef : tools) {
                    toolsArray.add(objectMapper.valueToTree(toolDef));
                }
                requestBody.set("tools", toolsArray);
            }

            String json = objectMapper.writeValueAsString(requestBody);
            Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .post(RequestBody.create(json, JSON))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("OpenAI streaming error ({}): {}", response.code(), errorBody);
                    onChunk.apply(new AiStreamChunk("Error: " + errorBody, null, true));
                    return;
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) return;

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8)
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            onChunk.apply(new AiStreamChunk("", null, true));
                            return;
                        }

                        try {
                            JsonNode chunk = objectMapper.readTree(data);
                            JsonNode choices = chunk.get("choices");
                            if (choices != null && choices.isArray() && choices.size() > 0) {
                                JsonNode delta = choices.get(0).get("delta");
                                if (delta != null) {
                                    String content = delta.get("content") != null ?
                                        delta.get("content").asText() : "";
                                    List<AiToolCall> toolCalls = null;

                                    JsonNode toolCallsNode = delta.get("tool_calls");
                                    if (toolCallsNode != null && toolCallsNode.isArray()) {
                                        toolCalls = new ArrayList<>();
                                        for (JsonNode tc : toolCallsNode) {
                                            JsonNode function = tc.get("function");
                                            if (function != null) {
                                                String id = tc.get("id") != null ? tc.get("id").asText() : UUID.randomUUID().toString();
                                                String name = function.get("name") != null ? function.get("name").asText() : "";
                                                String args = function.get("arguments") != null ? function.get("arguments").asText() : "";
                                                toolCalls.add(new AiToolCall(id, name, args));
                                            }
                                        }
                                    }

                                    boolean stop = !onChunk.apply(new AiStreamChunk(content, toolCalls, false));
                                    if (stop) return;
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Error parsing SSE chunk: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("OpenAI streaming error: {}", e.getMessage());
            onChunk.apply(new AiStreamChunk("Error: " + e.getMessage(), null, true));
        }
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("sk-your-api-key-here");
    }
}
