package com.axway.apim.swagger.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.APIImportMain;
import com.axway.apim.lib.AppException;
import com.axway.apim.lib.ErrorCode;
import com.axway.apim.lib.ErrorState;
import com.axway.apim.lib.Parameters;
import com.axway.apim.lib.Utils;
import com.axway.apim.swagger.APIManagerAdapter;
import com.axway.apim.swagger.api.properties.authenticationProfiles.AuthType;
import com.axway.apim.swagger.api.properties.authenticationProfiles.AuthenticationProfile;
import com.axway.apim.swagger.api.properties.cacerts.CaCert;
import com.axway.apim.swagger.api.properties.quota.QuotaRestriction;
import com.axway.apim.swagger.api.properties.quota.QuotaRestrictionDeserializer;
import com.axway.apim.swagger.api.state.DesiredAPI;
import com.axway.apim.swagger.api.state.DesiredTestOnlyAPI;
import com.axway.apim.swagger.api.state.IAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class FileConfigHandler extends AbstractConfigHandler implements ConfigHandlerInterface {
	
	private static Logger LOG = LoggerFactory.getLogger(FileConfigHandler.class);
	
	private ErrorState error = ErrorState.getInstance();
	
	private ObjectMapper mapper = new ObjectMapper();
	
	public FileConfigHandler(Object APIDefinition, Object apiConfig, String stage, boolean orgAdminUsed) {
		super(APIDefinition, apiConfig, stage, orgAdminUsed);
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
	
	private InputStream getInputStreamForCertFile(CaCert cert) throws AppException {
		InputStream is;
		File file;
		// Certificates might be stored somewhere else, so try to load them directly
		file = new File(cert.getCertFile());
		if(file.exists()) { 
			try {
				is = new FileInputStream(file);
				return is;
			} catch (FileNotFoundException e) {
				throw new AppException("Cant read given certificate file", ErrorCode.CANT_READ_CONFIG_FILE);
			}
		}
		String baseDir;
		try {
			baseDir = new File(config.get).getCanonicalFile().getParent();
		} catch (IOException e1) {
			error.setError("Can't read certificate file.", ErrorCode.CANT_READ_CONFIG_FILE);
			throw new AppException("Can't read certificate file.", ErrorCode.CANT_READ_CONFIG_FILE, e1);
		}
		file = new File(baseDir + File.separator + cert.getCertFile());
		if(file.exists()) { 
			try {
				is = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				throw new AppException("Cant read given certificate file", ErrorCode.CANT_READ_CONFIG_FILE);
			}
		} else {
			LOG.debug("Can't read certifiate from file-location: " + file.toString() + ". Now trying to read it from the classpath.");
			// Try to read it from classpath
			is = APIManagerAdapter.class.getResourceAsStream(cert.getCertFile()); 
		}
		if(is==null) {
			LOG.error("Can't read certificate: "+cert.getCertFile()+" from file or classpath.");
			LOG.error("Certificates in filesystem are either expected relative to the API-Config-File or as an absolute path.");
			LOG.error("In the same directory. 		Example: \"myCertFile.crt\"");
			LOG.error("Relative to it.         		Example: \"../../allMyCertsAreHere/myCertFile.crt\"");
			LOG.error("With an absolute path   		Example: \"/another/location/with/allMyCerts/myCertFile.crt\"");
			throw new AppException("Can't read certificate: "+cert.getCertFile()+" from file or classpath.", ErrorCode.CANT_READ_CONFIG_FILE);
		}
		return is;
	}
	
	private byte[] getAPIDefinitionContent() throws AppException {
		try {
			InputStream stream = getAPIDefinitionAsStream();
			Reader reader = new InputStreamReader(stream,StandardCharsets.UTF_8);
			return IOUtils.toByteArray(reader,StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AppException("Can't read API-Definition from file", ErrorCode.CANT_READ_API_DEFINITION_FILE, e);
		}
	}
	
	/**
	 * To make testing easier we allow reading test-files from classpath as well
	 * @throws AppException when the import Swagger-File can't be read.
	 * @return The import Swagger-File as an InputStream
	 */
	public InputStream getAPIDefinitionAsStream() throws AppException {
		InputStream is = null;
		if(APIDefinition.endsWith(".url")) {
			return getAPIDefinitionFromURL(Utils.getAPIDefinitionUriFromFile(APIDefinition));
		} else if(isHttpUri(APIDefinition)) {
			return getAPIDefinitionFromURL(APIDefinition);
		} else {
			try {
				File inputFile = new File(APIDefinition);
				if(inputFile.exists()) { 
					LOG.info("Reading API-Definition (Swagger/WSDL) from file: '" + APIDefinition + "' (relative path)");
					is = new FileInputStream(APIDefinition);
				} else {
					String baseDir = new File(this.apiConfig).getCanonicalFile().getParent();
					inputFile= new File(baseDir + File.separator + this.APIDefinition);
					LOG.info("Reading API-Definition (Swagger/WSDL) from file: '" + inputFile.getCanonicalFile() + "' (absolute path)"); 
					if(inputFile.exists()) { 
						is = new FileInputStream(inputFile);
					} else {
						is = this.getClass().getResourceAsStream(APIDefinition);
					}
				}
				if(is == null) {
					throw new AppException("Unable to read Swagger/WSDL file from: " + APIDefinition, ErrorCode.CANT_READ_API_DEFINITION_FILE);
				}
			} catch (Exception e) {
				throw new AppException("Unable to read Swagger/WSDL file from: " + APIDefinition, ErrorCode.CANT_READ_API_DEFINITION_FILE, e);
			}
			
		}
		return is;
	}
	
	private IAPI addImageContent(IAPI importApi) throws AppException {
		File file = null;
		if(importApi.getImage()!=null) { // An image is declared
			try {
				file = new File(importApi.getImage().getFilename());
				if(!file.exists()) { // The image isn't provided with an absolute path, try to read it relativ to the config file
					String baseDir = new File(this.apiConfig).getCanonicalFile().getParent();
					file = new File(baseDir + "/" + importApi.getImage().getFilename());
				}
				importApi.getImage().setBaseFilename(file.getName());
				InputStream is = this.getClass().getResourceAsStream(importApi.getImage().getFilename());
				if(file.exists()) {
					LOG.debug("Loading image from: '"+file.getCanonicalFile()+"'");
					importApi.getImage().setImageContent(IOUtils.toByteArray(new FileInputStream(file)));
					return importApi;
				} else if(is!=null) {
					LOG.debug("Trying to load image from classpath");
					// Try to read it from classpath
					importApi.getImage().setImageContent(IOUtils.toByteArray(is));
					return importApi;
				} else {
					throw new AppException("Image not found in filesystem ('"+file+"') or Classpath.", ErrorCode.UNXPECTED_ERROR);
				}
			} catch (Exception e) {
				throw new AppException("Can't read image-file: "+importApi.getImage().getFilename()+" from filesystem or classpath.", ErrorCode.UNXPECTED_ERROR, e);
			}
		}
		return importApi;
	}
	
	private void handleOutboundSSLAuthN(AuthenticationProfile authnProfile) throws AppException {
		if(!authnProfile.getType().equals(AuthType.ssl)) return;
		String clientCert = (String)authnProfile.getParameters().get("certFile");
		String password = (String)authnProfile.getParameters().get("password");
		String[] result = extractKeystoreTypeFromCertFile(clientCert);
		clientCert 			= result[0];
		String storeType 	= result[1];
		File clientCertFile = new File(clientCert);
		String clientCertClasspath = null;
		try {
			if(!clientCertFile.exists()) {
				// Try to find file using a relative path to the config file
				String baseDir = new File(this.apiConfig).getCanonicalFile().getParent();
				clientCertFile = new File(baseDir + "/" + clientCert);
			}
			if(!clientCertFile.exists()) {
				// If not found absolute & relative - Try to load it from ClassPath
				LOG.debug("Trying to load Client-Certificate from classpath");
				if(this.getClass().getResource(clientCert)==null) {
					throw new AppException("Can't read Client-Certificate-Keystore: "+clientCert+" from filesystem or classpath.", ErrorCode.UNXPECTED_ERROR);
				}
				clientCertClasspath = clientCert;
			}
			KeyStore store = null;
			store = loadKeystore(clientCertFile, clientCertClasspath, storeType, password);
			if(store==null) {
				ErrorState.getInstance().setError("Unable to configure Outbound SSL-Config. Can't load keystore: '"+clientCertFile+"' for any reason. "
						+ "Turn on debug to see log messages.", ErrorCode.WRONG_KEYSTORE_PASSWORD, false);
				throw new AppException("Unable to configure Outbound SSL-Config. Can't load keystore: '"+clientCertFile+"' for any reason.", ErrorCode.WRONG_KEYSTORE_PASSWORD);
			}
			X509Certificate certificate = null;
			Enumeration<String> e = store.aliases();
			while (e.hasMoreElements()) {
				String alias = e.nextElement();
				certificate = (X509Certificate) store.getCertificate(alias);
				certificate.getEncoded();
			}
			if(this.desiredAPI instanceof DesiredTestOnlyAPI) return; // Skip here when testing
			JsonNode node = APIManagerAdapter.getFileData(certificate.getEncoded(), clientCert);
			String data = node.get("data").asText();
			authnProfile.getParameters().put("pfx", data);
			authnProfile.getParameters().remove("certFile");
		} catch (Exception e) {
			throw new AppException("Can't read Client-Cert-File: "+clientCert+" from filesystem or classpath.", ErrorCode.UNXPECTED_ERROR, e);
		} 
	}
}
