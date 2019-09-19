package com.axway.apim.swagger;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
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
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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
import com.axway.apim.lib.APIPropertiesExport;
import com.axway.apim.lib.AppException;
import com.axway.apim.lib.Parameters;
import com.axway.apim.lib.ErrorCode;
import com.axway.apim.lib.ErrorState;
import com.axway.apim.lib.URLParser;
import com.axway.apim.lib.Utils;
import com.axway.apim.swagger.api.properties.APIDefintion;
import com.axway.apim.swagger.api.properties.applications.ClientApplication;
import com.axway.apim.swagger.api.properties.authenticationProfiles.AuthType;
import com.axway.apim.swagger.api.properties.authenticationProfiles.AuthenticationProfile;
import com.axway.apim.swagger.api.properties.cacerts.CaCert;
import com.axway.apim.swagger.api.properties.corsprofiles.CorsProfile;
import com.axway.apim.swagger.api.properties.inboundprofiles.InboundProfile;
import com.axway.apim.swagger.api.properties.organization.Organization;
import com.axway.apim.swagger.api.properties.outboundprofiles.OutboundProfile;
import com.axway.apim.swagger.api.properties.quota.APIQuota;
import com.axway.apim.swagger.api.properties.quota.QuotaRestriction;
import com.axway.apim.swagger.api.properties.quota.QuotaRestrictionDeserializer;
import com.axway.apim.swagger.api.properties.securityprofiles.DeviceType;
import com.axway.apim.swagger.api.properties.securityprofiles.SecurityDevice;
import com.axway.apim.swagger.api.properties.securityprofiles.SecurityProfile;
import com.axway.apim.swagger.api.state.AbstractAPI;
import com.axway.apim.swagger.api.state.DesiredAPI;
import com.axway.apim.swagger.api.state.DesiredTestOnlyAPI;
import com.axway.apim.swagger.api.state.IAPI;
import com.axway.apim.swagger.config.ConfigHandlerFactory;
import com.axway.apim.swagger.config.ConfigHandlerInterface;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;

/**
 * The APIConfig reflects the given API-Configuration plus the API-Definition, which is either a 
 * Swagger-File or a WSDL.
 * This class will read the API-Configuration plus the optional set stage and the API-Definition.
 * 
 * @author cwiechmann
 */
public class APIImportConfigAdapter {
	
	private static Logger LOG = LoggerFactory.getLogger(APIImportConfigAdapter.class);
	
	/** The configuration used to execute Swagger-Promote */
	private ConfigHandlerInterface configHandler;
	
	/** The desire API instance created by the APIConfigImporter based on the given APIConfig */
	private IAPI apiConfig;
	
	private ErrorState error = ErrorState.getInstance();

	/**
	 * Constructor just for testing. Don't use it!
	 */
	public APIImportConfigAdapter(IAPI apiConfig) {
		this.apiConfig = apiConfig;
	}
	
	/**
	 * @param apiConfig the content of the API-Configuration
	 * @param stage the stage
	 * @param APIDefinition the content of the API-Definition
	 * @param usingOrgAdmin is an Org-Admin running Swagger-Promote?
	 * @throws AppException 
	 */
	public APIImportConfigAdapter(Parameters params, boolean isUsingOrgAdmin) throws AppException {
		this.configHandler = ConfigHandlerFactory.getConfigHandler(params.getValue("contract"), params.getValue("apidefinition"), (String)params.getValue("stage"), isUsingOrgAdmin);
		this.apiConfig = configHandler.getApiConfig();
	}

	/**
	 * This method exists for testing purposes to get the desired API 
	 * after processing it maybe partially.
	 * @return
	 */
	public IAPI getAPIConfig() {
		return apiConfig;
	}

