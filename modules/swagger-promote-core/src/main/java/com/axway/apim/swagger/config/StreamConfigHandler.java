package com.axway.apim.swagger.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.axway.apim.lib.AppException;
import com.axway.apim.lib.ErrorCode;
import com.axway.apim.lib.ErrorState;
import com.axway.apim.lib.Parameters;
import com.axway.apim.swagger.api.properties.quota.QuotaRestriction;
import com.axway.apim.swagger.api.properties.quota.QuotaRestrictionDeserializer;
import com.axway.apim.swagger.api.state.DesiredAPI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamConfigHandler extends AbstractConfigHandler implements ConfigHandlerInterface {
	
	private static Logger LOG = LoggerFactory.getLogger(FileConfigHandler.class);

	private ErrorState error = ErrorState.getInstance();

	private ObjectMapper mapper = new ObjectMapper();
	
	private APIConfig apiImportCfg;
	
	public StreamConfigHandler(InputStream apiConfig, InputStream apiDefinition, String stage, boolean orgAdminUsed) throws AppException {
		super(apiConfig, apiDefinition, stage, orgAdminUsed);
		SimpleModule module = new SimpleModule();
		module.addDeserializer(QuotaRestriction.class, new QuotaRestrictionDeserializer());
		mapper.registerModule(module);
		apiImportCfg = new APIConfig();
		try {
			String apiConfigContent;
			try {
                apiConfigContent = IOUtils.toString(apiConfig, StandardCharsets.UTF_8);
			} catch (Exception e) {
				error.setError("Cant parse JSON-Config file(s)", ErrorCode.CANT_READ_CONFIG_FILE);
				throw new AppException("Cant parse JSON-Config file(s)", ErrorCode.CANT_READ_CONFIG_FILE, e);
			}
			try {
				apiConfigContent = substitueVariables(apiConfigContent);
			} catch (IOException e) {
				e.printStackTrace();
			}

			apiImportCfg.setApiConfig(mapper.readValue(apiConfigContent, DesiredAPI.class));
		} catch (Exception e) {
			error.setError("Cant parse JSON-Config file(s)", ErrorCode.CANT_READ_CONFIG_FILE);
			throw new AppException("Cant parse JSON-Config file(s)", ErrorCode.CANT_READ_CONFIG_FILE, e);
		}

	}

	
	public APIConfig getConfig() throws AppException {
		apiImportCfg.setApiDefinition(getAPIDefinitionContent());
		return apiImportCfg;
    }

    /**
	 * This method is replacing variables such as ${TokenEndpoint} with declared variables coming from either 
	 * the Environment-Variables or from system-properties.
	 * @param inputFile The API-Config file to be replaced and returned as String
	 * @return a String representation of the API-Config-File
	 * @throws IOException if the file can't be found
	 */
	private String substitueVariables(String apiConfig) throws IOException {
		StringSubstitutor substitutor = new StringSubstitutor(Parameters.getInstance().getEnvironmentProperties());
		String givenConfig = StringSubstitutor.replace(apiConfig, System.getenv());
		return substitutor.replace(givenConfig);
	}

	private String getAPIDefinitionContent() throws AppException {
		try {
			return IOUtils.toString((InputStream)this.apiDefinition, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AppException("Can't read API-Definition from file", ErrorCode.CANT_READ_API_DEFINITION_FILE, e);
		}
	}
}
