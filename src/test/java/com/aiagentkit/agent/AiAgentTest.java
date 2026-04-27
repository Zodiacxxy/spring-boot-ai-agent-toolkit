package com.aiagentkit.agent;

import com.aiagentkit.rag.Document;
import com.aiagentkit.rag.DocumentChunker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiAgentTest {

    @Test
    void testAgentCreation() {
        AiAgentConfig config = AiAgentConfig.builder()
            .systemPrompt("You are a test agent.")
            .temperature(0.5)
            .maxTokens(256)
            .build();

        // Using Ollama for testing (no API key needed)
        OllamaProvider provider = new OllamaProvider("http://localhost:11434");
        AiAgent agent = new AiAgent(provider, "llama3.2:latest", config);

        assertNotNull(agent);
        assertTrue(agent.getRegisteredTools().isEmpty());
    }

    @Test
    void testToolRegistration() {
        AiAgentConfig config = AiAgentConfig.builder().build();
        OllamaProvider provider = new OllamaProvider("http://localhost:11434");
        AiAgent agent = new AiAgent(provider, "llama3.2:latest", config);

        agent.registerTool("echo", new ToolExecutor() {
            @Override
            public String execute(String argumentsJson) {
                return argumentsJson;
            }

            @Override
            public String getDescription() {
                return "Echo the input back";
            }

            @Override
            public java.util.Map<String, Object> getParameters() {
                java.util.Map<String, Object> params = new java.util.LinkedHashMap<>();
                params.put("type", "object");
                java.util.Map<String, Object> properties = new java.util.LinkedHashMap<>();
                java.util.Map<String, Object> message = new java.util.LinkedHashMap<>();
                message.put("type", "string");
                message.put("description", "The message to echo");
                properties.put("message", message);
                params.put("properties", properties);
                params.put("required", java.util.List.of("message"));
                return params;
            }
        });

        assertEquals(1, agent.getRegisteredTools().size());
        assertTrue(agent.getRegisteredTools().contains("echo"));
    }

    @Test
    void testResetConversation() {
        AiAgentConfig config = AiAgentConfig.builder().build();
        OllamaProvider provider = new OllamaProvider("http://localhost:11434");
        AiAgent agent = new AiAgent(provider, "llama3.2:latest", config);

        agent.chat("Hello");
        assertTrue(agent.getConversationHistory().size() > 0);

        agent.resetConversation();
        assertTrue(agent.getConversationHistory().isEmpty());
    }

    @Test
    void testDocumentChunking() {
        DocumentChunker chunker = new DocumentChunker(50, 10);
        Document doc = new Document("test1",
            "This is a test document that should be split into multiple chunks. " +
            "Each chunk should contain approximately 50 characters. " +
            "The overlap between chunks should be 10 characters. " +
            "This ensures that context is preserved across chunk boundaries.");

        List<Document> chunks = chunker.chunkByCharacterCount(doc);

        assertTrue(chunks.size() > 1);
        assertEquals("test1_chunk_0", chunks.get(0).getId());
        assertTrue(chunks.get(0).getMetadata().containsKey("chunk_index"));
    }
}
