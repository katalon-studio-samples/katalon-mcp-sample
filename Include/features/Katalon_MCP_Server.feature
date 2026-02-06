@MCP @KatalonMCP
Feature: Katalon Public MCP Server API Testing
  As a test automation engineer
  I want to test the Katalon Public MCP Server
  So that I can verify it provides documentation search tools correctly

  Background:
    Given a Katalon MCP server at "https://mcp.katalon.com" with endpoint "/mcp"

  @Smoke
  Scenario: Successfully initialize MCP connection
    When I initialize the Katalon MCP client
    Then the Katalon MCP connection should be successful
    And the Katalon MCP server name should be "Katalon Public MCP Server"
    And the Katalon MCP protocol version should be "2024-11-05"

  @Smoke
  Scenario: Verify server capabilities
    When I initialize the Katalon MCP client
    Then the Katalon MCP server should support tools
    And the Katalon MCP server should support resources
    And the Katalon MCP server should support prompts

  @Tools
  Scenario: List available tools
    When I initialize the Katalon MCP client
    And I request the list of tools from Katalon MCP
    Then the Katalon MCP tools list should not be empty
    And the Katalon MCP tools list should contain a tool named "fetch"
    And the Katalon MCP tools list should contain a tool named "search"
    And the Katalon MCP tools list should contain a tool named "search_katalon_documentation_with_filter"

  @Tools
  Scenario: Verify fetch tool schema
    When I initialize the Katalon MCP client
    And I request the list of tools from Katalon MCP
    Then the Katalon MCP "fetch" tool should have a required parameter "id"

  @Tools
  Scenario: Verify search tool schema
    When I initialize the Katalon MCP client
    And I request the list of tools from Katalon MCP
    Then the Katalon MCP "search" tool should have a required parameter "query"

  @Tools
  Scenario: Verify search_katalon_documentation_with_filter tool schema
    When I initialize the Katalon MCP client
    And I request the list of tools from Katalon MCP
    Then the Katalon MCP "search_katalon_documentation_with_filter" tool should have a required parameter "query"
    And the Katalon MCP "search_katalon_documentation_with_filter" tool should have an optional parameter "products"
    And the Katalon MCP "search_katalon_documentation_with_filter" tool should have an optional parameter "max_results"

  @Integration
  Scenario: Execute search tool
    When I initialize the Katalon MCP client
    And I call the Katalon MCP "search" tool with query "Katalon Studio installation"
    Then the Katalon MCP tool call should be successful
    And the Katalon MCP tool result should contain search results

  @Integration
  Scenario: Execute filtered search tool
    When I initialize the Katalon MCP client
    And I call the Katalon MCP "search_katalon_documentation_with_filter" tool with query "test case" and product "katalon-studio"
    Then the Katalon MCP tool call should be successful
    And the Katalon MCP tool result should contain search results
