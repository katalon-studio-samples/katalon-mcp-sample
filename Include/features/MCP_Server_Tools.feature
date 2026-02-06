@MCP
Feature: MCP Server Tools Discovery
  As a test automation engineer
  I want to verify that the MCP server returns valid tools
  So that I can ensure the server is functioning correctly

  Background:
    Given an MCP server at "https://remote.mcpservers.org" with endpoint "/fetch/mcp"

  Scenario: Successfully connect to MCP server
    When I initialize the MCP client
    Then the connection should be successful
    And the server name should be "mcp-fetch"
    And the protocol version should be "2024-11-05"

  Scenario: Verify server capabilities
    When I initialize the MCP client
    Then the server should support tools
    And the server should not support resources
    And the server should not support prompts

  Scenario: List available tools
    When I initialize the MCP client
    And I request the list of tools
    Then the tools list should not be empty
    And the tools list should contain a tool named "fetch"

  Scenario: Verify fetch tool schema
    When I initialize the MCP client
    And I request the list of tools
    Then the "fetch" tool should have a required parameter "url"
    And the "fetch" tool should have an optional parameter "max_length"
    And the "fetch" tool should have an optional parameter "raw"
