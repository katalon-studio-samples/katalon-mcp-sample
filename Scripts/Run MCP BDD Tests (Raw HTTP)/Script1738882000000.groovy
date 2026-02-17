/**
 * Run MCP BDD Tests (Raw HTTP)
 * 
 * Executes the MCP Server Tools BDD test suite using Cucumber with raw HTTP requests.
 * Tests MCP server functionality using JSON-RPC 2.0 protocol including:
 * - Server initialization and session management
 * - Server capabilities validation
 * - Tool listing and schema validation
 * 
 * Test Feature: Include/features/MCP_Server_Tools_RawHttp.feature
 * Step Definitions: stepDefinitions.MCPServerStepsRawHttp
 * Transport: Raw HTTP/JSON-RPC
 */

import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW

// Set the glue code package for step definitions
CucumberKW.GLUE = ['stepDefinitions']

// Run the MCP Server Tools (Raw HTTP) feature file
CucumberKW.runFeatureFile('Include/features/MCP_Server_Tools_RawHttp.feature')
