/*
 * Copyright 2026 Katalon Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import java.time.Duration

// Test configuration - CoinGecko public SSE MCP server
// URL centralized in Object Repository for single-point configuration
String mcpServerSseUrl = findTestObject('CoinGecko MCP SSE').getRestUrl()

println "=========================================="
println "MCP Server Tools Test (SSE Transport)"
println "Server URL: ${mcpServerSseUrl}"
println "MCP SDK Version: 0.15.0"
println "=========================================="

// Create the SSE transport for the remote MCP server
def transport = HttpClientSseClientTransport.builder(mcpServerSseUrl)
    .build()

// Create synchronous MCP client
McpSyncClient mcpClient = McpClient.sync(transport)
    .requestTimeout(Duration.ofSeconds(60))
    .build()

try {
    // Initialize the connection to the MCP server
    println "\n[Step 1] Initializing connection to MCP server via SSE..."
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
    println "TEST PASSED: MCP SSE server returned ${tools.size()} tool(s)"
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
