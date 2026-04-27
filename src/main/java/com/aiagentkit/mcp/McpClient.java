package com.aiagentkit.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP (Model Context Protocol) client for interacting with MCP servers.
 * <p>
 * Supports stdio-based MCP transport, allowing the agent to dynamically
 * discover and call tools from any MCP-compatible server.
 */
public class McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);

    private final String serverCommand;
    private final List<String> serverArgs;
    private final Map<String, Object> serverEnv;
    private final ObjectMapper objectMapper;

    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;
    private int requestId = 0;
    private boolean connected = false;

    // Cached tool definitions
    private List<McpToolDefinition> cachedTools;

    public McpClient(String serverCommand, List<String> serverArgs) {
        this(serverCommand, serverArgs, new HashMap<>());
    }

    public McpClient(String serverCommand, List<String> serverArgs, Map<String, Object> serverEnv) {
        this.serverCommand = serverCommand;
        this.serverArgs = serverArgs;
        this.serverEnv = serverEnv;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Connect to the MCP server by starting the process.
     */
    public void connect() throws IOException {
        List<String> command = new ArrayList<>();
        command.add(serverCommand);
        command.addAll(serverArgs);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().putAll(System.getenv());
        for (Map.Entry<String, Object> entry : serverEnv.entrySet()) {
            pb.environment().put(entry.getKey(), String.valueOf(entry.getValue()));
        }

        this.process = pb.start();
        this.reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(
            new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        // Send initialize request
        sendInitialize();
        this.connected = true;
        log.info("Connected to MCP server: {}", serverCommand);
    }

    /**
     * List available tools from the MCP server.
     */
    public List<McpToolDefinition> listTools() throws IOException {
        if (cachedTools != null) {
            return cachedTools;
        }

        Map<String, Object> response = sendRequest("tools/list", new HashMap<>());
        JsonNode result = objectMapper.valueToTree(response);
        JsonNode tools = result.get("tools");

        List<McpToolDefinition> toolDefs = new ArrayList<>();
        if (tools != null && tools.isArray()) {
            for (JsonNode tool : tools) {
                String name = tool.get("name").asText();
                String description = tool.has("description") ? tool.get("description").asText() : "";
                Map<String, Object> inputSchema = new HashMap<>();
                if (tool.has("inputSchema")) {
                    inputSchema = objectMapper.convertValue(
                        tool.get("inputSchema"), Map.class);
                }
                toolDefs.add(new McpToolDefinition(name, description, inputSchema));
            }
        }

        this.cachedTools = toolDefs;
        log.info("Discovered {} tools from MCP server", toolDefs.size());
        return toolDefs;
    }

    /**
     * Call a tool on the MCP server.
     */
    public String callTool(String toolName, Map<String, Object> arguments) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);

        Map<String, Object> response = sendRequest("tools/call", params);
        JsonNode result = objectMapper.valueToTree(response);

        StringBuilder content = new StringBuilder();
        JsonNode contentArray = result.get("content");
        if (contentArray != null && contentArray.isArray()) {
            for (JsonNode item : contentArray) {
                if (item.has("text")) {
                    content.append(item.get("text").asText());
                }
            }
        }

        log.debug("Called MCP tool '{}' successfully", toolName);
        return content.toString();
    }

    /**
     * Disconnect from the MCP server.
     */
    public void disconnect() {
        this.connected = false;
        this.cachedTools = null;
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (process != null) {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            log.warn("Error disconnecting MCP client: {}", e.getMessage());
        }
        log.info("Disconnected from MCP server");
    }

    public boolean isConnected() { return connected; }

    // ---- Private helpers ----

    private void sendInitialize() throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "0.1.0");
        Map<String, Object> clientInfo = new HashMap<>();
        clientInfo.put("name", "spring-boot-ai-agent-toolkit");
        clientInfo.put("version", "1.0.0");
        params.put("capabilities", new HashMap<>());
        params.put("clientInfo", clientInfo);

        sendRequest("initialize", params);
    }

    private Map<String, Object> sendRequest(String method, Map<String, Object> params)
            throws IOException {
        int id = ++requestId;

        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.put("jsonrpc", "2.0");
        requestJson.put("id", id);
        requestJson.put("method", method);
        requestJson.set("params", objectMapper.valueToTree(params));

        String request = objectMapper.writeValueAsString(requestJson);
        writer.write(request);
        writer.newLine();
        writer.flush();

        String responseLine = reader.readLine();
        if (responseLine == null) {
            throw new IOException("MCP server closed connection");
        }

        JsonNode responseJson = objectMapper.readTree(responseLine);
        JsonNode errorNode = responseJson.get("error");
        if (errorNode != null && !errorNode.isNull()) {
            throw new IOException("MCP error: " + errorNode.get("message").asText());
        }

        JsonNode resultNode = responseJson.get("result");
        if (resultNode == null || resultNode.isNull()) {
            return new HashMap<>();
        }

        return objectMapper.convertValue(resultNode, Map.class);
    }
}
