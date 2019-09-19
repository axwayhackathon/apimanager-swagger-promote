package com.axway.apim.test.basic;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.axway.apim.lib.AppException;
import com.axway.apim.lib.EnvironmentProperties;
import com.axway.apim.lib.ErrorState;
import com.axway.apim.lib.Parameters;
import com.axway.apim.swagger.api.state.IAPI;
import com.axway.apim.swagger.config.ConfigHandlerInterface;
import com.axway.apim.swagger.config.FileConfigHandler;

public class FileConfigHandlerTest {

	private static Logger LOG = LoggerFactory.getLogger(FileConfigHandlerTest.class);
	
	@BeforeMethod
	public void cleanSingletons() {
		LOG.info("Deleting singletons before exuecting test.");
		ErrorState.deleteInstance();
	}
	
	@Test
	public void withoutStage() throws AppException, ParseException {
		try {
			String apiConfig = this.getClass().getResource("/com/axway/apim/test/files/basic/api-config-with-variables.json").getFile();
			String swagger = this.getClass().getResource("/api_definition_1/petstore.json").getFile();
			
			// We have no environment given
			EnvironmentProperties props = new EnvironmentProperties(null);
			CommandLine cmd = new DefaultParser().parse(new Options(), new String[]{});
			new Parameters(cmd, null, props);
			ConfigHandlerInterface configHandler = new FileConfigHandler(apiConfig, swagger, null, false);
			IAPI desiredAPI = configHandler.getApiConfig();
			
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
			EnvironmentProperties props = new EnvironmentProperties("variabletest");
			CommandLine cmd = new DefaultParser().parse(new Options(), new String[]{});
			new Parameters(cmd, null, props);
			String apiConfig = this.getClass().getResource("/com/axway/apim/test/files/basic/api-config-with-variables.json").getFile();
			String swagger = this.getClass().getResource("/api_definition_1/petstore.json").getFile();
			
			ConfigHandlerInterface configHandler = new FileConfigHandler(apiConfig, swagger, null, false);
			IAPI desiredAPI = configHandler.getApiConfig();
			Assert.assertEquals(desiredAPI.getSummary(), "resolvedToSomethingElse");
		} catch (Exception e) {
			LOG.error("Error running test: withStage", e);
			throw e;
		}
	}
	
	@Test
	public void usingOSEnvVariable() throws AppException, ParseException {
		try {
			// With that, NO stage-environment is loaded
			EnvironmentProperties props = new EnvironmentProperties(null);
			CommandLine cmd = new DefaultParser().parse(new Options(), new String[]{});
			new Parameters(cmd, null, props);
			String apiConfig = this.getClass().getResource("/com/axway/apim/test/files/basic/api-config-with-variables.json").getFile();
			String swagger = this.getClass().getResource("/api_definition_1/petstore.json").getFile();
			
			
			ConfigHandlerInterface configHandler = new FileConfigHandler(apiConfig, swagger, null, false);
			IAPI desiredAPI = configHandler.getApiConfig();
			
			String osArch = System.getProperty("os.arch");
			Assert.assertEquals(desiredAPI.getOrganization(), "API Development "+osArch);
		} catch (Exception e) {
			LOG.error("Error running test: usingOSEnvVariable", e);
			throw e;
		}
	}
	
	@Test
	public void notDeclaredVariable() throws AppException, ParseException {
		try {
			EnvironmentProperties props = new EnvironmentProperties(null);
			CommandLine cmd = new DefaultParser().parse(new Options(), new String[]{});
			new Parameters(cmd, null, props);
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
	
	@Test
	public void configFileWithSpaces() throws AppException, ParseException {
		try {
			EnvironmentProperties props = new EnvironmentProperties(null);
			CommandLine cmd = new DefaultParser().parse(new Options(), new String[]{});
			new Parameters(cmd, null, props);
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
}
