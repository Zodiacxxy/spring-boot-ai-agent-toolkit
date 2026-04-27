---
title: "Introducing Spring Boot AI Agent Toolkit: Build AI-Powered Java Apps in Minutes"
published: false
description: "A production-ready Spring Boot Starter for integrating AI agents, RAG pipelines, and MCP tools into Java applications. OpenAI, Anthropic, Ollama support."
tags: [java, springboot, ai, rag, opensource]
---

Today I'm excited to announce the first release of **Spring Boot AI Agent Toolkit** — a production-ready Spring Boot Starter that makes it trivial to integrate AI agents, RAG pipelines, and MCP tools into your Java applications.

## Why?

Java powers the majority of enterprise backend systems. But for AI integration, Java developers have been underserved compared to the Python/JavaScript ecosystem. This toolkit bridges that gap.

**With this toolkit you can:**
- Add an AI chat agent to any Spring Boot app in 3 lines of code
- Let AI models call your Java methods as tools (no Python microservices needed)
- Add RAG (Retrieval-Augmented Generation) for document-based Q&A
- Connect to any MCP-compatible server for dynamic tool discovery

## Key Features

### Multi-Provider AI Agent
Switch between **OpenAI (GPT-4, GPT-4o)**, **Anthropic (Claude 3/4)**, or **Ollama (local LLMs)** with a single line of config.

### Tool Calling
Register any Java method as an AI-callable tool with automatic JSON Schema parameters.

### RAG Pipeline
Built-in support for document chunking, embedding, vector storage, and cosine similarity search — no external vector database required for dev.

### MCP Protocol Support
Connect to any MCP-compatible server for dynamic tool discovery and execution.

## Quick Start

**Step 1:** Add dependency:
```kotlin
implementation("com.aiagentkit:spring-boot-ai-agent-toolkit:1.0.0")
```

**Step 2:** Configure:
```yaml
aiagent:
  provider: ollama
  model: llama3.2:latest
```

**Step 3:** Use it:
```java
@RestController
public class ChatController {
    @Autowired private AiAgent aiAgent;

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        return aiAgent.chat(message);
    }
}
```

That's it! Your Spring Boot app now has an AI agent.

## Get Involved

- ⭐ **Star on GitHub**: https://github.com/Zodiacxxy/spring-boot-ai-agent-toolkit
- 🌐 **Landing Page**: https://zodiacxxy.github.io/spring-boot-ai-agent-toolkit/
- 🤝 **Contributions Welcome**: PRs, docs, translations

## Support

If this project helps you, consider supporting its continued development:

**BTC:** `1Fxs7ZyZmEGsM39Xd1RqvP389m2xGXpPNc`

---

*MIT Licensed • Built with ❤️ for the Java & AI community*
