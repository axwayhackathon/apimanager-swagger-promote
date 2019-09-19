package com.axway.apim.test.basic;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.axway.apim.lib.AppException;
import com.axway.apim.swagger.APIManagerAdapter;
import com.axway.apim.swagger.config.FileConfigHandler;
import com.fasterxml.jackson.databind.JsonNode;

public class APIImportConfigTest {

  @Test
  public void testHttpUriCheck() throws AppException, IOException {
	  String httpUri1 = "https://petstore.swagger.io/v2/swagger.json";
	  String httpUri2 = "user/password@https://petstore.swagger.io/v2/swagger.json";
	  String httpUri3 = "user/password";
	  String httpUri4 = "user/password@";
	  
	  Assert.assertEquals(FileConfigHandler.isHttpUri(httpUri1), true);
	  Assert.assertEquals(FileConfigHandler.isHttpUri(httpUri2), true); 
	  Assert.assertEquals(FileConfigHandler.isHttpUri(httpUri3), false); 
	  Assert.assertEquals(FileConfigHandler.isHttpUri(httpUri4), false);
	  String fileToTest = "com/axway/apim/test/files/customproperties/app-1.config";
	  String appConfigContent = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResource(fileToTest), "UTF-8");
	  JsonNode jsonNode = APIManagerAdapter.parseAppConfig(appConfigContent);
	  JsonNode apiProperties = jsonNode.get("api");
	  Assert.assertEquals(apiProperties.at("/customProperty1/label").asText(), "Custom Property #1");
	  Assert.assertEquals(apiProperties.at("/customProperty2/type").asText(), "select");
	  Assert.assertEquals(apiProperties.at("/customProperty3/type").asText(), "switch");
  }
}
