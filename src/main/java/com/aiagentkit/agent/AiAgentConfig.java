package com.aiagentkit.agent;

/**
 * Configuration for the AI Agent.
 */
public class AiAgentConfig {

    private String systemPrompt = "You are a helpful AI assistant integrated into a Spring Boot application. " +
        "You have access to various tools to help answer questions and perform tasks.";
    private double temperature = 0.7;
    private int maxTokens = 4096;
    private int maxToolCalls = 10;
    private int maxHistoryMessages = 50;

    public AiAgentConfig() {}

    public AiAgentConfig(String systemPrompt, double temperature, int maxTokens,
                         int maxToolCalls, int maxHistoryMessages) {
        this.systemPrompt = systemPrompt;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.maxToolCalls = maxToolCalls;
        this.maxHistoryMessages = maxHistoryMessages;
    }

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

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String systemPrompt = "You are a helpful AI assistant.";
        private double temperature = 0.7;
        private int maxTokens = 4096;
        private int maxToolCalls = 10;
        private int maxHistoryMessages = 50;

        public Builder systemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; return this; }
        public Builder temperature(double temperature) { this.temperature = temperature; return this; }
        public Builder maxTokens(int maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder maxToolCalls(int maxToolCalls) { this.maxToolCalls = maxToolCalls; return this; }
        public Builder maxHistoryMessages(int maxHistoryMessages) { this.maxHistoryMessages = maxHistoryMessages; return this; }
        public AiAgentConfig build() {
            return new AiAgentConfig(systemPrompt, temperature, maxTokens, maxToolCalls, maxHistoryMessages);
        }
    }
}
