/**
 * Run Katalon MCP BDD Tests
 * 
 * Executes the Katalon MCP Server BDD test suite using Cucumber.
 * Tests the Katalon Public MCP Server functionality including:
 * - Server initialization and capabilities
 * - Tool listing and execution
 * - Search operations
 * 
 * Test Feature: Include/features/Katalon_MCP_Server.feature
 * Step Definitions: stepDefinitions.KatalonMCPServerSteps
 */

import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW

// Set the glue code package for step definitions
CucumberKW.GLUE = ['stepDefinitions']

// Run the Katalon MCP Server feature file
CucumberKW.runFeatureFile('Include/features/Katalon_MCP_Server.feature')
