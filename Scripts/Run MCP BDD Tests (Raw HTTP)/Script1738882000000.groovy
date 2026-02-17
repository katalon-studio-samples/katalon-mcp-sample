/**
 * Script: Run MCP BDD Tests (Raw HTTP)
 * 
 * Description:
 *   Runs BDD feature tests for MCP server using raw HTTP (no SDK dependencies).
 * 
 * Test Approach:
 *   BDD/Cucumber - Executes feature file with step definitions using raw HTTP/JSON-RPC.
 * 
 * Feature File:
 *   Include/features/MCP_Server_Tools_RawHttp.feature
 * 
 * Step Definitions:
 *   Include/scripts/groovy/stepDefinitions/MCPServerStepsRawHttp.groovy
 * 
 * Target Server:
 *   mcp-fetch server (URL configured in Object Repository)
 */

import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW

// Set the glue code package for step definitions
CucumberKW.GLUE = ['stepDefinitions']

// Run the MCP Server Tools (Raw HTTP) feature file
CucumberKW.runFeatureFile('Include/features/MCP_Server_Tools_RawHttp.feature')
