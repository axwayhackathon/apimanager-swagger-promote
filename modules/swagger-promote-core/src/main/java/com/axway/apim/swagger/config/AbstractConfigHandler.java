package com.axway.apim.swagger.config;

public abstract class AbstractConfigHandler {
	protected Object apiDefinition;
	
	protected Object apiConfig;
	
	protected String stage;
	
	protected boolean orgAdminUsed;

	public AbstractConfigHandler(Object apiConfig, Object APIDefinition, String stage, boolean orgAdminUsed) {
		super();
		this.apiDefinition = APIDefinition;
		this.apiConfig = apiConfig;
		this.stage = stage;
		this.orgAdminUsed = orgAdminUsed;
	}
	
	public boolean isOrgAdminUsed() {
		return this.orgAdminUsed;
	}
}
