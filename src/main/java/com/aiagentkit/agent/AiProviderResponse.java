package com.aiagentkit.agent;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Response from an AI provider after a chat completion request.
 */
public class AiProviderResponse {

    private final String content;
    private final List<AiToolCall> toolCalls;
    private final String model;
    private final int promptTokens;
    private final int completionTokens;

    public AiProviderResponse(String content, List<AiToolCall> toolCalls,
                              String model, int promptTokens, int completionTokens) {
        this.content = content;
        this.toolCalls = toolCalls != null ? toolCalls : new ArrayList<>();
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
    }

    public String getContent() { return content; }
    public List<AiToolCall> getToolCalls() { return toolCalls; }
    public boolean hasToolCalls() { return toolCalls != null && !toolCalls.isEmpty(); }
    public String getModel() { return model; }
    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public int getTotalTokens() { return promptTokens + completionTokens; }
}
