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

**Raw HTTP approach (recommended):**
- Test Case: `Test Cases/MCP Server Tools Test (Raw HTTP)`
- BDD: `Test Cases/Run MCP BDD Tests (Raw HTTP)`
- Suite: `Test Suites/MCP Server Test Suite - Raw`

**MCP SDK approach (has library conflicts - see Troubleshooting):**
- Test Case: `Test Cases/MCP Server Tools Test`
- BDD: `Test Cases/Run MCP BDD Tests`
- Suite: `Test Suites/MCP Server Test Suite`

## Test Coverage

Tests verify the `mcp-fetch` server at `https://remote.mcpservers.org/fetch/mcp`:

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

### MCP Java SDK (Library Conflicts)

Uses the official SDK (`io.modelcontextprotocol.sdk:mcp:0.17.2`). Currently blocked by a dependency conflict between the SDK's `json-schema-validator` 2.0.0+ and Katalon's bundled 1.5.7.

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
