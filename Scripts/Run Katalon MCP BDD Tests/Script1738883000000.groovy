/**
 * Script: Run Katalon MCP BDD Tests
 * 
 * Description:
 *   Runs BDD tests for the Katalon Public MCP Server using Streamable HTTP transport.
 * 
 * Test Approach:
 *   BDD/Cucumber - Executes feature file with step definitions for MCP server validation.
 * 
 * Feature File:
 *   Include/features/Katalon_MCP_Server.feature
 * 
 * Step Definitions:
 *   Include/scripts/groovy/stepDefinitions/KatalonMCPServerSteps.groovy
 * 
 * Target Server:
 *   https://mcp.katalon.com/mcp
 */

import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW

// Set the glue code package for step definitions
CucumberKW.GLUE = ['stepDefinitions']

// Run the Katalon MCP Server feature file
CucumberKW.runFeatureFile('Include/features/Katalon_MCP_Server.feature')
