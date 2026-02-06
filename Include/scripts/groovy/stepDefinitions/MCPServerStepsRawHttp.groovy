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

class MCPServerStepsRawHttp {

	// Shared state between steps
	static String sessionId
	static def initResult
	static def toolsResult
	static def tools
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
		RequestObject request = findTestObject('Object Repository/Fetch MCP')

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

	@Given("a raw HTTP MCP server at {string} with endpoint {string}")
	def setMcpServer(String serverBaseUrl, String serverEndpoint) {
		// Reset state - URL is configured in Object Repository
		sessionId = null
		requestId = 0
		println "Testing MCP server (raw HTTP): ${serverBaseUrl}${serverEndpoint}"
		println "Using Object Repository: Fetch MCP"
	}

	@When("I initialize the MCP client via raw HTTP")
	def initializeMcpClient() {
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
		println "MCP client initialized via raw HTTP"

		// Send initialized notification
		def notifyBody = JsonOutput.toJson([
			jsonrpc: "2.0",
			method: "notifications/initialized",
			params: [:]
		])
		def notifyRequest = createMcpRequest(notifyBody, sessionId)
		WS.sendRequest(notifyRequest)
	}

	@Then("the raw HTTP connection should be successful")
	def verifyConnectionSuccessful() {
		assert initResult != null : "Init result should not be null"
		assert initResult.result != null : "Init result.result should not be null"
		println "Connection verified successful"
	}

	@Then("the raw HTTP server name should be {string}")
	def verifyServerName(String expectedName) {
		def actualName = initResult.result?.serverInfo?.name
		assert actualName == expectedName : "Expected server name '${expectedName}' but got '${actualName}'"
		println "Server name verified: ${actualName}"
	}

	@Then("the raw HTTP protocol version should be {string}")
	def verifyProtocolVersion(String expectedVersion) {
		def actualVersion = initResult.result?.protocolVersion
		assert actualVersion == expectedVersion : "Expected protocol version '${expectedVersion}' but got '${actualVersion}'"
		println "Protocol version verified: ${actualVersion}"
	}

	@Then("the raw HTTP server should support tools")
	def verifyToolsSupported() {
		def capabilities = initResult.result?.capabilities
		assert capabilities?.tools != null : "Server should support tools"
		println "Tools capability verified"
	}

	@Then("the raw HTTP server should not support resources")
	def verifyResourcesNotSupported() {
		def capabilities = initResult.result?.capabilities
		assert capabilities?.resources == null : "Server should not support resources"
		println "Resources capability verified (not supported)"
	}

	@Then("the raw HTTP server should not support prompts")
	def verifyPromptsNotSupported() {
		def capabilities = initResult.result?.capabilities
		assert capabilities?.prompts == null : "Server should not support prompts"
		println "Prompts capability verified (not supported)"
	}

	@When("I request the list of tools via raw HTTP")
	def requestToolsList() {
		def toolsBody = createJsonRpcRequest("tools/list", [:])
		def request = createMcpRequest(toolsBody, sessionId)
		ResponseObject response = WS.sendRequest(request)

		WS.verifyResponseStatusCode(response, 200)

		toolsResult = parseSseResponse(response.getResponseText())
		tools = toolsResult.result?.tools
		println "Retrieved ${tools?.size() ?: 0} tools via raw HTTP"
	}

	@Then("the raw HTTP tools list should not be empty")
	def verifyToolsListNotEmpty() {
		assert tools != null : "Tools list should not be null"
		assert tools.size() > 0 : "Tools list should not be empty"
		println "Tools list contains ${tools.size()} tool(s)"
	}

	@Then("the raw HTTP tools list should contain a tool named {string}")
	def verifyToolExists(String toolName) {
		def tool = tools.find { it.name == toolName }
		assert tool != null : "Tool '${toolName}' should exist in tools list"
		println "Found tool: ${toolName}"
	}

	@Then("the raw HTTP {string} tool should have a required parameter {string}")
	def verifyRequiredParameter(String toolName, String paramName) {
		def tool = tools.find { it.name == toolName }
		assert tool != null : "Tool '${toolName}' should exist"

		def inputSchema = tool.inputSchema
		assert inputSchema != null : "Tool should have input schema"

		def required = inputSchema.required
		assert required != null && required.contains(paramName) :
		"Parameter '${paramName}' should be required for tool '${toolName}'"
		println "Verified required parameter '${paramName}' for tool '${toolName}'"
	}

	@Then("the raw HTTP {string} tool should have an optional parameter {string}")
	def verifyOptionalParameter(String toolName, String paramName) {
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

	@After("@RawHTTP")
	def cleanup() {
		sessionId = null
		initResult = null
		toolsResult = null
		tools = null
		requestId = 0
		println "Raw HTTP MCP client state cleared"
	}
}
