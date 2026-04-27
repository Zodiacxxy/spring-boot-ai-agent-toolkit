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

/**
 * Anthropic Claude provider integration.
 * Supports Claude 3.5 Sonnet, Claude 3 Opus, Claude 3 Haiku, Claude 4, etc.
 */
public class AnthropicProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicProvider.class);
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicProvider(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL);
    }

    public AnthropicProvider(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
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
            requestBody.put("system", systemPrompt);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            ArrayNode messagesArray = objectMapper.createArrayNode();
            for (Map<String, Object> msg : messages) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                String role = (String) msg.get("role");
                // Anthropic uses "user" and "assistant"; skip tool messages for simplicity
                if ("tool".equals(role)) {
                    ObjectNode toolResult = objectMapper.createObjectNode();
                    toolResult.put("role", "user");
                    toolResult.put("content", "[Tool " + msg.get("name") + " result]: " + msg.get("content"));
                    messagesArray.add(toolResult);
                } else if ("user".equals(role) || "assistant".equals(role)) {
                    msgNode.put("role", role);
                    msgNode.put("content", (String) msg.get("content"));
                    messagesArray.add(msgNode);
                }
            }
            requestBody.set("messages", messagesArray);

            // Build tools for Anthropic format
            if (tools != null && !tools.isEmpty()) {
                ArrayNode toolsArray = objectMapper.createArrayNode();
                for (Map<String, Object> toolDef : tools) {
                    ObjectNode anthropicTool = objectMapper.createObjectNode();
                    Map<String, Object> function = (Map<String, Object>) toolDef.get("function");
                    anthropicTool.put("name", (String) function.get("name"));
                    anthropicTool.put("description", (String) function.get("description"));
                    anthropicTool.set("input_schema", objectMapper.valueToTree(function.get("parameters")));
                    toolsArray.add(anthropicTool);
                }
                requestBody.set("tools", toolsArray);
            }

            String json = objectMapper.writeValueAsString(requestBody);
            Request request = new Request.Builder()
                .url(baseUrl + "/messages")
                .post(RequestBody.create(json, JSON))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Anthropic API error ({}): {}", response.code(), errorBody);
                    return new AiProviderResponse("Error: API returned " + response.code(), null, model, 0, 0);
                }

                String responseBody = response.body() != null ? response.body().string() : "{}";
                JsonNode root = objectMapper.readTree(responseBody);

                String content = "";
                List<AiToolCall> toolCalls = new ArrayList<>();

                JsonNode contentArray = root.get("content");
                if (contentArray != null && contentArray.isArray()) {
                    for (JsonNode block : contentArray) {
                        String type = block.get("type").asText();
                        if ("text".equals(type)) {
                            content += block.get("text").asText();
                        } else if ("tool_use".equals(type)) {
                            String id = block.get("id").asText();
                            String name = block.get("name").asText();
                            String args = block.get("input").toString();
                            toolCalls.add(new AiToolCall(id, name, args));
                        }
                    }
                }

                int inputTokens = root.has("usage") && root.get("usage").has("input_tokens")
                    ? root.get("usage").get("input_tokens").asInt() : 0;
                int outputTokens = root.has("usage") && root.get("usage").has("output_tokens")
                    ? root.get("usage").get("output_tokens").asInt() : 0;

                return new AiProviderResponse(content, toolCalls, model, inputTokens, outputTokens);
            }
        } catch (Exception e) {
            log.error("Anthropic chat error: {}", e.getMessage());
            return new AiProviderResponse("Error: " + e.getMessage(), null, model, 0, 0);
        }
    }

    @Override
    public void chatStream(String model, List<Map<String, Object>> messages,
                           String systemPrompt, double temperature, int maxTokens,
                           List<Map<String, Object>> tools,
                           Function<AiStreamChunk, Boolean> onChunk) {
        // Anthropic streaming uses SSE - similar to OpenAI but with different format
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("system", systemPrompt);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("stream", true);

            ArrayNode messagesArray = objectMapper.createArrayNode();
            for (Map<String, Object> msg : messages) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                String role = (String) msg.get("role");
                if ("user".equals(role) || "assistant".equals(role)) {
                    msgNode.put("role", role);
                    msgNode.put("content", (String) msg.get("content"));
                    messagesArray.add(msgNode);
                }
            }
            requestBody.set("messages", messagesArray);

            if (tools != null && !tools.isEmpty()) {
                ArrayNode toolsArray = objectMapper.createArrayNode();
                for (Map<String, Object> toolDef : tools) {
                    ObjectNode anthropicTool = objectMapper.createObjectNode();
                    Map<String, Object> function = (Map<String, Object>) toolDef.get("function");
                    anthropicTool.put("name", (String) function.get("name"));
                    anthropicTool.put("description", (String) function.get("description"));
                    anthropicTool.set("input_schema", objectMapper.valueToTree(function.get("parameters")));
                    toolsArray.add(anthropicTool);
                }
                requestBody.set("tools", toolsArray);
            }

            String json = objectMapper.writeValueAsString(requestBody);
            Request request = new Request.Builder()
                .url(baseUrl + "/messages")
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
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        try {
                            JsonNode event = objectMapper.readTree(data);
                            String type = event.get("type").asText();

                            if ("content_block_delta".equals(type)) {
                                JsonNode delta = event.get("delta");
                                if (delta != null && "text_delta".equals(delta.get("type").asText())) {
                                    String text = delta.get("text").asText();
                                    boolean stop = !onChunk.apply(new AiStreamChunk(text, null, false));
                                    if (stop) return;
                                }
                            } else if ("message_stop".equals(type)) {
                                onChunk.apply(new AiStreamChunk("", null, true));
                                return;
                            }
                        } catch (Exception e) {
                            log.debug("Error parsing SSE: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Anthropic streaming error: {}", e.getMessage());
            onChunk.apply(new AiStreamChunk("Error: " + e.getMessage(), null, true));
        }
    }

    @Override
    public String getProviderName() {
        return "anthropic";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