	/**
	 * Returns the IAPIDefintion that returns the desired state of the API. In this method:<br>
	 * - the API-Config is read
	 * - the API-Config is merged with the override
	 * - the API-Definition is read
	 * - Additionally some validations and completions are made here
	 * - in the future: This is the place to do some default handling.
	 * 
	 * @return IAPIDefintion with the desired state of the API. This state will be 
	 * the input to create the APIChangeState.
	 * 
	 * @throws AppException if the state can't be created.
	 */
	public IAPI getDesiredAPI() throws AppException {
		try {
			validateExposurePath(apiConfig);
			validateOrganization(apiConfig);
			addDefaultPassthroughSecurityProfile(apiConfig);
			addDefaultCorsProfile(apiConfig);
			addDefaultAuthenticationProfile(apiConfig);
			addDefaultOutboundProfile(apiConfig);
			addDefaultInboundProfile(apiConfig);
			//addImageContent(desiredAPI);
			validateCustomProperties(apiConfig);
			validateDescription(apiConfig);
			validateOutboundAuthN(apiConfig);
			validateHasQueryStringKey(apiConfig);
			completeCaCerts(apiConfig);
			addQuotaConfiguration(apiConfig);
			handleAllOrganizations(apiConfig);
			completeClientApplications(apiConfig);
			return apiConfig;
		} catch (Exception e) {
			if(e.getCause() instanceof AppException) {
				throw (AppException)e.getCause();
			}
			throw new AppException("Cannot validate/fulfill configuration file.", ErrorCode.CANT_READ_CONFIG_FILE, e);
		}
	}
	
	/**
	 * The purpose of this method is to translated the given Method-Names into internal 
	 * operationId. These operationIds are created and then known, when the API has 
	 * been inserted. 
	 * Translating the methodNames to operationIds already during import is required for 
	 * the comparison between the desired and actual API.
	 * @param desiredAPI the configured desired API
	 * @param actualAPI a potinetially existing actual API
	 * @return the desired API containing operationId in Inbound- and Outbound-Profiles
	 * @throws AppException when something goes wrong
	 */
	public IAPI completeDesiredAPI(IAPI desiredAPI, IAPI actualAPI) throws AppException {
		if(!actualAPI.isValid()) return desiredAPI;
		APIManagerAdapter mgrAdpater = APIManagerAdapter.getInstance();
		// We need to safe the original methodNames, as they are required during API-Re-Creation
		((DesiredAPI)desiredAPI).setOriginalInboundProfiles(desiredAPI.getInboundProfiles());
		((DesiredAPI)desiredAPI).setOriginalOutboundProfiles(desiredAPI.getOutboundProfiles());
		mgrAdpater.translateMethodIds(desiredAPI.getInboundProfiles(), actualAPI);
		mgrAdpater.translateMethodIds(desiredAPI.getOutboundProfiles(), actualAPI);
		return desiredAPI;
	}
	
	private void validateExposurePath(IAPI apiConfig) throws AppException {
		if(apiConfig.getPath()==null) {
			ErrorState.getInstance().setError("Config-Parameter: 'path' is not given", ErrorCode.CANT_READ_CONFIG_FILE, false);
			throw new AppException("Path is invalid.", ErrorCode.CANT_READ_CONFIG_FILE);
		}
		if(!apiConfig.getPath().startsWith("/")) {
			ErrorState.getInstance().setError("Config-Parameter: 'path' must start with a \"/\" following by a valid API-Path (e.g. /api/v1/customer).", ErrorCode.CANT_READ_CONFIG_FILE, false);
			throw new AppException("Path is invalid.", ErrorCode.CANT_READ_CONFIG_FILE);
		}
	}
	
	private void validateOrganization(IAPI apiConfig) throws AppException {
		if(apiConfig instanceof DesiredTestOnlyAPI) return;
		if(configHandler.isOrgAdminUsed()) { // Hardcode the orgId to the organization of the used OrgAdmin
			apiConfig.setOrganizationId(APIManagerAdapter.getCurrentUser(false).getOrganizationId());
		} else {
			String desiredOrgId = APIManagerAdapter.getInstance().getOrgId(apiConfig.getOrganization(), true);
			if(desiredOrgId==null) {
				error.setError("The given organization: '"+apiConfig.getOrganization()+"' is either unknown or hasn't the Development flag.", ErrorCode.UNKNOWN_ORGANIZATION, false);
				throw new AppException("The given organization: '"+apiConfig.getOrganization()+"' is either unknown or hasn't the Development flag.", ErrorCode.UNKNOWN_ORGANIZATION);
			}
			apiConfig.setOrganizationId(desiredOrgId);
		}
	}
	
