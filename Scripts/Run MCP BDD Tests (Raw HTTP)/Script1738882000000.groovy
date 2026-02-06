import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW

// Set the glue code package for step definitions
CucumberKW.GLUE = ['stepDefinitions']

// Run the MCP Server Tools (Raw HTTP) feature file
CucumberKW.runFeatureFile('Include/features/MCP_Server_Tools_RawHttp.feature')
