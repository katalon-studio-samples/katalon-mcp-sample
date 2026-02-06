import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW

// Set the glue code package for step definitions
CucumberKW.GLUE = ['stepDefinitions']

// Run the Katalon MCP Server feature file
CucumberKW.runFeatureFile('Include/features/Katalon_MCP_Server.feature')
