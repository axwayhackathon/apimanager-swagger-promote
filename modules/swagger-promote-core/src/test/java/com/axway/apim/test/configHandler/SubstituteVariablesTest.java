package com.axway.apim.test.configHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.axway.apim.lib.AppException;
import com.axway.apim.lib.EnvironmentProperties;
import com.axway.apim.lib.Parameters;
import com.axway.apim.lib.Parameters.ParameterEnum;
import com.axway.apim.swagger.api.state.IAPI;
import com.axway.apim.swagger.config.ConfigHandlerInterface;
import com.axway.apim.swagger.config.FileConfigHandler;

public class SubstituteVariablesTest {
	
	private static Logger LOG = LoggerFactory.getLogger(SubstituteVariablesTest.class);

	@BeforeClass
	private void initCommandParameters() {
		Map<ParameterEnum, Object> params = new HashMap<ParameterEnum, Object>();
		params.put(ParameterEnum.replaceHostInSwagger, "true");
		new Parameters(params);
	}
	
	@BeforeMethod
	public void cleanSingletons() {
		Parameters.getInstance().setEnvProperties(null);
	}

	@Test
	public void validateSystemOSAreSubstituted() throws AppException, IOException {
		String configFile = "/com/axway/apim/test/files/envProperties/1_config-with-os-variable.json";
		String pathToConfigFile = SubstituteVariablesTest.class.getResource(configFile).getFile();
		String swaggerFile = "/api_definition_1/petstore.json";
		String pathToSwaggerFile = SubstituteVariablesTest.class.getResource(swaggerFile).getFile();

		ConfigHandlerInterface configHandler = new FileConfigHandler(pathToConfigFile, pathToSwaggerFile, null, true);
		IAPI testAPI = configHandler.getApiConfig();

		if(System.getenv("TRAVIS")!=null && System.getenv("TRAVIS").equals("true")) {
			// At Travis an environment-variable USER exists which should have been replaced
			Assert.assertNotEquals(testAPI.getName(), "${USER}");
		} else {
			// On Windows use USERNAME in the version
			Assert.assertNotEquals(testAPI.getVersion(), "${USERNAME}");
		}
	}

	@Test
	public void validateBaseEnvReplacedOSAttribute() throws AppException, IOException {
		String configFile = "/com/axway/apim/test/files/envProperties/1_config-with-os-variable.json";
		String pathToConfigFile = SubstituteVariablesTest.class.getResource(configFile).getFile();
		String swaggerFile = "/api_definition_1/petstore.json";
		String pathToSwaggerFile = SubstituteVariablesTest.class.getResource(swaggerFile).getFile();
		
		// Simulate there is a System-Variable
		Properties props = System.getProperties();
		props.setProperty("OS_AND_MAIN_ENV_PROPERTY", "valueFromOS");
		
		// This should be replaced by what has been configured in env.properties
		EnvironmentProperties envProps = new EnvironmentProperties(null);
		Parameters.getInstance().setEnvProperties(envProps);

		ConfigHandlerInterface configHandler = new FileConfigHandler(pathToConfigFile, pathToSwaggerFile, null, true);
		IAPI testAPI = configHandler.getApiConfig();

		Assert.assertEquals(testAPI.getPath(), "valueFromMainEnv");
	}

	@Test
	public void validateStageEnvOveridesAll() throws AppException, IOException {
		String configFile = "/com/axway/apim/test/files/envProperties/1_config-with-os-variable.json";
		String pathToConfigFile = SubstituteVariablesTest.class.getResource(configFile).getFile();
		String swaggerFile = "/api_definition_1/petstore.json";
		String pathToSwaggerFile = SubstituteVariablesTest.class.getResource(swaggerFile).getFile();

		Properties props = System.getProperties();
		props.setProperty("OS_MAIN_AND_STAGE_ENV_PROPERTY", "valueFromOS");

		EnvironmentProperties envProps = new EnvironmentProperties("anyOtherStage");
		Parameters.getInstance().setEnvProperties(envProps);

		ConfigHandlerInterface configHandler = new FileConfigHandler(pathToConfigFile, pathToSwaggerFile, null, true);
		IAPI testAPI = configHandler.getApiConfig();

		Assert.assertEquals(testAPI.getOrganization(), "valueFromAnyOtherStageEnv");
	}

	@Test
	public void configFileWithSpaces() throws AppException, ParseException {
		try {
			String apiConfig = this.getClass().getResource("/com/axway/apim/test/files/basic/api config with spaces.json").getFile();
			String swagger = this.getClass().getResource("/api_definition_1/petstore.json").getFile();
			
			ConfigHandlerInterface configHandler = new FileConfigHandler(apiConfig, swagger, null, false);
			IAPI desiredAPI = configHandler.getApiConfig();
			
			
			Assert.assertEquals(desiredAPI.getVersion(), "${notDeclared}");
		} catch (Exception e) {
			LOG.error("Error running test: notDeclaredVariable", e);
			throw e;
		}
	}
	
	@Test
	public void withoutStage() throws AppException, ParseException {
		try {
			String apiConfig = this.getClass().getResource("/com/axway/apim/test/files/basic/api-config-with-variables.json").getFile();
			String swagger = this.getClass().getResource("/api_definition_1/petstore.json").getFile();
			
			EnvironmentProperties props = new EnvironmentProperties(null);
			Parameters.getInstance().setEnvProperties(props);
			
			// We have no environment given
			ConfigHandlerInterface fileConfigHandler = new FileConfigHandler(apiConfig, swagger, null, false);
			IAPI desiredAPI = fileConfigHandler.getApiConfig();
			
			Assert.assertNotNull(desiredAPI.getAPIDefinition(), "API Definition in desired API shouldn't be null");
			Assert.assertNotNull(desiredAPI.getAPIDefinition().getAPIDefinitionFile(), "getAPIDefinitionFile shouldn't be null");
			Assert.assertEquals(desiredAPI.getSummary(), "resolvedToSomething");
			
			
		} catch (Exception e) {
			LOG.error("Error running test: withoutStage", e);
			throw e;
		}
	}
	
	@Test
	public void withStage() throws AppException, ParseException {
		try {
			// With that, an environment: env.variabletest.properties is loaded!
			String apiConfig = this.getClass().getResource("/com/axway/apim/test/files/basic/api-config-with-variables.json").getFile();
			String swagger = this.getClass().getResource("/api_definition_1/petstore.json").getFile();
			
			EnvironmentProperties props = new EnvironmentProperties("variabletest");
			Parameters.getInstance().setEnvProperties(props);
			
			ConfigHandlerInterface configHandler = new FileConfigHandler(apiConfig, swagger, null, false);
			IAPI desiredAPI = configHandler.getApiConfig();
			Assert.assertEquals(desiredAPI.getSummary(), "resolvedToSomethingElse");
		} catch (Exception e) {
			LOG.error("Error running test: withStage", e);
			throw e;
		}
	}
	
	@Test
	public void notDeclaredVariable() throws AppException, ParseException {
		try {
			String apiConfig = this.getClass().getResource("/com/axway/apim/test/files/basic/api-config-with-variables.json").getFile();
			String swagger = this.getClass().getResource("/api_definition_1/petstore.json").getFile();
			
			ConfigHandlerInterface configHandler = new FileConfigHandler(apiConfig, swagger, null, false);
			IAPI desiredAPI = configHandler.getApiConfig();
			
			Assert.assertEquals(desiredAPI.getVersion(), "${notDeclared}");
		} catch (Exception e) {
			LOG.error("Error running test: notDeclaredVariable", e);
			throw e;
		}
	}
}
