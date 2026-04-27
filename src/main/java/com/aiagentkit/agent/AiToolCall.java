package com.aiagentkit.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a tool call made by the AI model.
 */
public class AiToolCall {

    private final String id;
    private final String name;
    private final String arguments;

    public AiToolCall(String id, String name, String arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getArguments() { return arguments; }
}
