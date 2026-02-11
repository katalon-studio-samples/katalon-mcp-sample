# Session Context

## User Prompts

### Prompt 1

Implement the following plan:

# Plan: Centralize MCP Server URLs in Object Repository

## Context

Network MCP server URLs are hardcoded in multiple test scripts and duplicated across regular and transparent proxy variants. The Object Repository already stores URLs for two servers (`Fetch MCP.rs`, `Katalon MCP.rs`) but they're only used by Raw HTTP tests. SDK-based tests (Streamable HTTP, SSE) hardcode their own URLs. This change centralizes all network MCP URLs in the Object Repository so ther...

### Prompt 2

commit these changes to a feature branch and push

