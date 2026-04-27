package com.aiagentkit.autoconfig;

import com.aiagentkit.agent.*;
import com.aiagentkit.mcp.McpClient;
import com.aiagentkit.mcp.McpIntegrationConfig;
import com.aiagentkit.rag.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Auto-configuration for the Spring Boot AI Agent Toolkit.
 * Automatically wires together AI providers, RAG pipelines, and MCP integrations.
 */
@AutoConfiguration
@EnableConfigurationProperties(AiAgentProperties.class)
public class AiAgentAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiAgentAutoConfiguration.class);

    private final AiAgentProperties properties;

    public AiAgentAutoConfiguration(AiAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates the appropriate AI provider based on configuration.
     */
    @Bean
    @ConditionalOnMissingBean
    public AiProvider aiProvider() {
        AiProvider provider;

        switch (properties.getProvider().toLowerCase()) {
            case "openai":
                provider = new OpenAiProvider(
                    properties.getOpenaiApiKey(),
                    properties.getOpenaiBaseUrl()
                );
                break;
            case "anthropic":
                provider = new AnthropicProvider(
                    properties.getAnthropicApiKey(),
                    properties.getAnthropicBaseUrl()
                );
                break;
            case "ollama":
                provider = new OllamaProvider(properties.getOllamaBaseUrl());
                break;
            default:
                log.warn("Unknown provider '{}', falling back to Ollama", properties.getProvider());
                provider = new OllamaProvider(properties.getOllamaBaseUrl());
        }

        log.info("Created AI provider: {} (model: {})",
            provider.getProviderName(), properties.getModel());

        return provider;
    }

    /**
     * Creates the AI Agent with configured settings.
     */
    @Bean
    @ConditionalOnMissingBean
    public AiAgent aiAgent(AiProvider aiProvider) {
        AiAgentConfig config = AiAgentConfig.builder()
            .systemPrompt(properties.getAgent().getSystemPrompt())
            .temperature(properties.getAgent().getTemperature())
            .maxTokens(properties.getAgent().getMaxTokens())
            .maxToolCalls(properties.getAgent().getMaxToolCalls())
            .maxHistoryMessages(properties.getAgent().getMaxHistoryMessages())
            .build();

        return new AiAgent(aiProvider, properties.getModel(), config);
    }

    /**
     * Creates the RAG pipeline with vector store, embedding service, and chunker.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "aiagent.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RagPipeline ragPipeline(EmbeddingService embeddingService) {
        DocumentChunker chunker = new DocumentChunker(
            properties.getRag().getChunkSize(),
            properties.getRag().getChunkOverlap()
        );

        InMemoryVectorStore vectorStore = new InMemoryVectorStore();

        return new RagPipeline(chunker, embeddingService, vectorStore,
            properties.getRag().getTopK());
    }

    /**
     * Creates the embedding service.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "aiagent.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
    public EmbeddingService embeddingService() {
        String apiKey = "openai".equals(properties.getProvider())
            ? properties.getOpenaiApiKey() : "";

        return new OpenAiEmbeddingService(
            apiKey,
            properties.getOpenaiBaseUrl(),
            properties.getRag().getEmbeddingModel(),
            properties.getRag().getEmbeddingDimension()
        );
    }

    /**
     * Creates MCP integration with configured servers.
     */
    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "aiagent.mcp-servers", name = "enabled", havingValue = "true", matchIfMissing = false)
    public McpIntegrationConfig mcpIntegration() {
        List<McpIntegrationConfig.McpServerConfig> serverConfigs =
            properties.getMcpServers().stream()
                .map(s -> {
                    McpIntegrationConfig.McpServerConfig config =
                        new McpIntegrationConfig.McpServerConfig(
                            s.getName(), s.getCommand(), s.getArgs()
                        );
                    config.getEnv().putAll(s.getEnv());
                    return config;
                })
                .collect(Collectors.toList());

        return new McpIntegrationConfig(serverConfigs);
    }
}
