package com.aiagentkit.agent;

import java.util.Map;

/**
 * Interface for tools that an AI agent can execute.
 * Implement this interface to create custom tools for the agent.
 */
public interface ToolExecutor {

    /**
     * Execute the tool with the given JSON arguments.
     *
     * @param argumentsJson JSON string of the tool arguments
     * @return the result of the tool execution as a string
     */
    String execute(String argumentsJson);

    /**
     * Get a human-readable description of what this tool does.
     * Used by the AI model to understand when to call this tool.
     */
    String getDescription();

    /**
     * Get the JSON Schema for this tool's parameters.
     * Used by the AI model to know what arguments to provide.
     */
    Map<String, Object> getParameters();
}
