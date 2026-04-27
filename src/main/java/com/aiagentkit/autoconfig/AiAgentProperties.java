package com.aiagentkit.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the Spring Boot AI Agent Toolkit.
 * <p>
 * Prefix: aiagent
 */
@ConfigurationProperties(prefix = "aiagent")
public class AiAgentProperties {

    /** The AI provider to use: openai, anthropic, ollama */
    private String provider = "ollama";

    /** The model name to use */
    private String model = "gpt-4o";

    /** OpenAI API key */
    private String openaiApiKey = "";

    /** OpenAI base URL (supports custom endpoints like Azure OpenAI) */
    private String openaiBaseUrl = "https://api.openai.com/v1";

    /** Anthropic API key */
    private String anthropicApiKey = "";

    /** Anthropic API base URL */
    private String anthropicBaseUrl = "https://api.anthropic.com/v1";

    /** Ollama base URL */
    private String ollamaBaseUrl = "http://localhost:11434";

    /** Agent configuration */
    private Agent agent = new Agent();

    /** RAG configuration */
    private Rag rag = new Rag();

    /** MCP server configurations */
    private List<McpServer> mcpServers = new ArrayList<>();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String openaiApiKey) { this.openaiApiKey = openaiApiKey; }
    public String getOpenaiBaseUrl() { return openaiBaseUrl; }
    public void setOpenaiBaseUrl(String openaiBaseUrl) { this.openaiBaseUrl = openaiBaseUrl; }
    public String getAnthropicApiKey() { return anthropicApiKey; }
    public void setAnthropicApiKey(String anthropicApiKey) { this.anthropicApiKey = anthropicApiKey; }
    public String getAnthropicBaseUrl() { return anthropicBaseUrl; }
    public void setAnthropicBaseUrl(String anthropicBaseUrl) { this.anthropicBaseUrl = anthropicBaseUrl; }
    public String getOllamaBaseUrl() { return ollamaBaseUrl; }
    public void setOllamaBaseUrl(String ollamaBaseUrl) { this.ollamaBaseUrl = ollamaBaseUrl; }
    public Agent getAgent() { return agent; }
    public void setAgent(Agent agent) { this.agent = agent; }
    public Rag getRag() { return rag; }
    public void setRag(Rag rag) { this.rag = rag; }
    public List<McpServer> getMcpServers() { return mcpServers; }
    public void setMcpServers(List<McpServer> mcpServers) { this.mcpServers = mcpServers; }

    public static class Agent {
        private String systemPrompt = "You are a helpful AI assistant integrated into a Spring Boot application.";
        private double temperature = 0.7;
        private int maxTokens = 4096;
        private int maxToolCalls = 10;
        private int maxHistoryMessages = 50;

        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public int getMaxToolCalls() { return maxToolCalls; }
        public void setMaxToolCalls(int maxToolCalls) { this.maxToolCalls = maxToolCalls; }
        public int getMaxHistoryMessages() { return maxHistoryMessages; }
        public void setMaxHistoryMessages(int maxHistoryMessages) { this.maxHistoryMessages = maxHistoryMessages; }
    }

    public static class Rag {
        private boolean enabled = true;
        private int chunkSize = 500;
        private int chunkOverlap = 50;
        private int topK = 5;
        private String embeddingModel = "text-embedding-3-small";
        private int embeddingDimension = 1536;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
        public int getChunkOverlap() { return chunkOverlap; }
        public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
        public int getEmbeddingDimension() { return embeddingDimension; }
        public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
    }

    public static class McpServer {
        private String name;
        private String command;
        private List<String> args = new ArrayList<>();
        private Map<String, String> env = new HashMap<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public List<String> getArgs() { return args; }
        public void setArgs(List<String> args) { this.args = args; }
        public Map<String, String> getEnv() { return env; }
        public void setEnv(Map<String, String> env) { this.env = env; }
    }
}