	private void handleAllOrganizations(IAPI apiConfig) throws AppException {
		if(apiConfig.getClientOrganizations()==null) return;
		if(apiConfig.getState().equals(IAPI.STATE_UNPUBLISHED)) {
			apiConfig.setClientOrganizations(null); // Making sure, orgs are not considered as a changed property
			return;
		}
		List<String> allDesiredOrgs = new ArrayList<String>();
		List<Organization> allOrgs = APIManagerAdapter.getInstance().getAllOrgs();
		if(apiConfig.getClientOrganizations().contains("ALL")) {
			for(Organization org : allOrgs) {
				allDesiredOrgs.add(org.getName());
			}
			apiConfig.getClientOrganizations().clear();
			apiConfig.getClientOrganizations().addAll(allDesiredOrgs);
			((DesiredAPI)apiConfig).setRequestForAllOrgs(true);
		} else {
			// As the API-Manager internally handles the owning organization in the same way, 
			// we have to add the Owning-Org as a desired org
			if(!apiConfig.getClientOrganizations().contains(apiConfig.getOrganization())) {
				apiConfig.getClientOrganizations().add(apiConfig.getOrganization());
			}
			// And validate each configured organization really exists in the API-Manager
			Iterator<String> it = apiConfig.getClientOrganizations().iterator();
			String invalidClientOrgs = null;
			while(it.hasNext()) {
				String org = it.next();
				Organization desiredOrg = new Organization();
				desiredOrg.setName(org);
				if(!allOrgs.contains(desiredOrg)) {
					LOG.warn("Unknown organization with name: '" + desiredOrg.getName() + "' configured. Ignoring this organization.");
					invalidClientOrgs = invalidClientOrgs==null ? org : invalidClientOrgs + ", "+org;
					APIPropertiesExport.getInstance().setProperty(ErrorCode.INVALID_CLIENT_ORGANIZATIONS.name(), invalidClientOrgs);
					it.remove();
					continue;
				}
			}
		}
	}
	
	private void addQuotaConfiguration(IAPI apiConfig) throws AppException {
		if(apiConfig.getState()==IAPI.STATE_UNPUBLISHED) return;
		DesiredAPI importAPI = (DesiredAPI)apiConfig;
		initQuota(importAPI.getSystemQuota());
		initQuota(importAPI.getApplicationQuota());
	}
	
	private void initQuota(APIQuota quotaConfig) {
		if(quotaConfig==null) return;
		if(quotaConfig.getType().equals("APPLICATION")) {
			quotaConfig.setName("Application Default");
			quotaConfig.setDescription("Maximum message rates per application. Applied to each application unless an Application-Specific quota is configured");
		} else {
			quotaConfig.setName("System Default");
			quotaConfig.setDescription(".....");			
		}
	}
	
	private void validateDescription(IAPI apiConfig) throws AppException {
		if(apiConfig.getDescriptionType()==null || apiConfig.getDescriptionType().equals("original")) return;
		String descriptionType = apiConfig.getDescriptionType();
		if(descriptionType.equals("manual")) {
			if(apiConfig.getDescriptionManual()==null) {
				throw new AppException("descriptionManual can't be null with descriptionType set to 'manual'", ErrorCode.CANT_READ_CONFIG_FILE);
			}
		} else if(descriptionType.equals("url")) {
			if(apiConfig.getDescriptionUrl()==null) {
				throw new AppException("descriptionUrl can't be null with descriptionType set to 'url'", ErrorCode.CANT_READ_CONFIG_FILE);
			}
		} else if(descriptionType.equals("markdown")) {
			if(apiConfig.getDescriptionMarkdown()==null) {
				throw new AppException("descriptionMarkdown can't be null with descriptionType set to 'markdown'", ErrorCode.CANT_READ_CONFIG_FILE);
			}
			if(!apiConfig.getDescriptionMarkdown().startsWith("${env.")) {
				throw new AppException("descriptionMarkdown must start with an environment variable", ErrorCode.CANT_READ_CONFIG_FILE);
			}
		} else if(descriptionType.equals("original")) {
			return;
		} else {
			throw new AppException("Unknown descriptionType: '"+descriptionType.equals("manual")+"'", ErrorCode.CANT_READ_CONFIG_FILE);
		}
	}
	
	private void addDefaultCorsProfile(IAPI apiConfig) throws AppException {
		if(apiConfig.getCorsProfiles()==null) {
			((AbstractAPI)apiConfig).setCorsProfiles(new ArrayList<CorsProfile>());
		}
		// Check if there is a default cors profile declared otherwise create one internally
		boolean defaultCorsFound = false;
		for(CorsProfile profile : apiConfig.getCorsProfiles()) {
			if(profile.getName().equals("_default")) {
				defaultCorsFound = true;
				break;
			}
		}
		if(!defaultCorsFound) {
			CorsProfile defaultCors = new CorsProfile();
			defaultCors.setName("_default");
			defaultCors.setIsDefault("true");
			defaultCors.setOrigins(new String[] {"*"});
			defaultCors.setAllowedHeaders(new String[] {});
			defaultCors.setExposedHeaders(new String[] {"X-CorrelationID"});
			defaultCors.setMaxAgeSeconds("0");
			defaultCors.setSupportCredentials("false");
			apiConfig.getCorsProfiles().add(defaultCors);
		}
	}
	
