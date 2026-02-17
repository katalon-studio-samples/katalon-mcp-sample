/**
 * Run MCP BDD Tests
 * 
 * Executes the MCP Server Tools BDD test suite using Cucumber with the MCP Java SDK.
 * Tests MCP server functionality using Streamable HTTP transport including:
 * - Server initialization and capabilities
 * - Tool listing and validation
 * 
 * Test Feature: Include/features/MCP_Server_Tools.feature
 * Step Definitions: stepDefinitions.MCPServerSteps
 * Transport: Streamable HTTP
 */

import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW

// Set the glue code package for step definitions
CucumberKW.GLUE = ['stepDefinitions']

// Run the MCP Server Tools feature file
CucumberKW.runFeatureFile('Include/features/MCP_Server_Tools.feature')
