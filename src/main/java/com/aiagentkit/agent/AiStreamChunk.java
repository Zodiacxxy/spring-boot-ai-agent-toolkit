package com.aiagentkit.agent;

import java.util.List;

/**
 * A chunk of a streaming AI response.
 */
public class AiStreamChunk {

    private final String content;
    private final List<AiToolCall> toolCalls;
    private final boolean done;

    public AiStreamChunk(String content, List<AiToolCall> toolCalls, boolean done) {
        this.content = content;
        this.toolCalls = toolCalls;
        this.done = done;
    }

    public String getContent() { return content; }
    public List<AiToolCall> getToolCalls() { return toolCalls; }
    public boolean isDone() { return done; }
}