	/**
	 * Purpose of this method is to load the actual existing applications from API-Manager 
	 * based on the provided criteria (App-Name, API-Key, OAuth-ClientId or Ext-ClientId). 
	 * Or, if the APP doesn't exists remove it from the list and log a warning message.
	 * Additionally, for each application it's check, that the organization has access 
	 * to this API, otherwise it will be removed from the list as well and a warning message is logged.
	 * @param apiConfig
	 * @throws AppException
	 */
	private void completeClientApplications(IAPI apiConfig) throws AppException {
		if(Parameters.getInstance().isIgnoreClientApps()) return;
		if(apiConfig.getState()==IAPI.STATE_UNPUBLISHED) return;
		ClientApplication loadedApp = null;
		ClientApplication app;
		if(apiConfig.getApplications()!=null) {
			LOG.info("Handling configured client-applications.");
			ListIterator<ClientApplication> it = apiConfig.getApplications().listIterator();
			String invalidClientApps = null;
			while(it.hasNext()) {
				app = it.next();
				if(app.getName()!=null) {
					loadedApp = APIManagerAdapter.getInstance().getApplication(app.getName());
					if(loadedApp==null) {
						LOG.warn("Unknown application with name: '" + app.getName() + "' configured. Ignoring this application.");
						invalidClientApps = invalidClientApps==null ? app.getName() : invalidClientApps + ", "+app.getName();
						APIPropertiesExport.getInstance().setProperty(ErrorCode.INVALID_CLIENT_APPLICATIONS.name(), invalidClientApps);
						it.remove();
						continue;
					}
					LOG.info("Found existing application: '"+app.getName()+"' ("+app.getId()+") based on given name '"+app.getName()+"'");
				} else if(app.getApiKey()!=null) {
					loadedApp = getAppForCredential(app.getApiKey(), APIManagerAdapter.CREDENTIAL_TYPE_API_KEY);
					if(loadedApp==null) {
						it.remove();
						continue;
					} 
				} else if(app.getOauthClientId()!=null) {
					loadedApp = getAppForCredential(app.getOauthClientId(), APIManagerAdapter.CREDENTIAL_TYPE_OAUTH);
					if(loadedApp==null) {
						it.remove();
						continue;
					} 
				} else if(app.getExtClientId()!=null) {
					loadedApp = getAppForCredential(app.getExtClientId(), APIManagerAdapter.CREDENTIAL_TYPE_EXT_CLIENTID);
					if(loadedApp==null) {
						it.remove();
						continue;
					} 
				}
				if(!APIManagerAdapter.getInstance().hasAdminAccount()) {
					if(!apiConfig.getOrganizationId().equals(loadedApp.getOrganizationId())) {
						LOG.warn("OrgAdmin can't handle application: '"+loadedApp.getName()+"' belonging to a different organization. Ignoring this application.");
						it.remove();
						continue;
					}
				}
				it.set(loadedApp); // Replace the incoming app, with the App loaded from API-Manager
			}
		}
	}
	
	private static ClientApplication getAppForCredential(String credential, String type) throws AppException {
		LOG.debug("Searching application with configured credential (Type: "+type+"): '"+credential+"'");
		ClientApplication app = APIManagerAdapter.getInstance().getAppIdForCredential(credential, type);
		if(app==null) {
			LOG.warn("Unknown application with ("+type+"): '" + credential + "' configured. Ignoring this application.");
			return null;
		}
		return app;
	}
	
	private void completeCaCerts(IAPI apiConfig) throws AppException {
		if(apiConfig.getCaCerts()!=null) {
			List<CaCert> completedCaCerts = new ArrayList<CaCert>();
			for(CaCert cert :apiConfig.getCaCerts()) {
				if(cert.getCertBlob()==null) {
					LOG.info("SOMETHING TO DO HERE PLEASE CHECK");
					/*JsonNode certInfo = APIManagerAdapter.getCertInfo(getInputStreamForCertFile(cert), cert);
					try {
						CaCert completedCert = mapper.readValue(certInfo.get(0).toString(), CaCert.class);
						completedCaCerts.add(completedCert);
					} catch (Exception e) {
						throw new AppException("Can't initialize given certificate.", ErrorCode.CANT_READ_CONFIG_FILE, e);
					}*/
				}
			}
			apiConfig.getCaCerts().clear();
			apiConfig.getCaCerts().addAll(completedCaCerts);
		}
	}
	
