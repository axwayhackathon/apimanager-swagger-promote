package com.axway.apim.test.envProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.axway.apim.lib.AppException;
import com.axway.apim.lib.Parameters;
import com.axway.apim.lib.Parameters.ParameterEnum;
import com.axway.apim.lib.EnvironmentProperties;
import com.axway.apim.lib.Parameters;
import com.axway.apim.swagger.api.state.IAPI;
import com.axway.apim.swagger.config.ConfigHandlerInterface;
import com.axway.apim.swagger.config.FileConfigHandler;

public class SubstituteVariablesTest {

	@BeforeClass
	private void initCommandParameters() {
		Map<ParameterEnum, Object> params = new HashMap<ParameterEnum, Object>();
		params.put(ParameterEnum.replaceHostInSwagger, "true");
		new Parameters(params);
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

		Properties props = System.getProperties();
		props.setProperty("OS_AND_MAIN_ENV_PROPERTY", "valueFromOS");

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

}
