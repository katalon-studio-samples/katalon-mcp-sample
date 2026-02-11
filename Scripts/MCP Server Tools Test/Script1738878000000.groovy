import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.katalon.mcp.utils.SslHelper
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// Test configuration - mcp-fetch server (Streamable HTTP)
// URL centralized in Object Repository for single-point configuration
String fullUrl = findTestObject('Fetch MCP').getRestUrl()
URI parsedUri = URI.create(fullUrl)
String mcpServerBaseUrl = "${parsedUri.scheme}://${parsedUri.host}" + (parsedUri.port > 0 ? ":${parsedUri.port}" : "")
String mcpEndpoint = parsedUri.path

println "=========================================="
println "MCP Server Tools Test (Streamable HTTP)"
println "Server URL: ${mcpServerBaseUrl}${mcpEndpoint}"
println "MCP SDK Version: 0.15.0"
println "=========================================="

// Preflight: detect transparent proxy SSL interception
// Uses a short timeout since we only care about the SSL handshake, not the full response
println "\n[Preflight] Checking for transparent proxy SSL interception..."
try {
    java.net.http.HttpClient.newBuilder().build()
        .send(
            HttpRequest.newBuilder()
                .uri(URI.create(mcpServerBaseUrl + mcpEndpoint))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
    println "No transparent proxy detected — default SSL succeeded"
} catch (javax.net.ssl.SSLHandshakeException e) {
    println "Transparent proxy detected — SSL handshake failed (${e.message})"
} catch (java.net.http.HttpTimeoutException e) {
    println "No transparent proxy detected — request timed out (SSL handshake succeeded)"
} catch (java.io.IOException e) {
    // SSLHandshakeException is wrapped in IOException by HttpClient
    if (e.cause instanceof javax.net.ssl.SSLHandshakeException) {
        println "Transparent proxy detected — SSL handshake failed (${e.cause.message})"
    } else {
        println "No transparent proxy detected — connection issue (${e.class.simpleName}: ${e.message})"
    }
} catch (Exception e) {
    println "Preflight inconclusive (${e.class.simpleName}: ${e.message})"
}

// Create the Streamable HTTP transport for the remote MCP server
// Use trust-all SSL to work behind transparent HTTPS proxies
def transport = HttpClientStreamableHttpTransport.builder(mcpServerBaseUrl)
    .endpoint(mcpEndpoint)
    .clientBuilder(SslHelper.createTrustAllClientBuilder())
    .build()

// Create synchronous MCP client
McpSyncClient mcpClient = McpClient.sync(transport)
    .requestTimeout(Duration.ofSeconds(60))
    .build()

try {
    // Initialize the connection to the MCP server
    println "\n[Step 1] Initializing connection to MCP server via Streamable HTTP..."
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

        // Check input schema if available
        def inputSchema = tool.inputSchema()
        if (inputSchema != null) {
            println "  Input Schema Type: ${inputSchema.type() ?: 'object'}"
            def properties = inputSchema.properties()
            if (properties != null && !properties.isEmpty()) {
                println "  Parameters:"
                properties.each { paramName, paramSchema ->
                    println "    - ${paramName}"
                }
            }
            def required = inputSchema.required()
            if (required != null && !required.isEmpty()) {
                println "  Required: ${required.join(', ')}"
            }
        }
    }

    println "\n=========================================="
    println "TEST PASSED: MCP Streamable HTTP server returned ${tools.size()} tool(s)"
    println "=========================================="

} catch (Exception e) {
    println "\nERROR: ${e.message}"
    e.printStackTrace()
    throw e
} finally {
    // Clean up - close the client connection
    println "\n[Cleanup] Closing MCP client connection..."
    try {
        mcpClient.closeGracefully()
        println "Connection closed successfully."
    } catch (Exception e) {
        println "Warning: Error closing connection - ${e.message}"
    }
}
