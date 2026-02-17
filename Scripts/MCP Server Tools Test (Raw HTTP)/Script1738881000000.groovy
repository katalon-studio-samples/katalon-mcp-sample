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

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.testobject.RequestObject
import com.kms.katalon.core.testobject.ResponseObject
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.testobject.impl.HttpTextBodyContent
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

println "=========================================="
println "MCP Server Tools Test (Raw HTTP)"
println "Using Object Repository: Fetch MCP"
println "=========================================="

// Helper to create JSON-RPC request body
def createJsonRpcRequest(String method, Map params = [:], int id = 1) {
    return JsonOutput.toJson([
        jsonrpc: "2.0",
        id: id,
        method: method,
        params: params
    ])
}

// Helper to parse SSE response and extract JSON-RPC result
def parseSseResponse(String responseBody) {
    // SSE format: "event: message\ndata: {...json...}"
    def lines = responseBody.split('\n')
    def dataLine = lines.find { it.startsWith('data: ') }
    if (dataLine) {
        def jsonStr = dataLine.substring(6) // Remove "data: " prefix
        return new JsonSlurper().parseText(jsonStr)
    }
    // If not SSE format, try parsing as plain JSON
    return new JsonSlurper().parseText(responseBody)
}

// Helper to create MCP request from Object Repository template
def createMcpRequest(String body, String sessionId = null) {
    RequestObject request = findTestObject('Object Repository/Fetch MCP')

    // Add session ID header if provided
    if (sessionId) {
        def headers = request.getHttpHeaderProperties() ?: []
        headers.add(new TestObjectProperty("mcp-session-id", null, sessionId))
        request.setHttpHeaderProperties(headers)
    }

    // Set body
    request.setBodyContent(new HttpTextBodyContent(body, "UTF-8", "application/json"))

    return request
}

String sessionId = null

try {
    // Step 1: Initialize connection
    println "\n[Step 1] Initializing connection to MCP server..."

    def initBody = createJsonRpcRequest("initialize", [
        protocolVersion: "2024-11-05",
        capabilities: [:],
        clientInfo: [name: "katalon-mcp-test", version: "1.0.0"]
    ])

    def initRequest = createMcpRequest(initBody)
    ResponseObject initResponse = WS.sendRequest(initRequest)

    WS.verifyResponseStatusCode(initResponse, 200)

    // Extract session ID from response headers (try different case variations)
    def headerFields = initResponse.getHeaderFields()
    println "Response headers: ${headerFields?.keySet()}"

    // Try different header name cases
    sessionId = headerFields?.get("mcp-session-id")?.get(0) ?:
                headerFields?.get("Mcp-Session-Id")?.get(0) ?:
                headerFields?.get("MCP-Session-ID")?.get(0)

    // If still not found, search case-insensitively
    if (!sessionId) {
        headerFields?.each { key, value ->
            if (key?.toLowerCase() == "mcp-session-id") {
                sessionId = value?.get(0)
            }
        }
    }
    println "Session ID: ${sessionId ?: 'none'}"

    // Parse response
    def initResult = parseSseResponse(initResponse.getResponseText())
    println "Response: ${JsonOutput.prettyPrint(JsonOutput.toJson(initResult))}"

    assert initResult.result != null : "Init result should not be null"

    def serverInfo = initResult.result.serverInfo
    def protocolVersion = initResult.result.protocolVersion
    def capabilities = initResult.result.capabilities

    println "Connected successfully!"
    println "Server Name: ${serverInfo?.name ?: 'Unknown'}"
    println "Server Version: ${serverInfo?.version ?: 'Unknown'}"
    println "Protocol Version: ${protocolVersion ?: 'Unknown'}"

    // Step 2: Check capabilities
    println "\n[Step 2] Checking server capabilities..."
    println "Tools supported: ${capabilities?.tools != null}"
    println "Resources supported: ${capabilities?.resources != null}"
    println "Prompts supported: ${capabilities?.prompts != null}"

    // Step 3: Send initialized notification (required by MCP protocol)
    println "\n[Step 3] Sending initialized notification..."

    def notifyBody = JsonOutput.toJson([
        jsonrpc: "2.0",
        method: "notifications/initialized",
        params: [:]
    ])

    def notifyRequest = createMcpRequest(notifyBody, sessionId)
    ResponseObject notifyResponse = WS.sendRequest(notifyRequest)
    println "Initialized notification sent (status: ${notifyResponse.getStatusCode()})"

    // Step 4: List tools
    println "\n[Step 4] Fetching available tools..."

    def toolsBody = createJsonRpcRequest("tools/list", [:], 2)
    def toolsRequest = createMcpRequest(toolsBody, sessionId)
    ResponseObject toolsResponse = WS.sendRequest(toolsRequest)

    WS.verifyResponseStatusCode(toolsResponse, 200)

    def toolsResult = parseSseResponse(toolsResponse.getResponseText())
    def tools = toolsResult.result?.tools

    assert tools != null : "Tools list should not be null"
    assert tools.size() > 0 : "Server should return at least one tool"

    println "Found ${tools.size()} tool(s):"
    println "----------------------------------------"

    tools.each { tool ->
        println "\nTool: ${tool.name}"
        println "  Description: ${tool.description ?: 'No description'}"

        def inputSchema = tool.inputSchema
        if (inputSchema) {
            println "  Input Schema Type: ${inputSchema.type ?: 'object'}"
            if (inputSchema.properties) {
                println "  Parameters:"
                inputSchema.properties.each { paramName, paramSchema ->
                    println "    - ${paramName}: ${paramSchema}"
                }
            }
            if (inputSchema.required) {
                println "  Required: ${inputSchema.required.join(', ')}"
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
}
