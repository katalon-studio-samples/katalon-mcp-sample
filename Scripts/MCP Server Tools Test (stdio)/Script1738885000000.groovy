/**
 * Script: MCP Server Tools Test (stdio)
 * 
 * Description:
 *   Tests a local MCP server using the MCP SDK with stdio transport.
 * 
 * Test Approach:
 *   MCP Java SDK - Uses official MCP SDK 0.15.0 with stdio transport for local servers.
 * 
 * Target Server:
 *   mcp-filesystem-server (local Go server)
 * 
 * Prerequisites:
 *   - MCP SDK installed: ./gradlew clean katalonCopyDependencies
 *   - Java 17
 *   - Go server installed: go install github.com/mark3labs/mcp-filesystem-server@latest
 * 
 * Test Steps:
 *   1. Create stdio transport with StdioClientTransport
 *   2. Initialize connection to local server process
 *   3. List available tools
 *   4. Validate tool schemas
 *   5. Clean up server process
 */

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.ServerParameters
import io.modelcontextprotocol.client.transport.StdioClientTransport
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult
import com.fasterxml.jackson.databind.ObjectMapper

import java.time.Duration

// Server configuration
// Default: Go install location. Override via environment variable MCP_FILESYSTEM_SERVER_PATH
String goPath = System.getenv("GOPATH") ?: "${System.getProperty('user.home')}/go"
String serverPath = System.getenv("MCP_FILESYSTEM_SERVER_PATH") ?: "${goPath}/bin/mcp-filesystem-server"

// Directory to allow the filesystem server to access (use project directory)
String allowedDirectory = System.getProperty("user.dir")

println "=========================================="
println "MCP Server Tools Test (stdio Transport)"
println "Server: ${serverPath}"
println "Allowed Directory: ${allowedDirectory}"
println "MCP SDK Version: 0.15.0"
println "=========================================="

// Verify server exists
def serverFile = new File(serverPath)
if (!serverFile.exists()) {
    throw new RuntimeException("MCP filesystem server not found at: ${serverPath}\n" +
        "Install with: go install github.com/mark3labs/mcp-filesystem-server@latest")
}

// Create server parameters for stdio transport
def serverParams = ServerParameters.builder(serverPath)
    .args([allowedDirectory])
    .build()

// Create the stdio transport with JSON mapper
def objectMapper = new ObjectMapper()
def jsonMapper = new JacksonMcpJsonMapper(objectMapper)
def transport = new StdioClientTransport(serverParams, jsonMapper)

// Create synchronous MCP client
McpSyncClient mcpClient = McpClient.sync(transport)
    .requestTimeout(Duration.ofSeconds(30))
    .build()

try {
    // Initialize the connection to the MCP server
    println "\n[Step 1] Initializing connection to MCP server via stdio..."
    def initResult = mcpClient.initialize()
    println "Connected successfully!"
    println "Server Name: ${initResult.serverInfo()?.name() ?: 'Unknown'}"
    println "Server Version: ${initResult.serverInfo()?.version() ?: 'Unknown'}"
    println "Protocol Version: ${initResult.protocolVersion() ?: 'Unknown'}"

    // Verify capabilities
    println "\n[Step 2] Checking server capabilities..."
    def capabilities = initResult.capabilities()
    if (capabilities != null) {
        println "Tools supported: ${capabilities.tools() != null}"
        println "Resources supported: ${capabilities.resources() != null}"
        println "Prompts supported: ${capabilities.prompts() != null}"
    }

    // List available tools
    println "\n[Step 3] Fetching available tools..."
    ListToolsResult toolsResult = mcpClient.listTools()
    def tools = toolsResult.tools()

    // Verify tools are returned
    assert tools != null : "Tools list should not be null"
    assert tools.size() > 0 : "Server should return at least one tool"

    println "Found ${tools.size()} tool(s):"
    println "----------------------------------------"

    tools.each { tool ->
        println "\nTool: ${tool.name()}"
        println "  Description: ${tool.description()?.take(100) ?: 'No description'}..."

        def inputSchema = tool.inputSchema()
        if (inputSchema != null) {
            def properties = inputSchema.properties()
            if (properties != null && !properties.isEmpty()) {
                println "  Parameters: ${properties.keySet().join(', ')}"
            }
            def required = inputSchema.required()
            if (required != null && !required.isEmpty()) {
                println "  Required: ${required.join(', ')}"
            }
        }
    }

    println "\n=========================================="
    println "TEST PASSED: MCP stdio server returned ${tools.size()} tool(s)"
    println "=========================================="

} catch (Exception e) {
    println "\nERROR: ${e.message}"
    e.printStackTrace()
    throw e
} finally {
    println "\n[Cleanup] Closing MCP client connection..."
    try {
        mcpClient.closeGracefully()
        println "Connection closed successfully."
    } catch (Exception e) {
        println "Warning: Error closing connection - ${e.message}"
    }
}
