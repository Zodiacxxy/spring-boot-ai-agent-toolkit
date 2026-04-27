package com.aiagentkit.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core AI Agent that orchestrates conversations with LLM providers,
 * manages tool execution, and maintains conversation context.
 * <p>
 * This agent supports:
 * - Multiple AI providers (OpenAI, Anthropic, Ollama)
 * - Tool/function calling
 * - Streaming responses
 * - Conversation history management
 * - System prompt customization
 */
public class AiAgent {

    private static final Logger log = LoggerFactory.getLogger(AiAgent.class);

    private final AiProvider provider;
    private final String model;
    private final ObjectMapper objectMapper;
    private final Map<String, ToolExecutor> tools;
    private final List<Map<String, Object>> conversationHistory;
    private final AiAgentConfig config;

    public AiAgent(AiProvider provider, String model, AiAgentConfig config) {
        this.provider = provider;
        this.model = model;
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.tools = new ConcurrentHashMap<>();
        this.conversationHistory = new ArrayList<>();
    }

    /**
     * Register a tool that the agent can invoke.
     */
    public void registerTool(String name, ToolExecutor executor) {
        tools.put(name, executor);
        log.info("Registered tool: {}", name);
    }

    /**
     * Register multiple tools at once.
     */
    public void registerTools(Map<String, ToolExecutor> toolMap) {
        tools.putAll(toolMap);
        log.info("Registered {} tools", toolMap.size());
    }

    /**
     * Remove a registered tool.
     */
    public void unregisterTool(String name) {
        tools.remove(name);
        log.info("Unregistered tool: {}", name);
    }

    /**
     * Get all registered tool names.
     */
    public Set<String> getRegisteredTools() {
        return tools.keySet();
    }

    /**
     * Send a message and get a response from the AI agent.
     * Supports automatic tool calling with up to maxToolCalls iterations.
     */
    public String chat(String userMessage) {
        return chat(userMessage, config.getMaxToolCalls());
    }

    /**
     * Send a message with a custom tool call limit.
     */
    public String chat(String userMessage, int maxToolCalls) {
        addUserMessage(userMessage);

        int toolCallCount = 0;

        while (toolCallCount < maxToolCalls) {
            List<Map<String, Object>> messages = buildMessageContext();

            try {
                AiProviderResponse response = provider.chat(
                    model,
                    messages,
                    config.getSystemPrompt(),
                    config.getTemperature(),
                    config.getMaxTokens(),
                    buildToolDefinitions()
                );

                if (response.hasToolCalls()) {
                    for (AiToolCall toolCall : response.getToolCalls()) {
                        String toolName = toolCall.getName();
                        ToolExecutor executor = tools.get(toolName);

                        if (executor == null) {
                            log.warn("Tool '{}' not registered", toolName);
                            addToolResult(toolCall.getId(), toolName, "Error: Tool '" + toolName + "' is not available.");
                        } else {
                            try {
                                String result = executor.execute(toolCall.getArguments());
                                addToolResult(toolCall.getId(), toolName, result);
                                log.debug("Executed tool '{}' successfully", toolName);
                            } catch (Exception e) {
                                log.error("Error executing tool '{}': {}", toolName, e.getMessage());
                                addToolResult(toolCall.getId(), toolName,
                                    "Error executing tool: " + e.getMessage());
                            }
                        }
                    }
                    toolCallCount++;
                } else {
                    String content = response.getContent();
                    addAssistantMessage(content);
                    return content;
                }
            } catch (Exception e) {
                log.error("AI provider chat error: {}", e.getMessage());
                return "Error communicating with AI provider: " + e.getMessage();
            }
        }

        log.warn("Reached maximum tool call limit ({}) without final response", maxToolCalls);
        return "I've reached the maximum number of tool calls. Please refine your request.";
    }

    /**
     * Stream a response from the AI agent. Returns chunks as they arrive.
     * Tool calls are handled automatically between streaming chunks.
     */
    public void chatStream(String userMessage, Function<String, Boolean> onChunk) {
        addUserMessage(userMessage);
        int toolCallCount = 0;

        while (toolCallCount < config.getMaxToolCalls()) {
            List<Map<String, Object>> messages = buildMessageContext();

            StringBuilder accumulatedContent = new StringBuilder();
            List<AiToolCall> pendingToolCalls = new ArrayList<>();

            provider.chatStream(model, messages, config.getSystemPrompt(),
                config.getTemperature(), config.getMaxTokens(),
                buildToolDefinitions(),
                chunk -> {
                    if (chunk.getContent() != null) {
                        accumulatedContent.append(chunk.getContent());
                        return onChunk.apply(chunk.getContent());
                    }
                    if (chunk.getToolCalls() != null) {
                        pendingToolCalls.addAll(chunk.getToolCalls());
                    }
                    return true;
                });

            if (!pendingToolCalls.isEmpty()) {
                for (AiToolCall toolCall : pendingToolCalls) {
                    ToolExecutor executor = tools.get(toolCall.getName());
                    if (executor != null) {
                        try {
                            String result = executor.execute(toolCall.getArguments());
                            addToolResult(toolCall.getId(), toolCall.getName(), result);
                        } catch (Exception e) {
                            addToolResult(toolCall.getId(), toolCall.getName(),
                                "Error: " + e.getMessage());
                        }
                    }
                }
                toolCallCount++;
            } else {
                addAssistantMessage(accumulatedContent.toString());
                return;
            }
        }
    }

    /**
     * Reset the conversation history.
     */
    public void resetConversation() {
        conversationHistory.clear();
        log.info("Conversation history reset");
    }

    /**
     * Get the current conversation history (immutable view).
     */
    public List<Map<String, Object>> getConversationHistory() {
        return Collections.unmodifiableList(conversationHistory);
    }

    // ---- Private helpers ----

    private void addUserMessage(String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", content);
        conversationHistory.add(message);
    }

    private void addAssistantMessage(String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", content);
        conversationHistory.add(message);
    }

    private void addToolResult(String toolCallId, String toolName, String result) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "tool");
        message.put("tool_call_id", toolCallId);
        message.put("name", toolName);
        message.put("content", result);
        conversationHistory.add(message);
    }

    private List<Map<String, Object>> buildMessageContext() {
        int maxHistory = config.getMaxHistoryMessages();
        if (conversationHistory.size() <= maxHistory) {
            return new ArrayList<>(conversationHistory);
        }
        // Keep system context by keeping messages within limit
        return conversationHistory.subList(
            Math.max(0, conversationHistory.size() - maxHistory),
            conversationHistory.size()
        );
    }

    private List<Map<String, Object>> buildToolDefinitions() {
        return tools.entrySet().stream()
            .map(entry -> {
                ToolExecutor executor = entry.getValue();
                Map<String, Object> toolDef = new LinkedHashMap<>();
                toolDef.put("type", "function");
                Map<String, Object> function = new LinkedHashMap<>();
                function.put("name", entry.getKey());
                function.put("description", executor.getDescription());
                function.put("parameters", executor.getParameters());
                toolDef.put("function", function);
                return toolDef;
            })
            .collect(Collectors.toList());
    }
}
