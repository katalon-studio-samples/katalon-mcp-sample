@MCP @RawHTTP
Feature: MCP Server Tools Discovery (Raw HTTP)
  As a test automation engineer
  I want to verify that the MCP server returns valid tools using raw HTTP
  So that I can ensure the server is functioning correctly without SDK dependencies

  Background:
    Given a raw HTTP MCP server at "https://remote.mcpservers.org" with endpoint "/fetch/mcp"

  Scenario: Successfully connect to MCP server via raw HTTP
    When I initialize the MCP client via raw HTTP
    Then the raw HTTP connection should be successful
    And the raw HTTP server name should be "mcp-fetch"
    And the raw HTTP protocol version should be "2024-11-05"

  Scenario: Verify server capabilities via raw HTTP
    When I initialize the MCP client via raw HTTP
    Then the raw HTTP server should support tools
    And the raw HTTP server should not support resources
    And the raw HTTP server should not support prompts

  Scenario: List available tools via raw HTTP
    When I initialize the MCP client via raw HTTP
    And I request the list of tools via raw HTTP
    Then the raw HTTP tools list should not be empty
    And the raw HTTP tools list should contain a tool named "fetch"

  Scenario: Verify fetch tool schema via raw HTTP
    When I initialize the MCP client via raw HTTP
    And I request the list of tools via raw HTTP
    Then the raw HTTP "fetch" tool should have a required parameter "url"
    And the raw HTTP "fetch" tool should have an optional parameter "max_length"
    And the raw HTTP "fetch" tool should have an optional parameter "raw"
