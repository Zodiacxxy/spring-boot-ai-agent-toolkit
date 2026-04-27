package com.aiagentkit.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages MCP server connections and integrates them with the AI Agent.
 * Supports multiple MCP servers, automatic tool discovery, and lifecycle management.
 */
public class McpIntegrationConfig implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(McpIntegrationConfig.class);

    private final List<McpServerConfig> serverConfigs;
    private final Map<String, McpClient> clients;
    private volatile boolean running = false;

    public McpIntegrationConfig(List<McpServerConfig> serverConfigs) {
        this.serverConfigs = serverConfigs;
        this.clients = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
        for (McpServerConfig config : serverConfigs) {
            try {
                McpClient client = new McpClient(
                    config.getCommand(),
                    config.getArgs(),
                    config.getEnv()
                );
                client.connect();
                clients.put(config.getName(), client);
                log.info("Started MCP server '{}'", config.getName());
            } catch (Exception e) {
                log.error("Failed to start MCP server '{}': {}", config.getName(), e.getMessage());
            }
        }
        running = true;
    }

    @Override
    public void stop() {
        for (Map.Entry<String, McpClient> entry : clients.entrySet()) {
            try {
                entry.getValue().disconnect();
                log.info("Stopped MCP server '{}'", entry.getKey());
            } catch (Exception e) {
                log.warn("Error stopping MCP server '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        clients.clear();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Get all discovered tools from all MCP servers.
     */
    public List<McpToolDefinition> getAllTools() {
        List<McpToolDefinition> allTools = new ArrayList<>();
        for (Map.Entry<String, McpClient> entry : clients.entrySet()) {
            try {
                allTools.addAll(entry.getValue().listTools());
            } catch (Exception e) {
                log.warn("Failed to list tools from '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        return allTools;
    }

    /**
     * Call a tool from any connected MCP server.
     */
    public String callTool(String toolName, Map<String, Object> arguments) throws IOException {
        for (Map.Entry<String, McpClient> entry : clients.entrySet()) {
            try {
                List<McpToolDefinition> tools = entry.getValue().listTools();
                boolean hasTool = tools.stream()
                    .anyMatch(t -> t.getName().equals(toolName));
                if (hasTool) {
                    return entry.getValue().callTool(toolName, arguments);
                }
            } catch (Exception e) {
                log.debug("Server '{}' doesn't have tool '{}': {}",
                    entry.getKey(), toolName, e.getMessage());
            }
        }
        throw new IllegalArgumentException("Tool '" + toolName + "' not found on any MCP server");
    }

    public McpClient getClient(String name) {
        return clients.get(name);
    }

    public Set<String> getConnectedServers() {
        return clients.keySet();
    }

    /**
     * Configuration for a single MCP server.
     */
    public static class McpServerConfig {
        private String name;
        private String command;
        private List<String> args = new ArrayList<>();
        private Map<String, Object> env = new HashMap<>();

        public McpServerConfig() {}

        public McpServerConfig(String name, String command, List<String> args) {
            this.name = name;
            this.command = command;
            this.args = args;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public List<String> getArgs() { return args; }
        public void setArgs(List<String> args) { this.args = args; }
        public Map<String, Object> getEnv() { return env; }
        public void setEnv(Map<String, Object> env) { this.env = env; }
    }
}
