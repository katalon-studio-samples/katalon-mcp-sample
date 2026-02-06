# AGENTS.md

Instructions for AI agents working with this Katalon Studio MCP testing project.

## Project Overview

This is a **Katalon Studio WEBSERVICE project** for API test automation. Created with Katalon Studio 10.4.2 Enterprise edition.

**Purpose:** Test MCP (Model Context Protocol) servers using both native Katalon HTTP capabilities and the official MCP Java SDK.

**Target MCP Servers:**
- `https://remote.mcpservers.org/fetch/mcp` (mcp-fetch server)
- `https://mcp.katalon.com/mcp` (Katalon Public MCP Server)

## Prerequisites

- **Java 17** (required)
- **Katalon Studio 10.4.2+** Enterprise edition (for IDE execution)
- **Katalon Runtime Engine** (for command-line execution)

## Build Commands

```bash
# Install MCP SDK dependencies (copies JARs to Drivers/ folder)
# Required for: SSE, Streamable HTTP, and stdio transport tests
# Not required for: Raw HTTP tests
./gradlew clean katalonCopyDependencies
```

## Running Tests

### Via Command Line (requires Katalon Runtime Engine)

```bash
# Run Raw HTTP test suite (no SDK required)
katalonc -noSplash -runMode=console -projectPath="$(pwd)/katalon-mcp-sample.prj" \
  -testSuitePath="Test Suites/MCP Server Test Suite - Raw" \
  -executionProfile="default"

# Run SDK-based test suite (requires SDK installation)
katalonc -noSplash -runMode=console -projectPath="$(pwd)/katalon-mcp-sample.prj" \
  -testSuitePath="Test Suites/MCP Server Test Suite" \
  -executionProfile="default"
```

### Via Katalon Studio IDE

1. Open `katalon-mcp-sample.prj` in Katalon Studio
2. Navigate to Test Suites
3. Right-click and run the desired suite

## Adding Dependencies

1. Add to `build.gradle` using `implementation` configuration:
   ```gradle
   implementation 'group:artifact:version'
   ```
2. Run: `./gradlew clean katalonCopyDependencies`
3. JARs are copied to `Drivers/` folder

### Current Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| `io.modelcontextprotocol.sdk:mcp` | 0.15.0 | MCP Java SDK (SSE & stdio transports) |

**Note:** Version 0.16.0+ requires `json-schema-validator:2.0.0` which conflicts with Katalon's bundled 1.5.7. Use 0.15.0 until Katalon upgrades its bundled version.

## Project Structure

| Type | Location | Format |
|------|----------|--------|
| Test cases | `Test Cases/` | `.tc` XML files |
| Test scripts | `Scripts/` | Groovy (matches Test Case name) |
| Test suites | `Test Suites/` | `.ts` XML + `.groovy` files |
| API requests | `Object Repository/` | `.rs` files |
| Custom keywords | `Keywords/` | Groovy classes |
| BDD features | `Include/features/` | `.feature` files |
| Step definitions | `Include/scripts/groovy/stepDefinitions/` | Groovy classes |
| Global variables | `Profiles/` | `.glbl` XML files |
| External JARs | `Drivers/` | JAR files (from Gradle) |

### Do Not Edit

- `Libs/` - Auto-generated files (CustomKeywords.groovy, GlobalVariable.groovy)

## Code Patterns

```groovy
// Accessing global variables
import internal.GlobalVariable
GlobalVariable.myVariable

// Using Object Repository
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
RequestObject request = findTestObject('Object Repository/Fetch MCP')

// Running BDD tests
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
CucumberKW.GLUE = ['stepDefinitions']
CucumberKW.runFeatureFile('Include/features/MyFeature.feature')
```

## Test Case Status

| Test Case | Status | Notes |
|-----------|--------|-------|
| `Katalon MCP Server Test` | **Working** | Tests Katalon Public MCP Server (Raw HTTP) |
| `Run Katalon MCP BDD Tests` | **Working** | BDD tests for Katalon MCP Server |
| `MCP Server Tools Test (Raw HTTP)` | **Working** | Tests mcp-fetch server (Raw HTTP) |
| `MCP Server Tools Test (SSE)` | **Working** | Tests CoinGecko server via SDK SSE transport |
| `MCP Server Tools Test` | **Working** | Tests mcp-fetch server via SDK Streamable HTTP |
| `MCP Server Tools Test (stdio)` | **Working** | Tests local mcp-filesystem-server via SDK stdio |
| `Run MCP BDD Tests (Raw HTTP)` | **Working** | BDD tests for mcp-fetch server |
| `Run MCP BDD Tests` | **Working** | BDD tests using SDK (Streamable HTTP) |

