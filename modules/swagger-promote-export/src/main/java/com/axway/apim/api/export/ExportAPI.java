package com.axway.apim.api.export;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.axway.apim.lib.AppException;
import com.axway.apim.swagger.APIManagerAdapter;
import com.axway.apim.swagger.api.properties.APIDefintion;
import com.axway.apim.swagger.api.properties.APIImage;
import com.axway.apim.swagger.api.properties.applications.ClientApplication;
import com.axway.apim.swagger.api.properties.authenticationProfiles.AuthType;
import com.axway.apim.swagger.api.properties.authenticationProfiles.AuthenticationProfile;
import com.axway.apim.swagger.api.properties.cacerts.CaCert;
import com.axway.apim.swagger.api.properties.corsprofiles.CorsProfile;
import com.axway.apim.swagger.api.properties.inboundprofiles.InboundProfile;
import com.axway.apim.swagger.api.properties.outboundprofiles.OutboundProfile;
import com.axway.apim.swagger.api.properties.profiles.ServiceProfile;
import com.axway.apim.swagger.api.properties.quota.APIQuota;
import com.axway.apim.swagger.api.properties.securityprofiles.DeviceType;
import com.axway.apim.swagger.api.properties.securityprofiles.SecurityDevice;
import com.axway.apim.swagger.api.properties.securityprofiles.SecurityProfile;
import com.axway.apim.swagger.api.properties.tags.TagMap;
import com.axway.apim.swagger.api.state.ActualAPI;
import com.axway.apim.swagger.api.state.IAPI;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonPropertyOrder({ "name", "path", "state", "version", "organization", "apiDefinition", "summary", "descriptionType", "descriptionManual", "vhost", 
	"backendBasepath", "image", "inboundProfiles", "outboundProfiles", "securityProfiles", "authenticationProfiles", "tags", "customProperties", 
	"corsProfiles", "caCerts", "applicationQuota", "systemQuota" })
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ExportAPI {
	
	IAPI actualAPIProxy = null;
	
	public String getPath() throws AppException {
		return this.actualAPIProxy.getPath();
	}

	public ExportAPI(IAPI actualAPIProxy) {
		super();
		this.actualAPIProxy = actualAPIProxy;
	}
	
	
	@JsonIgnore
	public boolean isValid() {
		return this.actualAPIProxy.isValid();
	}

	
	@JsonIgnore
	public String getOrganizationId() {
		try {
			return this.actualAPIProxy.getOrganizationId();
		} catch (AppException e) {
			throw new RuntimeException("Can't read orgId");
		}
	}

	
	@JsonIgnore
	public APIDefintion getAPIDefinition() {
		return this.actualAPIProxy.getAPIDefinition();
	}

	
	public Map<String, OutboundProfile> getOutboundProfiles() throws AppException {
		if(this.actualAPIProxy.getOutboundProfiles()==null) return null;
		if(this.actualAPIProxy.getOutboundProfiles().isEmpty()) return null;
		if(this.actualAPIProxy.getOutboundProfiles().size()==1) {
			OutboundProfile defaultProfile = this.actualAPIProxy.getOutboundProfiles().get("_default");
			if(defaultProfile.getRouteType().equals("proxy")
				&& defaultProfile.getAuthenticationProfile().equals("_default")
				&& defaultProfile.getRequestPolicy() == null 
				&& defaultProfile.getRequestPolicy() == null
				&& (APIManagerAdapter.hasAPIManagerVersion("7.6.2") && defaultProfile.getFaultHandlerPolicy() == null)
			) return null;
		}
		Iterator<OutboundProfile> it = this.actualAPIProxy.getOutboundProfiles().values().iterator();
		while(it.hasNext()) {
			OutboundProfile profile = it.next();
			profile.setApiId(null);
			if(profile.getRequestPolicy()!=null) profile.setRequestPolicy(getExternalPolicyName(profile.getRequestPolicy()), false);
			if(profile.getResponsePolicy()!=null) profile.setResponsePolicy(getExternalPolicyName(profile.getResponsePolicy()), false);
			if(profile.getRoutePolicy()!=null) profile.setRoutePolicy(getExternalPolicyName(profile.getRoutePolicy()), false);
			if(profile.getFaultHandlerPolicy()!=null) profile.setFaultHandlerPolicy(getExternalPolicyName(profile.getFaultHandlerPolicy()), false);
		}
		return this.actualAPIProxy.getOutboundProfiles();
	}
	
	private static String getExternalPolicyName(String policy) {
		if(policy.startsWith("<key")) {
			policy = policy.substring(policy.indexOf("<key type='FilterCircuit'>"));
			policy = policy.substring(policy.indexOf("value='")+7, policy.lastIndexOf("'/></key>"));
		}
		return policy;
	}

	
	public List<SecurityProfile> getSecurityProfiles() throws AppException {
		if(this.actualAPIProxy.getSecurityProfiles().size()==1) {
			if(this.actualAPIProxy.getSecurityProfiles().get(0).getDevices().get(0).getType()==DeviceType.passThrough)
				return null;
		}
		ListIterator<SecurityProfile> it = this.actualAPIProxy.getSecurityProfiles().listIterator();
		while(it.hasNext()) {
			SecurityProfile profile = it.next();
			for(SecurityDevice device : profile.getDevices()) {
				if(device.getType().equals(DeviceType.oauthExternal) || device.getType().equals(DeviceType.authPolicy)) {
					if(device.getProperties().containsKey("tokenStore")) {
						String tokenStore = device.getProperties().get("tokenStore");
						if(tokenStore!=null) {
							device.getProperties().put("tokenStore", getExternalPolicyName(tokenStore));
						}
					}
				}
				device.setConvertPolicies(false);
			}
		}
		return this.actualAPIProxy.getSecurityProfiles();
	}

	
	public List<AuthenticationProfile> getAuthenticationProfiles() {
		if(this.actualAPIProxy.getAuthenticationProfiles().size()==1) {
			if(this.actualAPIProxy.getAuthenticationProfiles().get(0).getType()==AuthType.none)
			return null;
		}
		return this.actualAPIProxy.getAuthenticationProfiles();
	}
	
	public Map<String, InboundProfile> getInboundProfiles() {
		if(this.actualAPIProxy.getInboundProfiles()==null) return null;
		if(this.actualAPIProxy.getInboundProfiles().isEmpty()) return null;
		if(this.actualAPIProxy.getInboundProfiles().size()==1) {
			InboundProfile defaultProfile = this.actualAPIProxy.getInboundProfiles().get("_default");
			if(defaultProfile.getSecurityProfile().equals("_default")
				&& defaultProfile.getCorsProfile().equals("_default")) return null;
		}
		return this.actualAPIProxy.getInboundProfiles();
	}

	
	public List<CorsProfile> getCorsProfiles() {
		if(this.actualAPIProxy.getCorsProfiles()==null) return null;
		if(this.actualAPIProxy.getCorsProfiles().isEmpty()) return null;
		if(this.actualAPIProxy.getCorsProfiles().size()==1) {
			CorsProfile corsProfile = this.actualAPIProxy.getCorsProfiles().get(0);
			if(corsProfile.equals(CorsProfile.getDefaultCorsProfile())) return null;
		}
		return this.actualAPIProxy.getCorsProfiles();
	}

	
	public String getVhost() {
		return this.actualAPIProxy.getVhost();
	}

	
	public TagMap<String, String[]> getTags() {
		if(this.actualAPIProxy.getTags()==null) return null;
		if(this.actualAPIProxy.getTags().isEmpty()) return null;
		return this.actualAPIProxy.getTags();
	}

	
	public String getState() throws AppException {
		return this.actualAPIProxy.getState();
	}

	
	public String getVersion() {
		return this.actualAPIProxy.getVersion();
	}

	
	public String getSummary() {
		return this.actualAPIProxy.getSummary();
	}

	public String getImage() {
		if(this.actualAPIProxy.getImage()==null) return null;
		// We don't have an Image provided from the API-Manager
		return "api-image"+this.actualAPIProxy.getImage().getFileExtension();
	}
	
	@JsonIgnore
	public APIImage getAPIImage() {
		if(this.actualAPIProxy.getImage()==null) return null;
		return this.actualAPIProxy.getImage();
	}

	
	public String getName() {
		return this.actualAPIProxy.getName();
	}

	
	public String getOrganization() {
		String orgId = null;
		try {
			orgId = getOrganizationId();
			return APIManagerAdapter.getInstance().getOrgName(orgId);
		} catch (Exception e) {
			throw new RuntimeException("Can't read orgName for orgId: '"+orgId+"'");
		}
	}

	
	@JsonIgnore
	public String getDeprecated() {
		return ((ActualAPI)this.actualAPIProxy).getDeprecated();
	}

	
	public Map<String, String> getCustomProperties() {
		return this.actualAPIProxy.getCustomProperties();
	}

	
	@JsonIgnore
	public int getAPIType() {
		return ((ActualAPI)this.actualAPIProxy).getAPIType();
	}

	
	public String getDescriptionType() {
		if(this.actualAPIProxy.getDescriptionType().equals("original")) return null;
		return this.actualAPIProxy.getDescriptionType();
	}

	
	public String getDescriptionManual() {
		return this.actualAPIProxy.getDescriptionManual();
	}

	
	public String getDescriptionMarkdown() {
		return this.actualAPIProxy.getDescriptionMarkdown();
	}

	
	public String getDescriptionUrl() {
		return this.actualAPIProxy.getDescriptionUrl();
	}

	public List<CaCert> getCaCerts() {
		if(this.actualAPIProxy.getCaCerts()==null) return null;
		if(this.actualAPIProxy.getCaCerts().size()==0) return null;
		return this.actualAPIProxy.getCaCerts();
	}

	
	public APIQuota getApplicationQuota() {
		return this.actualAPIProxy.getApplicationQuota();
	}

	
	public APIQuota getSystemQuota() {
		return this.actualAPIProxy.getSystemQuota();
	}

	
	@JsonIgnore
	public Map<String, ServiceProfile> getServiceProfiles() {
		return this.actualAPIProxy.getServiceProfiles();
	}

	
	public List<String> getClientOrganizations() {
		if(this.actualAPIProxy.getClientOrganizations().size()==0) return null;
		if(this.actualAPIProxy.getClientOrganizations().size()==1 && 
				this.actualAPIProxy.getClientOrganizations().get(0).equals(getOrganization())) 
			return null;
		return this.actualAPIProxy.getClientOrganizations();
	}

	
	public List<ClientApplication> getApplications() {
		if(this.actualAPIProxy.getApplications().size()==0) return null;
		return this.actualAPIProxy.getApplications();
	}

	
	@JsonProperty("apiDefinition")
	public String getApiDefinitionImport() {
		return this.getAPIDefinition().getAPIDefinitionFile();
	}
	
	@JsonIgnore
	public String getBackendBasepath() {
		return this.getServiceProfiles().get("_default").getBasePath();
	}
}
