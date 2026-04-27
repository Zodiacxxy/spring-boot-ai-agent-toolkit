# Spring Boot AI Agent 工具包 🚀

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)](https://www.oracle.com/java/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4-green)](https://spring.io/projects/spring-boot)

> **面向生产环境的 Spring Boot Starter，帮助你快速将 AI Agent、RAG（检索增强生成）和 MCP（模型上下文协议）工具集成到 Java 应用中。**

---

## ✨ 功能特性

- **🤖 多供应商 AI Agent** — 统一 API 支持 OpenAI（GPT-4, GPT-4o, GPT-3.5）、Anthropic（Claude 3/4）和 Ollama（本地大模型）
- **🔧 工具调用** — 将 Java 方法注册为 AI 可调用的工具，自动生成 JSON Schema 参数定义
- **📚 RAG 流水线** — 内置文档分块、向量化、存储和相似度搜索（支持内存、ChromaDB 或 PostgreSQL pgvector）
- **🔌 MCP 集成** — 连接到任意 MCP 兼容服务器，动态发现和执行工具
- **⚡ 流式响应** — 支持聊天和工具调用场景的实时流式响应
- **🎯 Spring Boot 自动配置** — 零配置即可开箱即用
- **📦 轻量模块化** — 按需使用，不必加载全部功能

---

## 🚀 快速开始

### 1. 添加依赖

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

### 2. 配置文件 `application.yml`

```yaml
aiagent:
  provider: ollama          # openai, anthropic, 或 ollama
  model: llama3.2:latest
  ollama-base-url: http://localhost:11434
```

使用 OpenAI：

```yaml
aiagent:
  provider: openai
  model: gpt-4o
  openai-api-key: ${OPENAI_API_KEY}
```

### 3. 在代码中使用 AI Agent

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

## 📖 详细使用说明

### 创建自定义工具

```java
@Component
public class WeatherTool implements ToolExecutor {

    @Override
    public String execute(String argumentsJson) {
        // 解析参数，调用天气 API，返回结果
        return "{\"temperature\": 22, \"condition\": \"sunny\"}";
    }

    @Override
    public String getDescription() {
        return "获取某个地点的当前天气";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("type", "string");
        location.put("description", "城市名称，例如：北京、上海");
        properties.put("location", location);
        params.put("properties", properties);
        params.put("required", List.of("location"));
        return params;
    }
}
```

### 注册工具

```java
@Configuration
public class AgentConfig {

    @Bean
    public AiAgent configuredAgent(AiProvider provider, WeatherTool weatherTool) {
        AiAgentConfig config = AiAgentConfig.builder()
            .systemPrompt("你是一个有用的天气助手。")
            .temperature(0.3)
            .build();

        AiAgent agent = new AiAgent(provider, "gpt-4o", config);
        agent.registerTool("get_weather", weatherTool);
        return agent;
    }
}
```

### 使用 RAG

```java
@Autowired
private RagPipeline ragPipeline;

public void indexAndQuery() {
    // 索引文档
    Document doc = new Document("doc1",
        "Spring Boot makes it easy to create stand-alone...",
        Map.of("source", "spring-docs"));
    ragPipeline.indexDocument(doc);

    // 查询
    RagPipeline.RagResult result = ragPipeline.query("如何创建 Spring Boot 应用？");
    System.out.println("上下文: " + result.getContext());
}
```

---

## 🏗️ 架构

```
┌─────────────────────────────────────────────────┐
│               你的 Spring Boot 应用               │
├─────────────────────────────────────────────────┤
│  AiAgent（编排器）                                │
│  ├── AiProvider (OpenAI / Anthropic / Ollama)    │
│  ├── ToolExecutor[] (你的 Java 工具)              │
│  ├── RagPipeline (向量化 → 存储 → 检索)           │
│  └── McpIntegration (MCP 服务器连接)              │
└─────────────────────────────────────────────────┘
```

---

## 🤝 贡献指南

欢迎各种形式的贡献！这是一个开源项目，我们感谢所有贡献：

- 🐛 通过 GitHub Issues 报告 Bug
- 💡 建议新功能或改进
- 🔧 提交 Pull Request
- 📖 改进文档
- 🌍 将 README 翻译成其他语言

---

## ☕ 支持本项目

如果这个工具包为你节省了时间或帮助了你的项目，请考虑支持它的持续开发：

**比特币 (BTC)：** `1Fxs7ZyZmEGsM39Xd1RqvP389m2xGXpPNc`

你的支持有助于维护和改进这个项目！🙏

---

## 📄 许可证

本项目基于 MIT 许可证开源，详情见 [LICENSE](LICENSE) 文件。