## MCP Server Testing

Two approaches are available for testing MCP servers:

### Approach 1: Raw HTTP (Currently Working)

Uses Katalon's native WS keywords with JSON-RPC 2.0. No external dependencies needed.

**Test Case:** `Test Cases/MCP Server Tools Test (Raw HTTP)`

**Object Repository:** `Object Repository/Fetch MCP` - Base request template with URL and headers

**Key implementation details:**

1. **Using Object Repository Template:**
   ```groovy
   import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

   // Get base request from Object Repository, override body
   RequestObject request = findTestObject('Object Repository/Fetch MCP')
   request.setBodyContent(new HttpTextBodyContent(body, "UTF-8", "application/json"))
   ```

2. **JSON-RPC 2.0 Protocol:** MCP uses JSON-RPC over HTTP with SSE responses
   ```groovy
   def body = JsonOutput.toJson([
       jsonrpc: "2.0", id: 1, method: "initialize",
       params: [protocolVersion: "2024-11-05", capabilities: [:],
                clientInfo: [name: "katalon-mcp-test", version: "1.0.0"]]
   ])
   ```

3. **Session ID Handling:** Server returns `mcp-session-id` header that must be included in subsequent requests. Header name case varies by server:
   ```groovy
   // Try multiple case variations
   sessionId = headerFields?.get("mcp-session-id")?.get(0) ?:
               headerFields?.get("Mcp-Session-Id")?.get(0) ?:
               headerFields?.get("MCP-Session-ID")?.get(0)

   // Fallback to case-insensitive search
   if (!sessionId) {
       headerFields?.each { key, value ->
           if (key?.toLowerCase() == "mcp-session-id") {
               sessionId = value?.get(0)
           }
       }
   }
   ```

4. **SSE Response Parsing:** Responses may be in SSE format:
   ```groovy
   def parseSseResponse(String responseBody) {
       def lines = responseBody.split('\n')
       def dataLine = lines.find { it.startsWith('data: ') }
       if (dataLine) {
           return new JsonSlurper().parseText(dataLine.substring(6))
       }
       return new JsonSlurper().parseText(responseBody)
   }
   ```

5. **MCP Lifecycle:** Initialize → Send `notifications/initialized` → Call methods (tools/list, tools/call, etc.)

### Approach 2: MCP Java SDK - SSE Transport (Working)

Uses MCP SDK 0.7.0 with SSE transport for servers that support the legacy SSE protocol.

**Test Case:** `Test Cases/MCP Server Tools Test (SSE)`

**Target Server:** CoinGecko Public MCP Server (`https://mcp.api.coingecko.com/sse`)

**Configuration for SSE transport:**
```groovy
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport

String mcpServerSseUrl = "https://mcp.api.coingecko.com/sse"

def transport = HttpClientSseClientTransport.builder(mcpServerSseUrl)
    .build()

McpSyncClient mcpClient = McpClient.sync(transport)
    .requestTimeout(Duration.ofSeconds(60))
    .build()

def initResult = mcpClient.initialize()
def toolsResult = mcpClient.listTools()
```

