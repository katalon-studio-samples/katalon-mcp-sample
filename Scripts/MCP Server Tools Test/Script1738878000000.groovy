import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testng.keyword.TestNGBuiltinKeywords as TestNGKW
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.Keys as Keys

// MCP SDK imports
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Duration

// Test configuration
String mcpServerBaseUrl = "https://remote.mcpservers.org"
String mcpEndpoint = "/fetch/mcp"

println "=========================================="
println "MCP Server Tools Test"
println "Server URL: ${mcpServerBaseUrl}${mcpEndpoint}"
println "=========================================="

// Create HTTP request builder with proper headers
def requestBuilder = HttpRequest.newBuilder()
    .header("Accept", "application/json, text/event-stream")
    .timeout(Duration.ofSeconds(60))

// Create HTTP client with timeouts
def clientBuilder = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(30))

// Create the Streamable HTTP transport for the remote MCP server
def transport = HttpClientStreamableHttpTransport.builder(mcpServerBaseUrl)
    .endpoint(mcpEndpoint)
    .clientBuilder(clientBuilder)
    .requestBuilder(requestBuilder)
    .build()

// Create synchronous MCP client
McpSyncClient mcpClient = McpClient.sync(transport)
    .requestTimeout(Duration.ofSeconds(60))
    .build()

try {
    // Initialize the connection to the MCP server
    println "\n[Step 1] Initializing connection to MCP server..."
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
        println "  Description: ${tool.description() ?: 'No description'}"

        // Check input schema if available
        def inputSchema = tool.inputSchema()
        if (inputSchema != null) {
            println "  Input Schema Type: ${inputSchema.type() ?: 'object'}"
            def properties = inputSchema.properties()
            if (properties != null && !properties.isEmpty()) {
                println "  Parameters:"
                properties.each { paramName, paramSchema ->
                    println "    - ${paramName}: ${paramSchema}"
                }
            }
            def required = inputSchema.required()
            if (required != null && !required.isEmpty()) {
                println "  Required: ${required.join(', ')}"
            }
        }
    }

    println "\n=========================================="
    println "TEST PASSED: MCP server returned ${tools.size()} tool(s)"
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
