# Katalon MCP Sample

A Katalon Studio project demonstrating how to test [MCP (Model Context Protocol)](https://modelcontextprotocol.io/) servers using both raw HTTP requests and the official MCP Java SDK.

## What This Project Shows

- Testing MCP servers with Katalon's native WS keywords (no external dependencies)
- Testing MCP servers with the official MCP Java SDK
- BDD/Cucumber test structure for MCP server validation
- Handling MCP protocol specifics: JSON-RPC 2.0, SSE responses, session management
- Working behind transparent HTTPS proxies that intercept/re-sign TLS traffic

## Quick Start

### Prerequisites

- Katalon Studio 10.4.2+ (Enterprise edition)
- Java 17

### Setup

1. Open the project in Katalon Studio
2. Install MCP SDK (required for SDK-based tests):
   ```bash
   ./gradlew clean katalonCopyDependencies
   ```

### Run Tests

**Raw HTTP approach (no SDK required):**
- Test Case: `Test Cases/Katalon MCP Server Test`
- Test Case: `Test Cases/MCP Server Tools Test (Raw HTTP)`
- BDD: `Test Cases/Run Katalon MCP BDD Tests`
- BDD: `Test Cases/Run MCP BDD Tests (Raw HTTP)`

**MCP SDK approach (requires SDK installation):**

*Streamable HTTP:*
- Test Case: `Test Cases/MCP Server Tools Test`
- Suite: `Test Suites/MCP Server Test Suite`

*SSE:*
- Test Case: `Test Cases/MCP Server Tools Test (SSE)`
- Suite: `Test Suites/MCP Server Test Suite - SSE`

*stdio (local servers):*
- Test Case: `Test Cases/MCP Server Tools Test (stdio)`
- Suite: `Test Suites/MCP Server Test Suite - stdio`
- Also requires: `go install github.com/mark3labs/mcp-filesystem-server@latest`

**Transparent proxy tests (requires SDK installation):**
- Test Case: `Test Cases/Transparent Proxy/MCP Server Tools Test (SSE)` — SSE transport with preflight proxy detection and SSL bypass
- Test Case: `Test Cases/Transparent Proxy/SSL Certificate Test` — Validates `SslHelper` against [badssl.com](https://badssl.com/) endpoints

## Test Coverage

### Katalon Public MCP Server (`https://mcp.katalon.com/mcp`)

| Scenario | Description |
|----------|-------------|
| Connection | Initialize MCP client, verify server info |
| Capabilities | Check tools/resources/prompts support |
| Tools List | Retrieve and validate available tools (fetch, search, search_katalon_documentation_with_filter) |
| Tool Schema | Verify tool parameters (required/optional) |
| Tool Execution | Execute search tools and verify results |

### mcp-fetch Server (`https://remote.mcpservers.org/fetch/mcp`)

| Scenario | Description |
|----------|-------------|
| Connection | Initialize MCP client, verify server info |
| Capabilities | Check tools/resources/prompts support |
| Tools List | Retrieve and validate available tools |
| Tool Schema | Verify tool parameters (required/optional) |

## Project Structure

```
├── Test Cases/              # Test case definitions
├── Scripts/                 # Groovy test scripts
├── Test Suites/             # Test suite configurations
├── Include/
│   ├── features/            # BDD feature files
│   └── scripts/groovy/
│       └── stepDefinitions/ # Cucumber step definitions
├── Drivers/                 # External JARs (from Gradle)
├── build.gradle             # Dependencies
├── CLAUDE.md                # Detailed technical documentation (→ AGENTS.md)
└── README.md                # This file
```

### SSL Helper

`Include/scripts/groovy/com/katalon/mcp/utils/SslHelper.groovy` provides SSL bypass for MCP SDK transports (`java.net.http.HttpClient`) when running behind a transparent HTTPS proxy.

```groovy
import com.katalon.mcp.utils.SslHelper

// Pass to any MCP SDK transport builder
def transport = HttpClientSseClientTransport.builder(url)
    .clientBuilder(SslHelper.createTrustAllClientBuilder())
    .build()
```

The Transparent Proxy test scripts also include a **preflight check** that detects proxy interception before connecting, so test output clearly indicates whether a proxy is present.

**Note:** For Katalon `WS.sendRequest()` (Raw HTTP tests), SSL bypass is controlled by Katalon Studio project settings, not code.

## Supported MCP Transports

| Transport | Status | Notes |
|-----------|--------|-------|
| Streamable HTTP | ✅ Supported | Raw HTTP or MCP SDK 0.15.0 `HttpClientStreamableHttpTransport` |
| SSE | ✅ Supported | MCP SDK 0.15.0 `HttpClientSseClientTransport` |
| stdio | ✅ Supported | MCP SDK 0.15.0 `StdioClientTransport` (local servers) |

## Two Testing Approaches

### Raw HTTP (Working)

Uses Katalon's built-in `WS` keywords with manual JSON-RPC construction. No external dependencies.

```groovy
def body = JsonOutput.toJson([
    jsonrpc: "2.0", id: 1, method: "initialize",
    params: [protocolVersion: "2024-11-05", capabilities: [:],
             clientInfo: [name: "katalon-mcp-test", version: "1.0.0"]]
])

RequestObject request = new RequestObject()
request.setRestUrl("https://remote.mcpservers.org/fetch/mcp")
request.setRestRequestMethod("POST")
// ... send with WS.sendRequest(request)
```

### MCP Java SDK (Working)

The official SDK (`io.modelcontextprotocol.sdk:mcp`) version 0.15.0 supports all transports:
- **Streamable HTTP**: `HttpClientStreamableHttpTransport`
- **SSE**: `HttpClientSseClientTransport`
- **stdio**: `StdioClientTransport`

**Note:** SDK 0.16.0+ requires `json-schema-validator:2.0.0` which conflicts with Katalon's bundled 1.5.7. Use 0.15.0.

## Troubleshooting

**MCP SDK `NoClassDefFoundError: Dialects`**

MCP SDK 0.16.0+ requires `json-schema-validator` 2.0.0+ which conflicts with Katalon's bundled version 1.5.7. Use SDK 0.15.0 which works without this conflict.

**400 Bad Request on subsequent calls**

Ensure you're extracting and sending the `mcp-session-id` header from the initialize response.

## Documentation

See [CLAUDE.md](CLAUDE.md) for detailed technical documentation including:
- Complete step definition patterns
- MCP protocol reference
- Troubleshooting guide
- Implementation details

## License

This project is licensed under the [Apache License 2.0](LICENSE).
