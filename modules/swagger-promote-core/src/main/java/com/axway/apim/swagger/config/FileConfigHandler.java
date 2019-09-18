package com.axway.apim.swagger.config;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.APIImportMain;
import com.axway.apim.lib.AppException;
import com.axway.apim.lib.ErrorCode;
import com.axway.apim.lib.ErrorState;
import com.axway.apim.lib.Parameters;
import com.axway.apim.lib.URLParser;
import com.axway.apim.lib.Utils;
import com.axway.apim.swagger.APIManagerAdapter;
import com.axway.apim.swagger.api.properties.APIDefintion;
import com.axway.apim.swagger.api.properties.authenticationProfiles.AuthType;
import com.axway.apim.swagger.api.properties.authenticationProfiles.AuthenticationProfile;
import com.axway.apim.swagger.api.properties.cacerts.CaCert;
import com.axway.apim.swagger.api.properties.quota.QuotaRestriction;
import com.axway.apim.swagger.api.properties.quota.QuotaRestrictionDeserializer;
import com.axway.apim.swagger.api.state.DesiredAPI;
import com.axway.apim.swagger.api.state.IAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class FileConfigHandler extends AbstractConfigHandler implements ConfigHandlerInterface {

	private static Logger LOG = LoggerFactory.getLogger(FileConfigHandler.class);

	private ErrorState error = ErrorState.getInstance();

	private ObjectMapper mapper = new ObjectMapper();
	
	private APIConfig apiImportCfg;
	
	private String pathToAPIDefinition;

	public FileConfigHandler(String apiConfig, String pathToAPIDefinition, String stage, boolean orgAdminUsed) throws AppException {
		super(apiConfig, pathToAPIDefinition, stage, orgAdminUsed);
		SimpleModule module = new SimpleModule();
		module.addDeserializer(QuotaRestriction.class, new QuotaRestrictionDeserializer());
		mapper.registerModule(module);
		apiImportCfg = new APIConfig();
		try {
			File configFile = new File(locateAPIConfigFile((String)apiConfig));
			String apiConfigContent;
			try {
				apiConfigContent = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
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
			String stagedConfig = getStageConfig(stage, configFile);
			if(stagedConfig!=null) {
				try {
					stagedConfig = substitueVariables(stagedConfig);
					ObjectReader updater = mapper.readerForUpdating(apiImportCfg.getApiConfig());
					apiImportCfg.setApiConfig(updater.readValue(stagedConfig));
					LOG.info("Loaded stage API-Config from file: " + stage);
				} catch (FileNotFoundException e) {
					LOG.debug("No config file found for stage: '"+stage+"'");
				}
			}
		} catch (Exception e) {
			error.setError("Cant parse JSON-Config file(s)", ErrorCode.CANT_READ_CONFIG_FILE);
			throw new AppException("Cant parse JSON-Config file(s)", ErrorCode.CANT_READ_CONFIG_FILE, e);
		}

	}

	public APIConfig getConfig() throws AppException {
		checkForAPIDefinitionInConfiguration();
		//apiImportCfg.setApiDefinition();
		APIDefintion apiDefinition = new APIDefintion();
		apiDefinition.setAPIDefinitionFile(apiImportCfg.getApiDefinitionFilename());
		apiDefinition.setAPIDefinitionContent(getAPIDefinitionContent().getBytes(), (DesiredAPI)apiImportCfg.getApiConfig());
		apiImportCfg.getApiConfig().setAPIDefinition(apiDefinition);
		return apiImportCfg;
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

	/**
	 * If an API-Path is given directly this has to be used.
	 * @param desiredAPIConfig
	 * @throws AppException
	 */
	private void checkForAPIDefinitionInConfiguration() throws AppException {
		if (StringUtils.isEmpty((String)this.pathToAPIDefinition)) {
			if (StringUtils.isNotEmpty(this.apiImportCfg.getApiConfig().getApiDefinitionImport())) {
				this.pathToAPIDefinition=this.apiImportCfg.getApiConfig().getApiDefinitionImport();
				LOG.debug("Reading API Definition from configuration file");
			} else {
				ErrorState.getInstance().setError("No API Definition configured", ErrorCode.NO_API_DEFINITION_CONFIGURED, false);
				throw new AppException("No API Definition configured", ErrorCode.NO_API_DEFINITION_CONFIGURED);
			}
		}
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
			baseDir = new File((String)this.apiConfig).getCanonicalFile().getParent();
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

	private String getAPIDefinitionContent() throws AppException {
		try {
			InputStream stream = getAPIDefinitionAsStream();
			return IOUtils.toString(stream, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AppException("Can't read API-Definition from file", ErrorCode.CANT_READ_API_DEFINITION_FILE, e);
		}
	}

	/**
	 * To make testing easier we allow reading test-files from classpath as well
	 * @throws AppException when the import Swagger-File can't be read.
	 * @return The import Swagger-File as an InputStream
	 */
	private InputStream getAPIDefinitionAsStream() throws AppException {
		InputStream is = null;
		if(this.pathToAPIDefinition.endsWith(".url")) {
			return getAPIDefinitionFromURL(Utils.getAPIDefinitionUriFromFile(pathToAPIDefinition));
		} else if(isHttpUri(pathToAPIDefinition)) {
			return getAPIDefinitionFromURL(pathToAPIDefinition);
		} else {
			try {
				File inputFile = new File(pathToAPIDefinition);
				if(inputFile.exists()) { 
					LOG.info("Reading API-Definition (Swagger/WSDL) from file: '" + pathToAPIDefinition + "' (absolute path)");
					is = new FileInputStream(pathToAPIDefinition);
				} else {
					String baseDir = new File(pathToAPIDefinition).getCanonicalFile().getParent();
					inputFile= new File(baseDir + File.separator + this.apiDefinition);
					LOG.info("Reading API-Definition (Swagger/WSDL) from file: '" + inputFile.getCanonicalFile() + "' (absolute path)"); 
					if(inputFile.exists()) { 
						is = new FileInputStream(inputFile);
					} else {
						is = this.getClass().getResourceAsStream(pathToAPIDefinition);
					}
				}
				if(is == null) {
					throw new AppException("Unable to read Swagger/WSDL file from: " + apiDefinition, ErrorCode.CANT_READ_API_DEFINITION_FILE);
				}
			} catch (Exception e) {
				throw new AppException("Unable to read Swagger/WSDL file from: " + apiDefinition, ErrorCode.CANT_READ_API_DEFINITION_FILE, e);
			}

		}
		return is;
	}

	private InputStream getAPIDefinitionFromURL(String urlToAPIDefinition) throws AppException {
		URLParser url = new URLParser(urlToAPIDefinition);
		String uri = url.getUri();
		String username = url.getUsername();
		String password = url.getPassword();
		CloseableHttpClient httpclient = createHttpClient(uri, username, password);
		try {
			HttpGet httpGet = new HttpGet(uri);

			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				@Override
				public String handleResponse(
						final HttpResponse response) throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity,StandardCharsets.UTF_8) : null;
					} else {
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}

			};
			String responseBody = httpclient.execute(httpGet, responseHandler);
			return new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new AppException("Cannot load API-Definition from URI: "+uri, ErrorCode.CANT_READ_API_DEFINITION_FILE, e);
		} finally {
			try {
				httpclient.close();
			} catch (Exception ignore) {}
		}
	}

	private IAPI addImageContent(IAPI importApi) throws AppException {
		File file = null;
		if(importApi.getImage()!=null) { // An image is declared
			try {
				file = new File(importApi.getImage().getFilename());
				if(!file.exists()) { // The image isn't provided with an absolute path, try to read it relativ to the config file
					String baseDir = new File(this.pathToAPIDefinition).getCanonicalFile().getParent();
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

	public static boolean isHttpUri(String pathToAPIDefinition) {
		String httpUri = pathToAPIDefinition.substring(pathToAPIDefinition.indexOf("@")+1);
		return( httpUri.startsWith("http://") || httpUri.startsWith("https://"));
	}
	
	public static boolean isHttpsUri(String uri) {
		return( uri.startsWith("https://") );
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
				String baseDir = new File(this.pathToAPIDefinition).getCanonicalFile().getParent();
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
			//if(this.desiredAPI instanceof DesiredTestOnlyAPI) return; // Skip here when testing
			JsonNode node = APIManagerAdapter.getFileData(certificate.getEncoded(), clientCert);
			String data = node.get("data").asText();
			authnProfile.getParameters().put("pfx", data);
			authnProfile.getParameters().remove("certFile");
		} catch (Exception e) {
			throw new AppException("Can't read Client-Cert-File: "+clientCert+" from filesystem or classpath.", ErrorCode.UNXPECTED_ERROR, e);
		} 
	}
	
	

	private void addSSLContext(String uri, HttpClientBuilder httpClientBuilder) throws KeyManagementException,
	NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {
		if (isHttpsUri(uri)) {
			SSLConnectionSocketFactory sslCtx = createSSLContext();
			if (sslCtx!=null) {
				httpClientBuilder.setSSLSocketFactory(sslCtx);
			}
		}
	}

	private void addBasicAuthCredential(String uri, String username, String password,
			HttpClientBuilder httpClientBuilder) {
		//if(this.desiredAPI instanceof DesiredTestOnlyAPI) return; // Don't do that when unit-testing
		if(username!=null) {
			LOG.info("Loading API-Definition from: " + uri + " ("+username+")");
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(
					new AuthScope(AuthScope.ANY),
					new UsernamePasswordCredentials(username, password));
			httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
		} else {
			LOG.info("Loading API-Definition from: " + uri);
		}
	}
	
	private CloseableHttpClient createHttpClient(String uri, String username, String password) throws AppException {
		HttpClientBuilder httpClientBuilder = HttpClients.custom();
		try {
			addBasicAuthCredential(uri, username, password, httpClientBuilder);
			addSSLContext(uri, httpClientBuilder);
			return httpClientBuilder.build();
		} catch (Exception e) {
			throw new AppException("Error during create http client for retrieving ...", ErrorCode.CANT_CREATE_HTTP_CLIENT);
		}
	}
	
	private SSLConnectionSocketFactory createSSLContext() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {
		String keyStorePath=System.getProperty("javax.net.ssl.keyStore","");
		if (StringUtils.isNotEmpty(keyStorePath)) {
			String keyStorePassword=System.getProperty("javax.net.ssl.keyStorePassword","");
			if (StringUtils.isNotEmpty(keyStorePassword)) {
				String keystoreType=System.getProperty("javax.net.ssl.keyStoreType",KeyStore.getDefaultType());
				LOG.debug("Reading keystore from {}",keyStorePath);
				KeyStore ks = KeyStore.getInstance(keystoreType);
				ks.load(new FileInputStream(new File(keyStorePath)), keyStorePassword.toCharArray());
				SSLContext sslcontext = SSLContexts.custom()
	                .loadKeyMaterial(ks,keyStorePassword.toCharArray())
	                .loadTrustMaterial(new TrustSelfSignedStrategy())
	                .build();
				String [] tlsProts = getAcceptedTLSProtocols();
				SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
		                sslcontext,
		                tlsProts,
		                null,
		                SSLConnectionSocketFactory.getDefaultHostnameVerifier());
				return sslsf;
			}
		} else {
			LOG.debug("NO javax.net.ssl.keyStore property. Avoid to set SSLContextFactory ");
		}
		return null;
	}

	private String[] getAcceptedTLSProtocols() {
		String protocols = System.getProperty("https.protocols","TLSv1.2"); //default TLSv1.2
		LOG.debug("https protocols: {}",protocols);
		return protocols.split(",");
	}

	private String[] extractKeystoreTypeFromCertFile(String certFileName) throws AppException {
		if(!certFileName.contains(":")) return new String[]{certFileName, null};
		int pos = certFileName.lastIndexOf(":");
		if(pos<3) return new String[]{certFileName, null}; // This occurs for the following format: c:/path/to/my/store
		String type = certFileName.substring(pos+1);
		if(!Security.getAlgorithms("KeyStore").contains(type)) {
			ErrorState.getInstance().setError("Unknown keystore type: '"+type+"'. Supported: " + Security.getAlgorithms("KeyStore"), ErrorCode.WRONG_KEYSTORE_PASSWORD);
			throw new AppException("Unknown keystore type: '"+type+"'. Supported: " + Security.getAlgorithms("KeyStore"), ErrorCode.WRONG_KEYSTORE_PASSWORD);
		}
		certFileName = certFileName.substring(0, pos);
		return new String[]{certFileName, type};
	}
	
	private KeyStore loadKeystore(File clientCertFile, String clientCertClasspath, String keystoreType, String password) throws IOException {
		InputStream is = null;
		KeyStore store = null;

		if(keystoreType!=null) {
			try {
				// Get the Inputstream and load the keystore with the given Keystore-Type
				if(clientCertClasspath==null) {
					is = new BufferedInputStream(new FileInputStream(clientCertFile));
				} else {
					is = this.getClass().getResourceAsStream(clientCertClasspath);
				}
				LOG.debug("Loading keystore: '"+clientCertFile+"' using keystore type: '"+keystoreType+"'");
				store = KeyStore.getInstance(keystoreType);
				store.load(is, password.toCharArray());
				return store;
			} catch (IOException e) {
				if(e.getMessage()!=null && e.getMessage().toLowerCase().contains("keystore password was incorrect")) {
					ErrorState.getInstance().setError("Unable to configure Outbound SSL-Config as password for keystore: is incorrect.", ErrorCode.WRONG_KEYSTORE_PASSWORD, false);
					throw e;
				}
				LOG.debug("Error message using type: " + keystoreType + " Error-Message: " + e.getMessage());
				throw e;
			} catch (Exception e) {
				LOG.debug("Error message using type: " + keystoreType + " Error-Message: " + e.getMessage());
				return null;
			} finally {
				if(is!=null) is.close();
			}
		}
		// Otherwise we try every known type		
		LOG.debug("Loading keystore: '"+clientCertFile+"' trying the following types: " + Security.getAlgorithms("KeyStore"));
		for(String type : Security.getAlgorithms("KeyStore")) {
			try {
				LOG.debug("Trying to load keystore: '"+clientCertFile+"' using type: '"+type+"'");
				// Get the Inputstream and load the keystore with the given Keystore-Type
				if(clientCertClasspath==null) {
					is = new BufferedInputStream(new FileInputStream(clientCertFile));
				} else {
					is = this.getClass().getResourceAsStream(clientCertClasspath);
				}
				store = KeyStore.getInstance(type);
				store.load(is, password.toCharArray());
				if(store!=null) {
					LOG.debug("Successfully loaded keystore: '"+clientCertFile+"' with type: " + type);
					return store;
				}
			} catch (IOException e) {
				if(e.getMessage()!=null && e.getMessage().toLowerCase().contains("keystore password was incorrect")) {
					ErrorState.getInstance().setError("Unable to configure Outbound SSL-Config as password for keystore: is incorrect.", ErrorCode.WRONG_KEYSTORE_PASSWORD, false);
					throw e;
				}
				LOG.debug("Error message using type: " + keystoreType + " Error-Message: " + e.getMessage());
			} catch (Exception e) {
				LOG.debug("Error message using type: " + keystoreType + " Error-Message: " + e.getMessage());
			} finally {
				if(is!=null) is.close();
			}
		}
		return null;
	}
}
