package stepDefinitions

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import cucumber.api.java.en.Given
import cucumber.api.java.en.When
import cucumber.api.java.en.Then
import cucumber.api.java.en.And
import cucumber.api.java.After

import com.kms.katalon.core.testobject.RequestObject
import com.kms.katalon.core.testobject.ResponseObject
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.testobject.impl.HttpTextBodyContent
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class KatalonMCPServerSteps {

	// Shared state between steps
	static String sessionId
	static def initResult
	static def toolsResult
	static def tools
	static def toolCallResult
	static int requestId = 0

	// Helper to create JSON-RPC request body
	static def createJsonRpcRequest(String method, Map params = [:]) {
		requestId++
		return JsonOutput.toJson([
			jsonrpc: "2.0",
			id: requestId,
			method: method,
			params: params
		])
	}

	// Helper to parse SSE response and extract JSON-RPC result
	static def parseSseResponse(String responseBody) {
		def lines = responseBody.split('\n')
		def dataLine = lines.find { it.startsWith('data: ') }
		if (dataLine) {
			def jsonStr = dataLine.substring(6)
			return new JsonSlurper().parseText(jsonStr)
		}
		return new JsonSlurper().parseText(responseBody)
	}

	// Helper to create MCP request from Object Repository template
	static RequestObject createMcpRequest(String body, String sessId = null) {
		RequestObject request = findTestObject('Object Repository/Katalon MCP')

		// Add session ID header if provided
		if (sessId) {
			def headers = request.getHttpHeaderProperties() ?: []
			headers.add(new TestObjectProperty("mcp-session-id", null, sessId))
			request.setHttpHeaderProperties(headers)
		}

		// Set body
		request.setBodyContent(new HttpTextBodyContent(body, "UTF-8", "application/json"))

		return request
	}

	// Helper to extract session ID from response
	static String extractSessionId(ResponseObject response) {
		def headerFields = response.getHeaderFields()
		def sessId = headerFields?.get("mcp-session-id")?.get(0) ?:
				headerFields?.get("Mcp-Session-Id")?.get(0) ?:
				headerFields?.get("MCP-Session-ID")?.get(0)

		if (!sessId) {
			headerFields?.each { key, value ->
				if (key?.toLowerCase() == "mcp-session-id") {
					sessId = value?.get(0)
				}
			}
		}
		return sessId
	}

	@Given("a Katalon MCP server at {string} with endpoint {string}")
	def setKatalonMcpServer(String serverBaseUrl, String serverEndpoint) {
		// Reset state - URL is configured in Object Repository
		sessionId = null
		requestId = 0
		toolCallResult = null
		println "Testing Katalon MCP server: ${serverBaseUrl}${serverEndpoint}"
		println "Using Object Repository: Katalon MCP"
	}

	@When("I initialize the Katalon MCP client")
	def initializeKatalonMcpClient() {
		def initBody = createJsonRpcRequest("initialize", [
			protocolVersion: "2024-11-05",
			capabilities: [:],
			clientInfo: [name: "katalon-mcp-test", version: "1.0.0"]
		])

		def request = createMcpRequest(initBody)
		ResponseObject response = WS.sendRequest(request)

		WS.verifyResponseStatusCode(response, 200)

		sessionId = extractSessionId(response)
		println "Session ID: ${sessionId ?: 'none'}"

		initResult = parseSseResponse(response.getResponseText())
		println "Katalon MCP client initialized"

		// Send initialized notification
		def notifyBody = JsonOutput.toJson([
			jsonrpc: "2.0",
			method: "notifications/initialized",
			params: [:]
		])
		def notifyRequest = createMcpRequest(notifyBody, sessionId)
		WS.sendRequest(notifyRequest)
	}

	@Then("the Katalon MCP connection should be successful")
	def verifyKatalonConnectionSuccessful() {
		assert initResult != null : "Init result should not be null"
		assert initResult.result != null : "Init result.result should not be null"
		println "Katalon MCP connection verified successful"
	}

	@Then("the Katalon MCP server name should be {string}")
	def verifyKatalonServerName(String expectedName) {
		def actualName = initResult.result?.serverInfo?.name
		assert actualName == expectedName : "Expected server name '${expectedName}' but got '${actualName}'"
		println "Server name verified: ${actualName}"
	}

	@Then("the Katalon MCP protocol version should be {string}")
	def verifyKatalonProtocolVersion(String expectedVersion) {
		def actualVersion = initResult.result?.protocolVersion
		assert actualVersion == expectedVersion : "Expected protocol version '${expectedVersion}' but got '${actualVersion}'"
		println "Protocol version verified: ${actualVersion}"
	}

	@Then("the Katalon MCP server should support tools")
	def verifyKatalonToolsSupported() {
		def capabilities = initResult.result?.capabilities
		assert capabilities?.tools != null : "Server should support tools"
		println "Tools capability verified"
	}

	@Then("the Katalon MCP server should support resources")
	def verifyKatalonResourcesSupported() {
		def capabilities = initResult.result?.capabilities
		assert capabilities?.resources != null : "Server should support resources"
		println "Resources capability verified"
	}

	@Then("the Katalon MCP server should support prompts")
	def verifyKatalonPromptsSupported() {
		def capabilities = initResult.result?.capabilities
		assert capabilities?.prompts != null : "Server should support prompts"
		println "Prompts capability verified"
	}

	@When("I request the list of tools from Katalon MCP")
	def requestKatalonToolsList() {
		def toolsBody = createJsonRpcRequest("tools/list", [:])
		def request = createMcpRequest(toolsBody, sessionId)
		ResponseObject response = WS.sendRequest(request)

		WS.verifyResponseStatusCode(response, 200)

		toolsResult = parseSseResponse(response.getResponseText())
		tools = toolsResult.result?.tools
		println "Retrieved ${tools?.size() ?: 0} tools from Katalon MCP"
	}

	@Then("the Katalon MCP tools list should not be empty")
	def verifyKatalonToolsListNotEmpty() {
		assert tools != null : "Tools list should not be null"
		assert tools.size() > 0 : "Tools list should not be empty"
		println "Tools list contains ${tools.size()} tool(s)"
	}

	@Then("the Katalon MCP tools list should contain a tool named {string}")
	def verifyKatalonToolExists(String toolName) {
		def tool = tools.find { it.name == toolName }
		assert tool != null : "Tool '${toolName}' should exist in tools list"
		println "Found tool: ${toolName}"
	}

	@Then("the Katalon MCP {string} tool should have a required parameter {string}")
	def verifyKatalonRequiredParameter(String toolName, String paramName) {
		def tool = tools.find { it.name == toolName }
		assert tool != null : "Tool '${toolName}' should exist"

		def inputSchema = tool.inputSchema
		assert inputSchema != null : "Tool should have input schema"

		def required = inputSchema.required
		assert required != null && required.contains(paramName) :
		"Parameter '${paramName}' should be required for tool '${toolName}'"
		println "Verified required parameter '${paramName}' for tool '${toolName}'"
	}

	@Then("the Katalon MCP {string} tool should have an optional parameter {string}")
	def verifyKatalonOptionalParameter(String toolName, String paramName) {
		def tool = tools.find { it.name == toolName }
		assert tool != null : "Tool '${toolName}' should exist"

		def inputSchema = tool.inputSchema
		assert inputSchema != null : "Tool should have input schema"

		def properties = inputSchema.properties
		assert properties != null && properties.containsKey(paramName) :
		"Parameter '${paramName}' should exist for tool '${toolName}'"

		def required = inputSchema.required ?: []
		assert !required.contains(paramName) :
		"Parameter '${paramName}' should be optional (not required) for tool '${toolName}'"
		println "Verified optional parameter '${paramName}' for tool '${toolName}'"
	}

	@When("I call the Katalon MCP {string} tool with query {string}")
	def callKatalonSearchTool(String toolName, String query) {
		def callBody = createJsonRpcRequest("tools/call", [
			name: toolName,
			arguments: [query: query]
		])

		def request = createMcpRequest(callBody, sessionId)
		ResponseObject response = WS.sendRequest(request)

		WS.verifyResponseStatusCode(response, 200)

		toolCallResult = parseSseResponse(response.getResponseText())
		println "Called ${toolName} tool with query: ${query}"
	}

	@When("I call the Katalon MCP {string} tool with query {string} and product {string}")
	def callKatalonFilteredSearchTool(String toolName, String query, String product) {
		def callBody = createJsonRpcRequest("tools/call", [
			name: toolName,
			arguments: [
				query: query,
				products: [product]
			]
		])

		def request = createMcpRequest(callBody, sessionId)
		ResponseObject response = WS.sendRequest(request)

		WS.verifyResponseStatusCode(response, 200)

		toolCallResult = parseSseResponse(response.getResponseText())
		println "Called ${toolName} tool with query: ${query}, product: ${product}"
	}

	@Then("the Katalon MCP tool call should be successful")
	def verifyKatalonToolCallSuccessful() {
		assert toolCallResult != null : "Tool call result should not be null"
		assert toolCallResult.result != null : "Tool call should return a result"
		assert toolCallResult.error == null : "Tool call should not return an error"
		println "Tool call verified successful"
	}

	@Then("the Katalon MCP tool result should contain search results")
	def verifyKatalonToolResultContainsResults() {
		assert toolCallResult.result != null : "Tool result should not be null"
		// The result contains content array with search results
		def content = toolCallResult.result.content
		assert content != null : "Tool result should contain content"
		println "Tool result contains search results"
		println "Result preview: ${JsonOutput.toJson(toolCallResult.result).take(200)}..."
	}

	@After("@KatalonMCP")
	def cleanup() {
		sessionId = null
		initResult = null
		toolsResult = null
		tools = null
		toolCallResult = null
		requestId = 0
		println "Katalon MCP client state cleared"
	}
}