	private void validateCustomProperties(IAPI apiConfig) throws AppException {
		if(apiConfig.getCustomProperties()!=null) {
			JsonNode configuredProps = APIManagerAdapter.getCustomPropertiesConfig();
			Iterator<String> props = apiConfig.getCustomProperties().keySet().iterator();
			while(props.hasNext()) {
				String propertyKey = props.next();
				String propertyValue = apiConfig.getCustomProperties().get(propertyKey);
				JsonNode configuredProp = configuredProps.at("/api/"+propertyKey);
				if(configuredProp instanceof MissingNode) {
					ErrorState.getInstance().setError("The custom-property: '" + propertyKey + "' is not configured in API-Manager.", ErrorCode.CANT_READ_CONFIG_FILE, false);
					throw new AppException("The custom-property: '" + propertyKey + "' is not configured in API-Manager.", ErrorCode.CANT_READ_CONFIG_FILE);
				}
				JsonNode propType = configuredProp.get("type");
				if(propType!=null && ( propType.asText().equals("select") || propType.asText().equals("switch") )) {
					boolean valueFound = false;
					ArrayNode selectOptions = (ArrayNode)configuredProp.get("options");
					for(JsonNode option : selectOptions) {
						if(option.at("/value").asText().equals(propertyValue)) {
							valueFound = true;
							break;
						}
					}
					if(!valueFound) {
						ErrorState.getInstance().setError("The value: '" + propertyValue + "' isn't configured for custom property: '" + propertyKey + "'", ErrorCode.CANT_READ_CONFIG_FILE, false);
						throw new AppException("The value: '" + propertyValue + "' isn't configured for custom property: '" + propertyKey + "'", ErrorCode.CANT_READ_CONFIG_FILE);
					}
				}
			}
		}
	}
	
	private IAPI addDefaultInboundProfile(IAPI importApi) throws AppException {
		if(importApi.getInboundProfiles()==null || importApi.getInboundProfiles().size()==0) return importApi;
		Iterator<String> it = importApi.getInboundProfiles().keySet().iterator();
		while(it.hasNext()) {
			String profileName = it.next();
			if(profileName.equals("_default")) return importApi; // Nothing to, there is a default profile
		}
		InboundProfile defaultProfile = new InboundProfile();
		defaultProfile.setSecurityProfile("_default");
		defaultProfile.setCorsProfile("_default");
		defaultProfile.setMonitorAPI(true);
		defaultProfile.setMonitorSubject("authentication.subject.id");
		importApi.getInboundProfiles().put("_default", defaultProfile);
		return importApi;
	}
	
	private IAPI addDefaultPassthroughSecurityProfile(IAPI importApi) throws AppException {
		boolean hasDefaultProfile = false;
		if(importApi.getSecurityProfiles()==null) importApi.setSecurityProfiles(new ArrayList<SecurityProfile>());
		List<SecurityProfile> profiles = importApi.getSecurityProfiles();
		for(SecurityProfile profile : importApi.getSecurityProfiles()) {
			if(profile.getIsDefault()) {
				if(hasDefaultProfile) {
					ErrorState.getInstance().setError("You can have only one _default SecurityProfile.", ErrorCode.CANT_READ_CONFIG_FILE, false);
					throw new AppException("You can have only one _default SecurityProfile.", ErrorCode.CANT_READ_CONFIG_FILE);
				}
				hasDefaultProfile=true;
				profile.setName("_default"); // Overwrite the name if it is default! (this is required by the API-Manager)
			}
		}
		if(profiles==null || profiles.size()==0 || !hasDefaultProfile) {
			SecurityProfile passthroughProfile = new SecurityProfile();
			passthroughProfile.setName("_default");
			passthroughProfile.setIsDefault(true);
			SecurityDevice passthroughDevice = new SecurityDevice();
			passthroughDevice.setName("Pass Through");
			passthroughDevice.setType(DeviceType.passThrough);
			passthroughDevice.setOrder(0);
			passthroughDevice.getProperties().put("subjectIdFieldName", "Pass Through");
			passthroughDevice.getProperties().put("removeCredentialsOnSuccess", "true");
			passthroughProfile.getDevices().add(passthroughDevice);
			
			profiles.add(passthroughProfile);
		}
		return importApi;
	}
	
