package com.axway.apim.swagger.config;

import com.axway.apim.swagger.api.state.IAPI;

public class APIConfig {
	
	/**
	 * The given API-Configuration by the user in his configuration file 
	 * and will be completed by the APIImportConfigAdapter
	 * This is either the base-configuration or already updated 
	 * with the staged configuration.
	 */
	private IAPI apiConfig;
	
	private boolean orgAdminUsed;
	
	private String apiDefinitionFilename;
	
	private String apiDefinition;

	public boolean isOrgAdminUsed() {
		return orgAdminUsed;
	}

	public void setOrgAdminUsed(boolean orgAdminUsed) {
		this.orgAdminUsed = orgAdminUsed;
	}

	public String getApiDefinitionFilename() {
		return apiDefinitionFilename;
	}

	public void setApiDefinitionFilename(String apiDefinitionFilename) {
		this.apiDefinitionFilename = apiDefinitionFilename;
	}

	public String getApiDefinition() {
		return apiDefinition;
	}

	public void setApiDefinition(String apiDefinition) {
		this.apiDefinition = apiDefinition;
	}

	/**
	 * This is the configuration already parsed
	 * @return
	 */
	public IAPI getApiConfig() {
		return apiConfig;
	}

	public void setApiConfig(IAPI apiConfig) {
		this.apiConfig = apiConfig;
	}
	
}
