/**
 * MCP Server Tools Test (Streamable HTTP)
 * 
 * Tests MCP server connectivity and tool listing capabilities using the MCP Java SDK
 * with Streamable HTTP transport. This test verifies:
 * - Connection initialization to the remote MCP server
 * - Server capabilities (tools, resources, prompts)
 * - Tool listing and schema validation
 * 
 * The test uses SslHelper to bypass SSL certificate verification, allowing it to work
 * behind transparent HTTPS proxies that re-sign TLS traffic.
 * 
 * Target server: mcp-fetch server (URL from Object Repository)
 * Transport: Streamable HTTP
 * SDK Version: 0.15.0
 */

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
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

WebUI.comment("==========================================")
WebUI.comment("MCP Server Tools Test (Streamable HTTP)")
WebUI.comment("Server URL: ${mcpServerBaseUrl}${mcpEndpoint}")
WebUI.comment("MCP SDK Version: 0.15.0")
WebUI.comment("==========================================")

// Preflight: detect transparent proxy SSL interception
// Uses a short timeout since we only care about the SSL handshake, not the full response
WebUI.comment("\n[Preflight] Checking for transparent proxy SSL interception...")
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
    WebUI.comment("No transparent proxy detected — default SSL succeeded")
} catch (javax.net.ssl.SSLHandshakeException e) {
    WebUI.comment("Transparent proxy detected — SSL handshake failed (${e.message})")
} catch (java.net.http.HttpTimeoutException e) {
    WebUI.comment("No transparent proxy detected — request timed out (SSL handshake succeeded)")
} catch (java.io.IOException e) {
    // SSLHandshakeException is wrapped in IOException by HttpClient
    if (e.cause instanceof javax.net.ssl.SSLHandshakeException) {
        WebUI.comment("Transparent proxy detected — SSL handshake failed (${e.cause.message})")
    } else {
        WebUI.comment("No transparent proxy detected — connection issue (${e.class.simpleName}: ${e.message})")
    }
} catch (Exception e) {
    WebUI.comment("Preflight inconclusive (${e.class.simpleName}: ${e.message})")
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
    WebUI.comment("\n[Step 1] Initializing connection to MCP server via Streamable HTTP...")
    def initResult = mcpClient.initialize()
    WebUI.comment("Connected successfully!")
    WebUI.comment("Server Name: ${initResult.serverInfo()?.name() ?: 'Unknown'}")
    WebUI.comment("Server Version: ${initResult.serverInfo()?.version() ?: 'Unknown'}")
    WebUI.comment("Protocol Version: ${initResult.protocolVersion() ?: 'Unknown'}")

    // Verify capabilities
    WebUI.comment("\n[Step 2] Checking server capabilities...")
    def capabilities = initResult.capabilities()
    if (capabilities != null) {
        WebUI.comment("Tools supported: ${capabilities.tools() != null}")
        WebUI.comment("Resources supported: ${capabilities.resources() != null}")
        WebUI.comment("Prompts supported: ${capabilities.prompts() != null}")
    }

    // List available tools
    WebUI.comment("\n[Step 3] Fetching available tools...")
    ListToolsResult toolsResult = mcpClient.listTools()
    def tools = toolsResult.tools()

    // Verify tools are returned
    assert tools != null : "Tools list should not be null"
    assert tools.size() > 0 : "Server should return at least one tool"

    WebUI.comment("Found ${tools.size()} tool(s):")
    WebUI.comment("----------------------------------------")

    tools.each { tool ->
        WebUI.comment("\nTool: ${tool.name()}")
        WebUI.comment("  Description: ${tool.description()?.take(100) ?: 'No description'}...")

        // Check input schema if available
        def inputSchema = tool.inputSchema()
        if (inputSchema != null) {
            WebUI.comment("  Input Schema Type: ${inputSchema.type() ?: 'object'}")
            def properties = inputSchema.properties()
            if (properties != null && !properties.isEmpty()) {
                WebUI.comment("  Parameters:")
                properties.each { paramName, paramSchema ->
                    WebUI.comment("    - ${paramName}")
                }
            }
            def required = inputSchema.required()
            if (required != null && !required.isEmpty()) {
                WebUI.comment("  Required: ${required.join(', ')}")
            }
        }
    }

    WebUI.comment("\n==========================================")
    WebUI.comment("TEST PASSED: MCP Streamable HTTP server returned ${tools.size()} tool(s)")
    WebUI.comment("==========================================")

} catch (Exception e) {
    WebUI.comment("\nERROR: ${e.message}")
    e.printStackTrace()
    throw e
} finally {
    // Clean up - close the client connection
    WebUI.comment("\n[Cleanup] Closing MCP client connection...")
    try {
        mcpClient.closeGracefully()
        WebUI.comment("Connection closed successfully.")
    } catch (Exception e) {
        WebUI.comment("Warning: Error closing connection - ${e.message}")
    }
}
