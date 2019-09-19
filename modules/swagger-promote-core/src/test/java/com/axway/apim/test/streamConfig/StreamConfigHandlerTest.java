package com.axway.apim.test.streamConfig;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.axway.apim.lib.ErrorState;
import com.axway.apim.lib.Parameters;
import com.axway.apim.swagger.api.state.DesiredAPI;
import com.axway.apim.swagger.config.StreamConfigHandler;

public class StreamConfigHandlerTest {

	private static Logger LOG = LoggerFactory.getLogger(StreamConfigHandlerTest.class);
	
	@BeforeMethod
	public void cleanSingletons() {
		LOG.info("Deleting singletons before exuecting test.");
		ErrorState.deleteInstance();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("host", "not-used");
		params.put("username", "not-used");
		params.put("password", "not-used");
		new Parameters(params);
	}
	
	@Test
	public void testStreamConfigHandler() throws Exception {
		try {
			String apiConfig = this.getClass().getResource("/com/axway/apim/test/files/basic/api-config-with-variables.json").getFile();
			String swagger = this.getClass().getResource("/api_definition_1/petstore.json").getFile();
			StreamConfigHandler configHandler = new StreamConfigHandler(new FileInputStream(apiConfig), new FileInputStream(swagger), null, false);
			
			DesiredAPI desiredAPI = configHandler.getApiConfig();
			
			Assert.assertNotNull(desiredAPI.getAPIDefinition(), "API Definition in desired API shouldn't be null");
			Assert.assertNotNull(desiredAPI.getAPIDefinition().getAPIDefinitionFile(), "getAPIDefinitionFile shouldn't be null");
		} catch (Exception e) {
			LOG.error("Error running test: withoutStage", e);
			throw e;
		}
	}
}
