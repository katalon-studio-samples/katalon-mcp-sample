# AGENTS.md

Instructions for AI agents working with this Katalon Studio MCP testing project.

## Project Overview

This is a **Katalon Studio WEBSERVICE project** for API test automation. Created with Katalon Studio 10.4.2 Enterprise edition.

**Purpose:** Test MCP (Model Context Protocol) servers using both native Katalon HTTP capabilities and the official MCP Java SDK.

**Target MCP Server:** `https://remote.mcpservers.org/fetch/mcp` (mcp-fetch server)

## Prerequisites

- **Java 17** (required)
- **Katalon Studio 10.4.2+** Enterprise edition (for IDE execution)
- **Katalon Runtime Engine** (for command-line execution)

## Build Commands

```bash
# Install dependencies (copies JARs to Drivers/ folder)
./gradlew clean katalonCopyDependencies
```

## Running Tests

### Via Command Line (requires Katalon Runtime Engine)

```bash
# Run Raw HTTP test suite (recommended - working)
katalonc -noSplash -runMode=console -projectPath="$(pwd)/katalon-mcp-sample.prj" \
  -testSuitePath="Test Suites/MCP Server Test Suite - Raw" \
  -executionProfile="default"

# Run Raw HTTP BDD tests
katalonc -noSplash -runMode=console -projectPath="$(pwd)/katalon-mcp-sample.prj" \
  -testSuitePath="Test Suites/MCP Server Test Suite - BDD - Raw" \
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
| `io.modelcontextprotocol.sdk:mcp` | 0.17.2 | MCP Java SDK (has library conflicts) |

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
| `MCP Server Tools Test (Raw HTTP)` | **Working** | Use this |
| `MCP Server Tools Test` | Blocked | SDK library conflicts |
| `Run MCP BDD Tests (Raw HTTP)` | **Working** | Use this |
| `Run MCP BDD Tests` | Blocked | SDK library conflicts |

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

### Approach 2: MCP Java SDK (Has Library Conflicts)

Uses the official MCP Java SDK. Currently blocked by dependency conflicts.

**Test Case:** `Test Cases/MCP Server Tools Test`

**Configuration for HTTP transport:**
```groovy
String mcpServerBaseUrl = "https://remote.mcpservers.org"
String mcpEndpoint = "/fetch/mcp"

def transport = HttpClientStreamableHttpTransport.builder(mcpServerBaseUrl)
    .endpoint(mcpEndpoint)  // Must use separate baseUrl and endpoint
    .build()
```

**Known Issue:** The MCP SDK depends on `json-schema-validator` 2.0.0+ which uses a `Dialects` class that doesn't exist in Katalon's bundled version (1.5.7). This causes `NoClassDefFoundError` at runtime.

**Attempted workarounds:**
1. Excluding Katalon's bundled libraries via Project Settings > Library Management - breaks Katalon internals
2. Excluding SDK's validator in build.gradle - breaks SDK schema validation
3. Both exclusion approaches lead to `ClassNotFoundException` or `NoClassDefFoundError`

**Goal:** Eventually get MCP SDK working for full protocol support

## BDD Testing (Cucumber)

Feature files are in `Include/features/` and step definitions in `Include/scripts/groovy/stepDefinitions/`.

### Available Step Definitions

| Class | Feature File | Approach | Status |
|-------|-------------|----------|--------|
| `MCPServerStepsRawHttp` | `MCP_Server_Tools_RawHttp.feature` | Raw HTTP/JSON-RPC | **Working** |
| `MCPServerSteps` | `MCP_Server_Tools.feature` | MCP Java SDK | Has library conflicts |

### How Step Definitions Are Selected

Cucumber selects step definitions by **pattern matching** the step text, not by class name or file association. Both step definition classes are loaded when `GLUE = ['stepDefinitions']` is set.

To avoid conflicts, the two approaches use different step text patterns:
- **Raw HTTP:** Steps contain "raw HTTP" (e.g., `When I initialize the MCP client via raw HTTP`)
- **SDK:** Steps do not contain "raw HTTP" (e.g., `When I initialize the MCP client`)

### Step Definition Patterns

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
- `@RawHTTP` - Raw HTTP approach tests (triggers cleanup hook in MCPServerStepsRawHttp)

## Troubleshooting

### MCP SDK Library Conflicts

**Error:** `NoClassDefFoundError: com/networknt/schema/Dialects`

**Cause:** MCP SDK 0.17.2 depends on `json-schema-validator` 2.0.0+ which requires the `Dialects` class. Katalon bundles version 1.5.7 which doesn't have this class.

**Current workaround:** Use the Raw HTTP approach instead of the SDK.

**Potential future solutions:**
1. Wait for Katalon to upgrade their bundled json-schema-validator
2. Use an older MCP SDK version that's compatible with json-schema-validator 1.x
3. Shadow/relocate the conflicting classes in build.gradle

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

**mcp-fetch server capabilities:**
- Tools: Yes (fetch tool for URL fetching)
- Resources: No
- Prompts: No
