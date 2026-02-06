# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Katalon Studio WEBSERVICE project** for API test automation. Created with Katalon Studio 10.4.2 Enterprise edition.

**Purpose:** Test MCP (Model Context Protocol) servers using both native Katalon HTTP capabilities and the official MCP Java SDK.

**Target MCP Server:** `https://remote.mcpservers.org/fetch/mcp` (mcp-fetch server)

## Project Structure

- **Test Cases/** - Test case definitions (.tc XML files)
- **Scripts/** - Groovy test scripts (corresponding to Test Cases)
- **Test Suites/** - Test suite definitions for grouping test cases
- **Object Repository/** - API request objects and test objects
- **Keywords/** - Custom keyword classes (Groovy)
- **Data Files/** - External test data (CSV, Excel, etc.)
- **Profiles/** - Execution profiles with global variables (e.g., `default.glbl`)
- **Include/scripts/groovy/** - Additional Groovy source files
- **Include/features/** - BDD feature files (Cucumber/Gherkin)
- **Libs/** - Auto-generated files (CustomKeywords.groovy, GlobalVariable.groovy) - do not edit manually
- **Drivers/** - External JAR dependencies (populated by `./gradlew katalonCopyDependencies`)

## Test Cases and Suites

### Test Cases
| Test Case | Status | Description |
|-----------|--------|-------------|
| `MCP Server Tools Test (Raw HTTP)` | **Working** | Tests MCP server using native Katalon WS keywords |
| `MCP Server Tools Test` | Has library conflicts | Tests MCP server using official SDK |
| `Run MCP BDD Tests (Raw HTTP)` | **Working** | Runs Raw HTTP BDD feature file |
| `Run MCP BDD Tests` | Has library conflicts | Runs SDK-based BDD feature file |

### Test Suites
| Suite | Description |
|-------|-------------|
| `MCP Server Test Suite - Raw` | Raw HTTP test cases |
| `MCP Server Test Suite - BDD - Raw` | Raw HTTP BDD tests |
| `MCP Server Test Suite` | SDK-based test cases |
| `MCP Server Test Suite - BDD` | SDK-based BDD tests |

## Running Tests

Tests are executed through Katalon Studio IDE or command line:

```bash
# Command line execution (requires Katalon Runtime Engine)
katalonc -noSplash -runMode=console -projectPath="$(pwd)/katalon-mcp-sample.prj" \
  -testSuitePath="Test Suites/MCP Server Test Suite - Raw" \
  -executionProfile="default"
```

## Build Commands

Requires **Java 17**. Use the Gradle wrapper to manage dependencies:

```bash
# Install dependencies (copies JARs to Drivers/ folder)
./gradlew clean katalonCopyDependencies
```

## Adding Dependencies

External libraries are added via `build.gradle` using `implementation` configuration. After modifying build.gradle, run `./gradlew clean katalonCopyDependencies` to copy JARs to `Drivers/`.

Current dependencies:
- `io.modelcontextprotocol.sdk:mcp:0.17.2` - MCP Java SDK for testing MCP servers

## Key Conventions

- Test scripts use Groovy syntax with Katalon's built-in keywords
- Custom keywords are created in `Keywords/` and auto-registered in `Libs/CustomKeywords.groovy`
- Global variables are defined in execution profiles (`Profiles/*.glbl`) and accessed via `GlobalVariable.<name>`
- API test objects are stored in `Object Repository/` as `.rs` files

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

### Running BDD Tests
```groovy
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW

CucumberKW.GLUE = ['stepDefinitions']

// Raw HTTP approach (working)
CucumberKW.runFeatureFile('Include/features/MCP_Server_Tools_RawHttp.feature')

// MCP SDK approach (has library conflicts)
CucumberKW.runFeatureFile('Include/features/MCP_Server_Tools.feature')
```

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
