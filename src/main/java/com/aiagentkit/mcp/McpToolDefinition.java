package com.aiagentkit.mcp;

import java.util.*;

/**
 * Definition of an MCP (Model Context Protocol) tool.
 */
public class McpToolDefinition {

    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;

    public McpToolDefinition(String name, String description, Map<String, Object> inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Map<String, Object> getInputSchema() { return inputSchema; }
}