**Other public SSE MCP servers:** Sentry, Linear, Neon, PayPal, Square, Asana, Atlassian, Webflow (see https://mcpservers.org/remote-mcp-servers)

### Approach 3: MCP Java SDK - Streamable HTTP (Working)

Uses MCP SDK 0.15.0 with Streamable HTTP transport.

**Test Case:** `Test Cases/MCP Server Tools Test`

**Target Server:** mcp-fetch server (`https://remote.mcpservers.org/fetch/mcp`)

**Configuration for Streamable HTTP transport:**
```groovy
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport

String mcpServerBaseUrl = "https://remote.mcpservers.org"
String mcpEndpoint = "/fetch/mcp"

def transport = HttpClientStreamableHttpTransport.builder(mcpServerBaseUrl)
    .endpoint(mcpEndpoint)
    .build()

McpSyncClient mcpClient = McpClient.sync(transport)
    .requestTimeout(Duration.ofSeconds(60))
    .build()

def initResult = mcpClient.initialize()
def toolsResult = mcpClient.listTools()
```

**Note:** SDK 0.16.0+ requires `json-schema-validator:2.0.0` which conflicts with Katalon's bundled 1.5.7. SDK 0.15.0 works without this conflict.

### Approach 4: MCP Java SDK - stdio (Working)

Uses MCP SDK 0.15.0 with stdio transport for local MCP servers.

**Test Case:** `Test Cases/MCP Server Tools Test (stdio)`

**Target Server:** mcp-filesystem-server (local, installed via `go install github.com/mark3labs/mcp-filesystem-server@latest`)

**Server Location:** `$GOPATH/bin/mcp-filesystem-server` (default: `~/go/bin/mcp-filesystem-server`)

**Configuration for stdio transport:**
```groovy
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.ServerParameters
import io.modelcontextprotocol.client.transport.StdioClientTransport
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper
import com.fasterxml.jackson.databind.ObjectMapper

String serverPath = "${System.getProperty('user.home')}/go/bin/mcp-filesystem-server"
String allowedDirectory = "/path/to/allowed/dir"

def serverParams = ServerParameters.builder(serverPath)
    .args([allowedDirectory])
    .build()

def objectMapper = new ObjectMapper()
def jsonMapper = new JacksonMcpJsonMapper(objectMapper)
def transport = new StdioClientTransport(serverParams, jsonMapper)

McpSyncClient mcpClient = McpClient.sync(transport)
    .requestTimeout(Duration.ofSeconds(30))
    .build()

def initResult = mcpClient.initialize()
def toolsResult = mcpClient.listTools()
```

## BDD Testing (Cucumber)

Feature files are in `Include/features/` and step definitions in `Include/scripts/groovy/stepDefinitions/`.

### Available Step Definitions

| Class | Feature File | Approach | Status |
|-------|-------------|----------|--------|
| `KatalonMCPServerSteps` | `Katalon_MCP_Server.feature` | Streamable HTTP | **Working** |
| `MCPServerStepsRawHttp` | `MCP_Server_Tools_RawHttp.feature` | Raw HTTP/JSON-RPC | **Working** |
| `MCPServerSteps` | `MCP_Server_Tools.feature` | MCP Java SDK | Has library conflicts |

### How Step Definitions Are Selected

Cucumber selects step definitions by **pattern matching** the step text, not by class name or file association. Both step definition classes are loaded when `GLUE = ['stepDefinitions']` is set.

To avoid conflicts, the two approaches use different step text patterns:
- **Raw HTTP:** Steps contain "raw HTTP" (e.g., `When I initialize the MCP client via raw HTTP`)
- **SDK:** Steps do not contain "raw HTTP" (e.g., `When I initialize the MCP client`)

### Step Definition Patterns

**Katalon MCP steps (KatalonMCPServerSteps.groovy):**
- `Given a Katalon MCP server at {string} with endpoint {string}`
- `When I initialize the Katalon MCP client`
- `When I request the list of tools from Katalon MCP`
- `When I call the Katalon MCP {string} tool with query {string}`
- `When I call the Katalon MCP {string} tool with query {string} and product {string}`
- `Then the Katalon MCP connection should be successful`
- `Then the Katalon MCP server name should be {string}`
- `Then the Katalon MCP protocol version should be {string}`
- `Then the Katalon MCP server should support tools`
- `Then the Katalon MCP server should support resources`
- `Then the Katalon MCP server should support prompts`
- `Then the Katalon MCP tools list should not be empty`
- `Then the Katalon MCP tools list should contain a tool named {string}`
- `Then the Katalon MCP {string} tool should have a required parameter {string}`
- `Then the Katalon MCP {string} tool should have an optional parameter {string}`
- `Then the Katalon MCP tool call should be successful`
- `Then the Katalon MCP tool result should contain search results`

**Raw HTTP steps (MCPServerStepsRawHttp.groovy):**
- `Given a raw HTTP MCP server at {string} with endpoint {string}`
- `When I initialize the MCP client via raw HTTP`
- `When I request the list of tools via raw HTTP`
- `Then the raw HTTP connection should be successful`
- `Then the raw HTTP server name should be {string}`
- `Then the raw HTTP protocol version should be {string}`
- `Then the raw HTTP server should support tools`
- `Then the raw HTTP tools list should not be empty`
- `Then the raw HTTP tools list should contain a tool named {string}`
- `Then the raw HTTP {string} tool should have a required parameter {string}`
- `Then the raw HTTP {string} tool should have an optional parameter {string}`

**SDK steps (MCPServerSteps.groovy):**
- `Given an MCP server at {string}`
- `When I initialize the MCP client`
- `When I request the list of tools`
- `Then the connection should be successful`
- `Then the server name should be {string}`
- etc.

### BDD Test Tags

- `@MCP` - All MCP-related tests
- `@KatalonMCP` - Katalon MCP Server tests (triggers cleanup hook in KatalonMCPServerSteps)
- `@RawHTTP` - Raw HTTP approach tests (triggers cleanup hook in MCPServerStepsRawHttp)
- `@Smoke` - Basic connectivity and capability tests
- `@Tools` - Tool listing and schema validation tests
- `@Integration` - Tool execution tests

## Troubleshooting

### MCP SDK Library Conflicts

**Error:** `NoClassDefFoundError: com/networknt/schema/Dialects`

**Cause:** MCP SDK 0.17.2 depends on `json-schema-validator` 2.0.0+ which requires the `Dialects` class. Katalon bundles version 1.5.7 which doesn't have this class.

**Current workaround:** Use the Raw HTTP approach instead of the SDK.

**Potential future solutions:**
1. Wait for Katalon to upgrade their bundled json-schema-validator
2. Use an older MCP SDK version that's compatible with json-schema-validator 1.x
3. Shadow/relocate the conflicting classes in build.gradle

### BDD Steps "Undefined" After Adding New Step Definitions

**Error:** `8 Scenarios (8 undefined), 41 Steps (41 undefined)` - All steps show `# null` instead of method references.

**Cause:** Katalon Studio hasn't compiled/detected newly added step definition files.

**Solution:** Close and reopen the project in Katalon Studio. Alternatively:
- Right-click on the project → **Refresh**
- Or use **Project → Refresh** (F5)

This forces Katalon to recompile Groovy files and make new step definitions available to Cucumber.

### 400 Bad Request on MCP Calls

**Cause:** Missing or incorrect `mcp-session-id` header.

**Solution:** Extract the session ID from the initialize response and include it in subsequent requests. Use case-insensitive header lookup.

### 404 Not Found with MCP SDK

**Cause:** Incorrect URL configuration - SDK may append a default endpoint.

**Solution:** Use separate `baseUrl` and `.endpoint()` configuration:
```groovy
def transport = HttpClientStreamableHttpTransport.builder("https://remote.mcpservers.org")
    .endpoint("/fetch/mcp")
    .build()
```

### SSE Response Parsing Errors

**Cause:** Response may be in Server-Sent Events format (`data: {...}`) instead of plain JSON.

**Solution:** Check for SSE format and extract the JSON data:
```groovy
if (responseBody.contains("data: ")) {
    def dataLine = responseBody.split('\n').find { it.startsWith('data: ') }
    json = new JsonSlurper().parseText(dataLine.substring(6))
}
```

## MCP Protocol Reference

**Protocol Version:** 2024-11-05

**JSON-RPC Methods:**
- `initialize` - Initialize connection, returns server capabilities
- `notifications/initialized` - Client notification after init (no response expected)
- `tools/list` - Get available tools
- `tools/call` - Execute a tool
- `resources/list` - Get available resources (if supported)
- `prompts/list` - Get available prompts (if supported)

**Katalon Public MCP Server capabilities:**
- Server URL: `https://mcp.katalon.com/mcp`
- Tools: Yes
  - `fetch` - Fetch Katalon documentation pages (required: `id`)
  - `search` - General search across all Katalon docs (required: `query`)
  - `search_katalon_documentation_with_filter` - Filtered search by product (required: `query`, optional: `products`, `max_results`)
- Resources: Yes
- Prompts: Yes
- Tasks: Yes

**mcp-fetch server capabilities:**
- Server URL: `https://remote.mcpservers.org/fetch/mcp`
- Tools: Yes (fetch tool for URL fetching)
- Resources: No
- Prompts: No
