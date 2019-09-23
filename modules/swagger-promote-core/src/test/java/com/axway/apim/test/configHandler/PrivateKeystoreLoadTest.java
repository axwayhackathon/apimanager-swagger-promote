package com.axway.apim.test.configHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.axway.apim.lib.AppException;
import com.axway.apim.lib.EnvironmentProperties;
import com.axway.apim.lib.Parameters;
import com.axway.apim.lib.Parameters.ParameterEnum;
import com.axway.apim.swagger.api.properties.authenticationProfiles.AuthType;
import com.axway.apim.swagger.api.state.IAPI;
import com.axway.apim.swagger.config.ConfigHandlerInterface;
import com.axway.apim.swagger.config.FileConfigHandler;

public class PrivateKeystoreLoadTest {

	Map<String, Object> parameters = new HashMap<>();

	EnvironmentProperties env = new EnvironmentProperties();

	@BeforeClass
	private void initTestIndicator() {
		Map<ParameterEnum, Object> params = new HashMap<ParameterEnum, Object>();
		new Parameters(params);
	}

	@BeforeMethod(alwaysRun = true)
	private void setupAuthenticationProfile() throws AppException {
		parameters.put("source", "file");
		parameters.put("trustAll", "true");
		parameters.put("password", "axway");
		parameters.put("certFile", "/com/axway/apim/test/files/certificates/clientcert.pfx");
		// Set some environment variable to be replaced in the config file, which is dynamic!
		env.putMainProperties(parameters);
		new Parameters(new HashMap<>()).setEnvProperties(env);
	}

	@Test
	public void testWorkingKeystoreFile() throws AppException, IOException {
		String apiConfig = this.getClass().getResource("/com/axway/apim/test/files/security/api_outbound-ssl.json").getFile();
		String swagger = this.getClass().getResource("/api_definition_1/petstore.json").getFile();
		ConfigHandlerInterface configHandler = new FileConfigHandler(apiConfig, swagger, null, true);
		IAPI testAPI = configHandler.getApiConfig();
		configHandler.getX509Certificate(testAPI.getAuthenticationProfiles().get(0));
		Assert.assertEquals(testAPI.getAuthenticationProfiles().get(0).getType(), AuthType.ssl);
		Assert.assertEquals(testAPI.getAuthenticationProfiles().get(0).getParameters().get("password"), "axway");
		// Assert.assertNotNull(testAPI.getAuthenticationProfiles().get(0).getParameters().get("pks"));
	}

	@Test
	public void testInvalidPasswordKeystoreFile() throws AppException, IOException {
		String apiConfig = this.getClass().getResource("/com/axway/apim/test/files/security/api_outbound-ssl.json").getFile();
		String swagger = this.getClass().getResource("/api_definition_1/petstore.json").getFile();
		parameters.put("password", "thatswrong");
		env.putMainProperties(parameters);
		new Parameters(new HashMap<>()).setEnvProperties(env);
		ConfigHandlerInterface configHandler = new FileConfigHandler(apiConfig, swagger, null, true);

		try {
			IAPI testAPI = configHandler.getApiConfig();
			configHandler.getX509Certificate(testAPI.getAuthenticationProfiles().get(0));
			// TODO: How to handle this bug.
			// With the PKCS12KeyStore, BadPaddingException is raised rather than a message saying that the password is incorrect
			// java.io.IOException: failed to decrypt safe contents entry: javax.crypto.BadPaddingException: Given final block not properly padded
			// https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6415637
			Assert.fail("Test must fail due to a wrong keystore password");
		} catch(AppException e) {
			Assert.assertTrue(e.getMessage().contains("keystore password was incorrect"),
					"Expected: 'keystore password was incorrect' vs. Actual: '" + e.getMessage()+"'");
		}
	}

	@Test
	public void testInvalidKeystoreType() throws AppException, IOException {
		String apiConfig = this.getClass().getResource("/com/axway/apim/test/files/security/api_outbound-ssl.json").getFile();
		String swagger = this.getClass().getResource("/api_definition_1/petstore.json").getFile();
		parameters.put("certFile", "/com/axway/apim/test/files/certificates/clientcert.pfx:ABC");
		env.putMainProperties(parameters);
		new Parameters(new HashMap<>()).setEnvProperties(env);
		// APIImportConfigAdapter configAdapter = new APIImportConfigAdapter(new FileConfigHandler(apiConfig, swagger, null, true));
		ConfigHandlerInterface configHandler = new FileConfigHandler(apiConfig, swagger, null, true);
		try {
			IAPI testAPI = configHandler.getApiConfig();
			configHandler.getX509Certificate(testAPI.getAuthenticationProfiles().get(0));
			Assert.fail("Test must fail as it points to an unknown keystore");
		} catch(AppException e) {
			Assert.assertTrue(e.getMessage().contains("Unknown keystore type: 'ABC'."), 
					"Expected: 'Unknown keystore type: 'ABC'.' vs. Actual: '" + e.getMessage()+"'");
		}
	}

	@Test
	public void testvalidKeystorePKCSD12Type() throws AppException, IOException {
		String apiConfig = this.getClass().getResource("/com/axway/apim/test/files/security/api_outbound-ssl.json").getFile();
		String swagger = this.getClass().getResource("/api_definition_1/petstore.json").getFile();
		parameters.put("certFile", "/com/axway/apim/test/files/certificates/clientcert.pfx:PKCS12");
		env.putMainProperties(parameters);
		new Parameters(new HashMap<>()).setEnvProperties(env);
		ConfigHandlerInterface configHandler = new FileConfigHandler(apiConfig, swagger, null, true);

		IAPI testAPI = configHandler.getApiConfig();
		configHandler.getX509Certificate(testAPI.getAuthenticationProfiles().get(0));
		Assert.assertEquals(testAPI.getAuthenticationProfiles().get(0).getType(), AuthType.ssl);
		Assert.assertEquals(testAPI.getAuthenticationProfiles().get(0).getParameters().get("password"), "axway");
		// Assert.assertNotNull(testAPI.getAuthenticationProfiles().get(0).getParameters().get("pks"));
	}
}