	private IAPI addDefaultAuthenticationProfile(IAPI importApi) throws AppException {
		if(importApi.getAuthenticationProfiles()==null) return importApi; // Nothing to add (no default is needed, as we don't send any Authn-Profile)
		boolean hasDefaultProfile = false;
		List<AuthenticationProfile> profiles = importApi.getAuthenticationProfiles();
		for(AuthenticationProfile profile : profiles) {
			if(profile.getIsDefault() || profile.getName().equals("_default")) {
				if(hasDefaultProfile) {
					ErrorState.getInstance().setError("You can have only one AuthenticationProfile configured as default", ErrorCode.CANT_READ_CONFIG_FILE, false);
					throw new AppException("You can have only one AuthenticationProfile configured as default", ErrorCode.CANT_READ_CONFIG_FILE);
				}
				hasDefaultProfile=true;
				profile.setName("_default"); // Overwrite the name if it is default! (this is required by the API-Manager)
				profile.setIsDefault(true); 
			}
		}
		if(!hasDefaultProfile) {
			LOG.warn("THERE NO DEFAULT authenticationProfile CONFIGURED. Auto-Creating a No-Authentication outbound profile as default!");
			AuthenticationProfile noAuthNProfile = new AuthenticationProfile();
			noAuthNProfile.setName("_default");
			noAuthNProfile.setIsDefault(true);
			noAuthNProfile.setType(AuthType.none);
			profiles.add(noAuthNProfile);
		}
		return importApi;
	}
	
	private IAPI addDefaultOutboundProfile(IAPI importApi) throws AppException {
		if(importApi.getOutboundProfiles()==null || importApi.getOutboundProfiles().size()==0) return importApi;
		Iterator<String> it = importApi.getOutboundProfiles().keySet().iterator();
		while(it.hasNext()) {
			String profileName = it.next();
			if(profileName.equals("_default")) {
				// Validate the _default Outbound-Profile has an AuthN-Profile, otherwise we must add (See isseu #133)
				OutboundProfile profile = importApi.getOutboundProfiles().get(profileName);
				if(profile.getAuthenticationProfile()==null) {
					LOG.warn("Provided default outboundProfile doesn't contain AuthN-Profile - Setting it to default");
					profile.setAuthenticationProfile("_default");
				}
			}
			return importApi;
		}
		OutboundProfile defaultProfile = new OutboundProfile();
		defaultProfile.setAuthenticationProfile("_default");
		defaultProfile.setRouteType("proxy");
		importApi.getOutboundProfiles().put("_default", defaultProfile);
		return importApi;
	}
	
	private void validateOutboundAuthN(IAPI importApi) throws AppException {
		// Request to use some specific Outbound-AuthN for this API
		if(importApi.getAuthenticationProfiles()!=null && importApi.getAuthenticationProfiles().size()!=0) {
			if(importApi.getAuthenticationProfiles().get(0).getType().equals(AuthType.ssl)) 
				LOG.warn("SOMETHING TO DO HERE! Please check");
				//handleOutboundSSLAuthN(importApi.getAuthenticationProfiles().get(0));
		}
		
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
	
	private void validateHasQueryStringKey(IAPI importApi) throws AppException {
		if(importApi instanceof DesiredTestOnlyAPI) return; // Do nothing when unit-testing
		if(APIManagerAdapter.getApiManagerVersion().startsWith("7.5")) return; // QueryStringRouting isn't supported
		if(APIManagerAdapter.getInstance().hasAdminAccount()) {
			String apiRoutingKeyEnabled = APIManagerAdapter.getApiManagerConfig("apiRoutingKeyEnabled");
			if(apiRoutingKeyEnabled.equals("true")) {
				if(importApi.getApiRoutingKey()==null) {
					ErrorState.getInstance().setError("API-Manager configured for Query-String option, but API doesn' declare it.", ErrorCode.API_CONFIG_REQUIRES_QUERY_STRING, false);
					throw new AppException("API-Manager configured for Query-String option, but API doesn' declare it.", ErrorCode.API_CONFIG_REQUIRES_QUERY_STRING);
				}
			}
		} else {
			LOG.debug("Can't check if QueryString for API is needed without Admin-Account.");
		}
	}
/*
	public String getPathToAPIDefinition() {
		return APIDefinition;
	}

	public void setPathToAPIDefinition(String pathToAPIDefinition) {
		this.APIDefinition = pathToAPIDefinition;
	}
	*/
	
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
	
	
	
}
