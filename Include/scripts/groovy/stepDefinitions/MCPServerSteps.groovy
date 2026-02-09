package stepDefinitions

import cucumber.api.java.en.Given
import cucumber.api.java.en.When
import cucumber.api.java.en.Then
import cucumber.api.java.en.And
import cucumber.api.java.After

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema

import com.katalon.mcp.utils.SslHelper
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Duration

class MCPServerSteps {

	// Shared state between steps
	static String baseUrl
	static String endpoint
	static McpSyncClient mcpClient
	static def initResult
	static def toolsResult
	static def tools

	@Given("an MCP server at {string} with endpoint {string}")
	def setMcpServer(String serverBaseUrl, String serverEndpoint) {
		baseUrl = serverBaseUrl
		endpoint = serverEndpoint
		println "Configured MCP server: ${baseUrl}${endpoint}"
	}

	@When("I initialize the MCP client")
	def initializeMcpClient() {
		// Create HTTP request builder with proper headers
		def requestBuilder = HttpRequest.newBuilder()
				.header("Accept", "application/json, text/event-stream")
				.timeout(Duration.ofSeconds(60))

		// Create HTTP client with timeouts and trust-all SSL for transparent proxies
		def clientBuilder = SslHelper.createTrustAllClientBuilder()
				.connectTimeout(Duration.ofSeconds(30))

		// Create the Streamable HTTP transport
		def transport = HttpClientStreamableHttpTransport.builder(baseUrl)
				.endpoint(endpoint)
				.clientBuilder(clientBuilder)
				.requestBuilder(requestBuilder)
				.build()

		// Create synchronous MCP client
		mcpClient = McpClient.sync(transport)
				.requestTimeout(Duration.ofSeconds(60))
				.build()

		// Initialize connection
		initResult = mcpClient.initialize()
		println "MCP client initialized successfully"
	}

	@Then("the connection should be successful")
	def verifyConnectionSuccessful() {
		assert initResult != null : "Init result should not be null"
		assert initResult.serverInfo() != null : "Server info should not be null"
		println "Connection verified successful"
	}

	@Then("the server name should be {string}")
	def verifyServerName(String expectedName) {
		def actualName = initResult.serverInfo()?.name()
		assert actualName == expectedName : "Expected server name '${expectedName}' but got '${actualName}'"
		println "Server name verified: ${actualName}"
	}

	@Then("the protocol version should be {string}")
	def verifyProtocolVersion(String expectedVersion) {
		def actualVersion = initResult.protocolVersion()
		assert actualVersion == expectedVersion : "Expected protocol version '${expectedVersion}' but got '${actualVersion}'"
		println "Protocol version verified: ${actualVersion}"
	}

	@Then("the server should support tools")
	def verifyToolsSupported() {
		def capabilities = initResult.capabilities()
		assert capabilities?.tools() != null : "Server should support tools"
		println "Tools capability verified"
	}

	@Then("the server should not support resources")
	def verifyResourcesNotSupported() {
		def capabilities = initResult.capabilities()
		assert capabilities?.resources() == null : "Server should not support resources"
		println "Resources capability verified (not supported)"
	}

	@Then("the server should not support prompts")
	def verifyPromptsNotSupported() {
		def capabilities = initResult.capabilities()
		assert capabilities?.prompts() == null : "Server should not support prompts"
		println "Prompts capability verified (not supported)"
	}

	@When("I request the list of tools")
	def requestToolsList() {
		toolsResult = mcpClient.listTools()
		tools = toolsResult.tools()
		println "Retrieved ${tools?.size() ?: 0} tools"
	}

	@Then("the tools list should not be empty")
	def verifyToolsListNotEmpty() {
		assert tools != null : "Tools list should not be null"
		assert tools.size() > 0 : "Tools list should not be empty"
		println "Tools list contains ${tools.size()} tool(s)"
	}

	@Then("the tools list should contain a tool named {string}")
	def verifyToolExists(String toolName) {
		def tool = tools.find { it.name() == toolName }
		assert tool != null : "Tool '${toolName}' should exist in tools list"
		println "Found tool: ${toolName}"
	}

	@Then("the {string} tool should have a required parameter {string}")
	def verifyRequiredParameter(String toolName, String paramName) {
		def tool = tools.find { it.name() == toolName }
		assert tool != null : "Tool '${toolName}' should exist"

		def inputSchema = tool.inputSchema()
		assert inputSchema != null : "Tool should have input schema"

		def required = inputSchema.required()
		assert required != null && required.contains(paramName) :
		"Parameter '${paramName}' should be required for tool '${toolName}'"
		println "Verified required parameter '${paramName}' for tool '${toolName}'"
	}

	@Then("the {string} tool should have an optional parameter {string}")
	def verifyOptionalParameter(String toolName, String paramName) {
		def tool = tools.find { it.name() == toolName }
		assert tool != null : "Tool '${toolName}' should exist"

		def inputSchema = tool.inputSchema()
		assert inputSchema != null : "Tool should have input schema"

		def properties = inputSchema.properties()
		assert properties != null && properties.containsKey(paramName) :
		"Parameter '${paramName}' should exist for tool '${toolName}'"

		def required = inputSchema.required() ?: []
		assert !required.contains(paramName) :
		"Parameter '${paramName}' should be optional (not required) for tool '${toolName}'"
		println "Verified optional parameter '${paramName}' for tool '${toolName}'"
	}

	@After
	def cleanup() {
		if (mcpClient != null) {
			try {
				mcpClient.closeGracefully()
				println "MCP client closed"
			} catch (Exception e) {
				println "Warning: Error closing MCP client - ${e.message}"
			}
			mcpClient = null
			initResult = null
			toolsResult = null
			tools = null
		}
	}
}
