/**
 * Script: Run MCP BDD Tests
 * 
 * Description:
 *   Runs BDD feature tests for MCP server tools discovery using the MCP SDK.
 * 
 * Test Approach:
 *   BDD/Cucumber - Executes feature file with step definitions using MCP Java SDK.
 * 
 * Feature File:
 *   Include/features/MCP_Server_Tools.feature
 * 
 * Step Definitions:
 *   Include/scripts/groovy/stepDefinitions/MCPServerSteps.groovy
 * 
 * Prerequisites:
 *   - MCP SDK installed: ./gradlew clean katalonCopyDependencies
 */

import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW

// Set the glue code package for step definitions
CucumberKW.GLUE = ['stepDefinitions']

// Run the MCP Server Tools feature file
CucumberKW.runFeatureFile('Include/features/MCP_Server_Tools.feature')
