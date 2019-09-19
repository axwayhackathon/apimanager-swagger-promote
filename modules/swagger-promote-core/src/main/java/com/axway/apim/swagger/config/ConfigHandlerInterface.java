package com.axway.apim.swagger.config;

import com.axway.apim.lib.AppException;
import com.axway.apim.swagger.api.state.DesiredAPI;

public interface ConfigHandlerInterface {
	public DesiredAPI getApiConfig() throws AppException;
	public boolean isOrgAdminUsed();
}
