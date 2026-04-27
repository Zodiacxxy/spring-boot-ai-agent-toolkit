package com.aiagentkit.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Interface for AI provider integrations.
 * Each provider (OpenAI, Anthropic, Ollama, etc.) implements this interface.
 */
public interface AiProvider {

    /**
     * Send a chat completion request to the AI provider.
     *
     * @param model         the model name (e.g., "gpt-4", "claude-3-opus")
     * @param messages      the conversation messages
     * @param systemPrompt  the system prompt
     * @param temperature   the sampling temperature
     * @param maxTokens     the maximum tokens to generate
     * @param tools         the tool definitions available to the model
     * @return the provider response
     */
    AiProviderResponse chat(
        String model,
        List<Map<String, Object>> messages,
        String systemPrompt,
        double temperature,
        int maxTokens,
        List<Map<String, Object>> tools
    );

    /**
     * Stream a chat completion response from the AI provider.
     *
     * @param model         the model name
     * @param messages      the conversation messages
     * @param systemPrompt  the system prompt
     * @param temperature   the sampling temperature
     * @param maxTokens     the maximum tokens to generate
     * @param tools         the tool definitions
     * @param onChunk       callback for each streaming chunk
     */
    void chatStream(
        String model,
        List<Map<String, Object>> messages,
        String systemPrompt,
        double temperature,
        int maxTokens,
        List<Map<String, Object>> tools,
        Function<AiStreamChunk, Boolean> onChunk
    );

    /**
     * Get the provider name (e.g., "openai", "anthropic", "ollama").
     */
    String getProviderName();

    /**
     * Check if the provider is properly configured and reachable.
     */
    boolean isAvailable();
}
