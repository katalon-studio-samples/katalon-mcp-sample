# Katalon MCP Sample

A Katalon Studio project demonstrating how to test [MCP (Model Context Protocol)](https://modelcontextprotocol.io/) servers using both raw HTTP requests and the official MCP Java SDK.

## What This Project Shows

- Testing MCP servers with Katalon's native WS keywords (no external dependencies)
- Testing MCP servers with the official MCP Java SDK
- BDD/Cucumber test structure for MCP server validation
- Handling MCP protocol specifics: JSON-RPC 2.0, SSE responses, session management

## Quick Start

### Prerequisites

- Katalon Studio 10.4.2+ (Enterprise edition)
- Java 17

### Setup

1. Open the project in Katalon Studio
2. (Optional) Install SDK dependencies:
   ```bash
   ./gradlew clean katalonCopyDependencies
   ```

### Run Tests

**Katalon MCP Server (recommended):**
- Test Case: `Test Cases/Katalon MCP Server Test`
- BDD: `Test Cases/Run Katalon MCP BDD Tests`
- Suite: `Test Suites/Katalon MCP Server Test Suite`

**Raw HTTP approach (mcp-fetch server):**
- Test Case: `Test Cases/MCP Server Tools Test (Raw HTTP)`
- BDD: `Test Cases/Run MCP BDD Tests (Raw HTTP)`
- Suite: `Test Suites/MCP Server Test Suite - Raw`

**MCP SDK approach (blocked - see Troubleshooting):**
- Test Case: `Test Cases/MCP Server Tools Test`
- Suite: `Test Suites/MCP Server Test Suite`

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
├── CLAUDE.md                # Detailed technical documentation
└── README.md                # This file
```

## Supported MCP Transports

| Transport | Status | Notes |
|-----------|--------|-------|
| Streamable HTTP | ✅ Supported | Raw HTTP approach (no SDK needed) |
| SSE | ✅ Supported | MCP SDK 0.15.0 with `HttpClientSseClientTransport` |
| stdio | 🚧 SDK Ready | MCP SDK 0.15.0 has `StdioClientTransport` |

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

### MCP Java SDK (SSE Working, Streamable HTTP Blocked)

The official SDK (`io.modelcontextprotocol.sdk:mcp`) has version constraints:
- **0.15.0** (current): SSE and stdio transports work. No json-schema-validator conflict.
- **0.16.0+**: Requires `json-schema-validator:2.0.0` which conflicts with Katalon's bundled 1.5.7.

Use Raw HTTP for Streamable HTTP servers until Katalon upgrades its bundled dependencies.

## Troubleshooting

**MCP SDK `NoClassDefFoundError: Dialects`**

The MCP SDK requires `json-schema-validator` 2.0.0+ which conflicts with Katalon's bundled version. Use the Raw HTTP approach as a workaround.

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
