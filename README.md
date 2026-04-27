# Spring Boot AI Agent Toolkit 🚀

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)](https://www.oracle.com/java/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4-green)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-lightgrey)](https://gradle.org/)

> **A production-ready Spring Boot Starter for integrating AI agents, RAG (Retrieval-Augmented Generation) pipelines, and MCP (Model Context Protocol) tools into your Java applications.**

---

## ✨ Features

- **🤖 Multi-Provider AI Agent** — Supports OpenAI (GPT-4, GPT-4o, GPT-3.5), Anthropic (Claude 3/4), and Ollama (local LLMs via a single unified API)
- **🔧 Tool Calling** — Register Java methods as AI-callable tools with JSON Schema parameter definitions
- **📚 RAG Pipeline** — Built-in document chunking, embedding, vector storage, and similarity search (InMemory, ChromaDB, or PostgreSQL pgvector)
- **🔌 MCP Integration** — Connect to any MCP-compatible server for dynamic tool discovery and execution
- **⚡ Streaming Support** — Real-time response streaming for both chat and tool-calling scenarios
- **🎯 Spring Boot Auto-Configuration** — Zero-config setup with sensible defaults
- **📦 Lightweight & Modular** — Use what you need, leave what you don't

---

## 🚀 Quick Start

### 1. Add the dependency

```kotlin
// build.gradle.kts
implementation("com.aiagentkit:spring-boot-ai-agent-toolkit:1.0.0")
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.aiagentkit</groupId>
    <artifactId>spring-boot-ai-agent-toolkit</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configure in `application.yml`

```yaml
aiagent:
  provider: ollama          # openai, anthropic, or ollama
  model: llama3.2:latest
  ollama-base-url: http://localhost:11434
```

Or for OpenAI:

```yaml
aiagent:
  provider: openai
  model: gpt-4o
  openai-api-key: ${OPENAI_API_KEY}
```

### 3. Use the AI Agent in your code

```java
@RestController
public class ChatController {

    @Autowired
    private AiAgent aiAgent;

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        return aiAgent.chat(message);
    }
}
```

---

## 📖 Detailed Usage

### Creating a Custom Tool

```java
@Component
public class WeatherTool implements ToolExecutor {

    @Override
    public String execute(String argumentsJson) {
        // Parse arguments, call weather API, return result
        return "{\"temperature\": 22, \"condition\": \"sunny\"}";
    }

    @Override
    public String getDescription() {
        return "Get the current weather for a location";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("type", "string");
        location.put("description", "City name, e.g. Beijing, New York");
        properties.put("location", location);
        params.put("properties", properties);
        params.put("required", List.of("location"));
        return params;
    }
}
```

### Registering Tools

```java
@Configuration
public class AgentConfig {

    @Bean
    public AiAgent configuredAgent(AiProvider provider, WeatherTool weatherTool) {
        AiAgentConfig config = AiAgentConfig.builder()
            .systemPrompt("You are a helpful weather assistant.")
            .temperature(0.3)
            .build();

        AiAgent agent = new AiAgent(provider, "gpt-4o", config);
        agent.registerTool("get_weather", weatherTool);
        return agent;
    }
}
```

### Using RAG

```java
@Autowired
private RagPipeline ragPipeline;

public void indexAndQuery() {
    // Index a document
    Document doc = new Document("doc1", "Spring Boot makes it easy to create stand-alone...",
        Map.of("source", "spring-docs"));
    ragPipeline.indexDocument(doc);

    // Query
    RagPipeline.RagResult result = ragPipeline.query("How do I create a Spring Boot application?");
    System.out.println("Context: " + result.getContext());
}
```

### MCP Server Integration

```yaml
aiagent:
  mcp-servers:
    - name: filesystem
      command: npx
      args:
        - -y
        - "@modelcontextprotocol/server-filesystem"
        - /tmp
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────┐
│              Your Spring Boot App                │
├─────────────────────────────────────────────────┤
│  AiAgent (Orchestrator)                         │
│  ├── AiProvider (OpenAI / Anthropic / Ollama)    │
│  ├── ToolExecutor[] (Your Java Tools)           │
│  ├── RagPipeline (Embed → Store → Retrieve)      │
│  └── McpIntegration (MCP Server Connection)     │
└─────────────────────────────────────────────────┘
```

---

## 🧪 Running Tests

```bash
./gradlew test
```

For integration tests with Ollama:

```bash
ollama pull llama3.2:latest
./gradlew test -Dspring.profiles.active=local
```

---

## 🤝 Contributing

Contributions are welcome! This is an open-source project and we appreciate all forms of contribution:

- 🐛 Report bugs via GitHub Issues
- 💡 Suggest features or improvements
- 🔧 Submit Pull Requests
- 📖 Improve documentation
- 🌍 Translate README into other languages

---

## ☕ Support This Project

If this toolkit saves you time or helps your project, consider supporting its development:

**Bitcoin (BTC):** `1Fxs7ZyZmEGsM39Xd1RqvP389m2xGXpPNc`

Your support helps maintain and improve this project! 🙏

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 📬 Contact

- GitHub Issues: [https://github.com/liufeng/spring-boot-ai-agent-toolkit/issues](https://github.com/liufeng/spring-boot-ai-agent-toolkit/issues)
- Email: liufeng@gmail.com
