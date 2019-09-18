package com.axway.apim.swagger.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.APIImportMain;
import com.axway.apim.lib.AppException;
import com.axway.apim.lib.ErrorCode;
import com.axway.apim.lib.ErrorState;
import com.axway.apim.lib.Parameters;
import com.axway.apim.swagger.api.properties.quota.QuotaRestriction;
import com.axway.apim.swagger.api.properties.quota.QuotaRestrictionDeserializer;
import com.axway.apim.swagger.api.state.DesiredAPI;
import com.axway.apim.swagger.api.state.IAPI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class FileConfigHandler {
	
	private static Logger LOG = LoggerFactory.getLogger(FileConfigHandler.class);
	
	private ErrorState error = ErrorState.getInstance();
	
	private ObjectMapper mapper = new ObjectMapper();
	
	private String pathToAPIDefinition;
	
	private String apiConfigFile;
	
	private String stage;

	public FileConfigHandler(String apiConfigFile, String stage, String pathToAPIDefinition, boolean usingOrgAdmin) {
		super();
		this.apiConfigFile = apiConfigFile;
		this.stage = stage;
		this.pathToAPIDefinition = pathToAPIDefinition;
	}
	
	public APIConfig getConfig() throws AppException {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(QuotaRestriction.class, new QuotaRestrictionDeserializer());
		mapper.registerModule(module);
		APIConfig config = new APIConfig();
		try {
			File configFile = new File(locateAPIConfigFile(apiConfigFile));
			String apiConfig;
			try {
				apiConfig = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
			} catch (Exception e) {
				error.setError("Cant parse JSON-Config file(s)", ErrorCode.CANT_READ_CONFIG_FILE);
				throw new AppException("Cant parse JSON-Config file(s)", ErrorCode.CANT_READ_CONFIG_FILE, e);
			}
			try {
				apiConfig = substitueVariables(apiConfig);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			config.setApiConfig(mapper.readValue(apiConfig, DesiredAPI.class));
			String stagedConfig = getStageConfig(stage, configFile);
			if(stagedConfig!=null) {
				try {
					stagedConfig = substitueVariables(stagedConfig);
					ObjectReader updater = mapper.readerForUpdating(config.getApiConfig());
					config.setApiConfig(updater.readValue(stagedConfig));
					LOG.info("Loaded stage API-Config from file: " + stage);
				} catch (FileNotFoundException e) {
					LOG.debug("No config file found for stage: '"+stage+"'");
				}
			}
		} catch (Exception e) {
			error.setError("Cant parse JSON-Config file(s)", ErrorCode.CANT_READ_CONFIG_FILE);
			throw new AppException("Cant parse JSON-Config file(s)", ErrorCode.CANT_READ_CONFIG_FILE, e);
		}
		checkForAPIDefinitionInConfiguration(config.getApiConfig());
		return config;
	}
	
	private static String locateAPIConfigFile(String apiConfigFile) throws AppException {
		try {
			apiConfigFile = URLDecoder.decode(apiConfigFile, "UTF-8");
			File configFile = new File(apiConfigFile);
			if(configFile.exists()) return configFile.getCanonicalPath();
			// This is mainly to load the samples sitting inside the package!
			String installFolder = new File(APIImportMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getParent();
			configFile = new File(installFolder + File.separator + apiConfigFile);
			if(configFile.exists()) return configFile.getCanonicalPath();
			throw new AppException("Unable to find given Config-File: '"+apiConfigFile+"'", ErrorCode.CANT_READ_CONFIG_FILE);
		} catch (Exception e) {
			throw new AppException("Unable to find given Config-File: '"+apiConfigFile+"'", ErrorCode.CANT_READ_CONFIG_FILE);
		}
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
	
	private String getStageConfig(String stage, File apiConfigFile) throws IOException {
		if(stage == null) return null;
		File stageFile = new File(stage);
		if(stageFile.exists()) { // This is to support testing with dynamically created files!
			return stageFile.getAbsolutePath();
		}
		if(!stage.equals("NOT_SET")) {
			String apiConfigFileName = apiConfigFile.getCanonicalPath();
			stageFile = new File(apiConfigFileName.substring(0, apiConfigFileName.lastIndexOf(".")+1) + stage + apiConfigFileName.substring(apiConfigFileName.lastIndexOf(".")));
			if(stageFile.exists()) {
				return stageFile.getAbsolutePath();
			} else {
				return null;
			}
		}
		LOG.debug("No stage provided");
		return null;
	}
	
	private void checkForAPIDefinitionInConfiguration(IAPI apiConfig) throws AppException {
		String path = getCurrentPath();
		LOG.debug("Current path={}",path);
		if (StringUtils.isEmpty(this.pathToAPIDefinition)) {
			if (StringUtils.isNotEmpty(apiConfig.getApiDefinitionImport())) {
				this.pathToAPIDefinition=apiConfig.getApiDefinitionImport();
				LOG.debug("Reading API Definition from configuration file");
			} else {
				ErrorState.getInstance().setError("No API Definition configured", ErrorCode.NO_API_DEFINITION_CONFIGURED, false);
				throw new AppException("No API Definition configured", ErrorCode.NO_API_DEFINITION_CONFIGURED);
			}
		}
	}
	
	private String getCurrentPath() {
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		return s;
	}
}
